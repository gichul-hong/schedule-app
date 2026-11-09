package com.scheduleapp.core.platform

import com.scheduleapp.core.database.dao.CarBookDao
import com.scheduleapp.core.database.dao.ScheduleDao
import com.scheduleapp.core.database.entity.CarExpenseLogEntity
import com.scheduleapp.core.database.entity.FuelDetailEntity
import com.scheduleapp.core.database.entity.MaintenanceItemEntity
import com.scheduleapp.core.database.entity.RoutineEntity
import com.scheduleapp.core.database.entity.RoutineLogEntity
import com.scheduleapp.core.database.entity.ScheduleEventEntity
import com.scheduleapp.core.database.entity.TodoItemEntity
import com.scheduleapp.core.database.entity.VehicleEntity
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

data class DatabaseSnapshot(
    val version: Int = 1,
    val exportedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val events: List<ScheduleEventEntity> = emptyList(),
    val todos: List<TodoItemEntity> = emptyList(),
    val routines: List<RoutineEntity> = emptyList(),
    val routineLogs: List<RoutineLogEntity> = emptyList(),
    val vehicles: List<VehicleEntity> = emptyList(),
    val expenses: List<CarExpenseLogEntity> = emptyList(),
    val fuelDetails: List<FuelDetailEntity> = emptyList(),
    val maintenanceItems: List<MaintenanceItemEntity> = emptyList()
) {
    val totalRecords: Int
        get() = events.size + todos.size + routines.size + routineLogs.size +
                vehicles.size + expenses.size + fuelDetails.size + maintenanceItems.size
}

class DatabaseBackupEngine(
    private val scheduleDao: ScheduleDao,
    private val carBookDao: CarBookDao
) {
    suspend fun createSnapshot(): DatabaseSnapshot {
        val events = scheduleDao.getAllEvents().first()
        val todos = scheduleDao.getAllTodos().first()
        val routines = scheduleDao.getAllRoutines().first()
        val routineLogs = scheduleDao.getAllRoutineLogs().first()

        val vehicles = carBookDao.getAllVehicles().first()
        val expenses = carBookDao.getAllExpenses().first()
        val fuelDetails = carBookDao.getAllFuelDetails().first()
        val maintenanceItems = carBookDao.getAllMaintenanceItems().first()

        return DatabaseSnapshot(
            version = 1,
            exportedAt = Clock.System.now().toEpochMilliseconds(),
            events = events,
            todos = todos,
            routines = routines,
            routineLogs = routineLogs,
            vehicles = vehicles,
            expenses = expenses,
            fuelDetails = fuelDetails,
            maintenanceItems = maintenanceItems
        )
    }

    suspend fun exportToJson(): String {
        val snapshot = createSnapshot()
        return serializeSnapshot(snapshot)
    }

    suspend fun restoreFromJson(json: String): Result<DatabaseSnapshot> = runCatching {
        val snapshot = parseSnapshot(json)

        // Clear existing tables in dependency order
        carBookDao.clearAllFuelDetails()
        carBookDao.clearAllExpenses()
        carBookDao.clearAllMaintenanceItems()
        carBookDao.clearAllVehicles()

        scheduleDao.clearAllRoutineLogs()
        scheduleDao.clearAllRoutines()
        scheduleDao.clearAllTodos()
        scheduleDao.clearAllEvents()

        // Insert restored tables in referential integrity order
        if (snapshot.vehicles.isNotEmpty()) carBookDao.insertVehicles(snapshot.vehicles)
        if (snapshot.expenses.isNotEmpty()) carBookDao.insertExpenses(snapshot.expenses)
        if (snapshot.fuelDetails.isNotEmpty()) carBookDao.insertFuelDetails(snapshot.fuelDetails)
        if (snapshot.maintenanceItems.isNotEmpty()) carBookDao.insertMaintenanceItems(snapshot.maintenanceItems)

        if (snapshot.routines.isNotEmpty()) scheduleDao.insertRoutines(snapshot.routines)
        if (snapshot.routineLogs.isNotEmpty()) scheduleDao.insertRoutineLogs(snapshot.routineLogs)
        if (snapshot.todos.isNotEmpty()) scheduleDao.insertTodos(snapshot.todos)
        if (snapshot.events.isNotEmpty()) scheduleDao.insertEvents(snapshot.events)

        snapshot
    }

    companion object {
        fun serializeSnapshot(s: DatabaseSnapshot): String {
            val sb = StringBuilder()
            sb.append("{\n")
            sb.append("  \"version\": ${s.version},\n")
            sb.append("  \"exportedAt\": ${s.exportedAt},\n")

            sb.append("  \"events\": [")
            if (s.events.isNotEmpty()) {
                sb.append("\n")
                s.events.forEachIndexed { i, e ->
                    sb.append("    {\"id\": ${e.id}, \"externalEventId\": ${e.externalEventId.toJsonValue()}, \"title\": ${e.title.toJsonString()}, \"startTime\": ${e.startTime}, \"endTime\": ${e.endTime}, \"isAllDay\": ${e.isAllDay}, \"category\": ${e.category.toJsonString()}, \"linkedCarRecordId\": ${e.linkedCarRecordId}}")
                    if (i < s.events.size - 1) sb.append(",")
                    sb.append("\n")
                }
                sb.append("  ")
            }
            sb.append("],\n")

            sb.append("  \"todos\": [")
            if (s.todos.isNotEmpty()) {
                sb.append("\n")
                s.todos.forEachIndexed { i, t ->
                    sb.append("    {\"id\": ${t.id}, \"title\": ${t.title.toJsonString()}, \"dueDate\": ${t.dueDate}, \"priority\": ${t.priority}, \"isCompleted\": ${t.isCompleted}, \"completedAt\": ${t.completedAt}}")
                    if (i < s.todos.size - 1) sb.append(",")
                    sb.append("\n")
                }
                sb.append("  ")
            }
            sb.append("],\n")

            sb.append("  \"routines\": [")
            if (s.routines.isNotEmpty()) {
                sb.append("\n")
                s.routines.forEachIndexed { i, r ->
                    sb.append("    {\"id\": ${r.id}, \"title\": ${r.title.toJsonString()}, \"repeatDaysMask\": ${r.repeatDaysMask}, \"targetTime\": ${r.targetTime.toJsonValue()}, \"category\": ${r.category.toJsonString()}, \"createdAt\": ${r.createdAt}}")
                    if (i < s.routines.size - 1) sb.append(",")
                    sb.append("\n")
                }
                sb.append("  ")
            }
            sb.append("],\n")

            sb.append("  \"routineLogs\": [")
            if (s.routineLogs.isNotEmpty()) {
                sb.append("\n")
                s.routineLogs.forEachIndexed { i, l ->
                    sb.append("    {\"id\": ${l.id}, \"routineId\": ${l.routineId}, \"date\": ${l.date.toJsonString()}, \"isCompleted\": ${l.isCompleted}}")
                    if (i < s.routineLogs.size - 1) sb.append(",")
                    sb.append("\n")
                }
                sb.append("  ")
            }
            sb.append("],\n")

            sb.append("  \"vehicles\": [")
            if (s.vehicles.isNotEmpty()) {
                sb.append("\n")
                s.vehicles.forEachIndexed { i, v ->
                    sb.append("    {\"id\": ${v.id}, \"name\": ${v.name.toJsonString()}, \"model\": ${v.model.toJsonString()}, \"fuelType\": ${v.fuelType.toJsonString()}, \"initialOdometer\": ${v.initialOdometer}, \"currentOdometer\": ${v.currentOdometer}, \"isDefault\": ${v.isDefault}}")
                    if (i < s.vehicles.size - 1) sb.append(",")
                    sb.append("\n")
                }
                sb.append("  ")
            }
            sb.append("],\n")

            sb.append("  \"expenses\": [")
            if (s.expenses.isNotEmpty()) {
                sb.append("\n")
                s.expenses.forEachIndexed { i, e ->
                    sb.append("    {\"id\": ${e.id}, \"vehicleId\": ${e.vehicleId}, \"date\": ${e.date}, \"type\": ${e.type.toJsonString()}, \"amount\": ${e.amount}, \"odometer\": ${e.odometer}, \"memo\": ${e.memo.toJsonValue()}, \"receiptImageUri\": ${e.receiptImageUri.toJsonValue()}}")
                    if (i < s.expenses.size - 1) sb.append(",")
                    sb.append("\n")
                }
                sb.append("  ")
            }
            sb.append("],\n")

            sb.append("  \"fuelDetails\": [")
            if (s.fuelDetails.isNotEmpty()) {
                sb.append("\n")
                s.fuelDetails.forEachIndexed { i, f ->
                    sb.append("    {\"expenseId\": ${f.expenseId}, \"unitPrice\": ${f.unitPrice}, \"liters\": ${f.liters}, \"isFullTank\": ${f.isFullTank}, \"calculatedEfficiency\": ${f.calculatedEfficiency}}")
                    if (i < s.fuelDetails.size - 1) sb.append(",")
                    sb.append("\n")
                }
                sb.append("  ")
            }
            sb.append("],\n")

            sb.append("  \"maintenanceItems\": [")
            if (s.maintenanceItems.isNotEmpty()) {
                sb.append("\n")
                s.maintenanceItems.forEachIndexed { i, m ->
                    sb.append("    {\"id\": ${m.id}, \"vehicleId\": ${m.vehicleId}, \"partName\": ${m.partName.toJsonString()}, \"cycleKm\": ${m.cycleKm}, \"cycleMonths\": ${m.cycleMonths}, \"lastReplacedDate\": ${m.lastReplacedDate}, \"lastReplacedOdometer\": ${m.lastReplacedOdometer}, \"nextDueDate\": ${m.nextDueDate}, \"scheduledEventId\": ${m.scheduledEventId}}")
                    if (i < s.maintenanceItems.size - 1) sb.append(",")
                    sb.append("\n")
                }
                sb.append("  ")
            }
            sb.append("]\n")

            sb.append("}")
            return sb.toString()
        }

        @Suppress("UNCHECKED_CAST")
        fun parseSnapshot(json: String): DatabaseSnapshot {
            val root = MiniJsonParser(json).parse() as? Map<String, Any?>
                ?: throw IllegalArgumentException("Invalid JSON root: expected object")

            val version = (root["version"] as? Number)?.toInt() ?: 1
            val exportedAt = (root["exportedAt"] as? Number)?.toLong() ?: 0L

            val eventsList = (root["events"] as? List<Map<String, Any?>>)?.map { m ->
                ScheduleEventEntity(
                    id = (m["id"] as? Number)?.toLong() ?: 0L,
                    externalEventId = m["externalEventId"] as? String,
                    title = m["title"] as? String ?: "",
                    startTime = (m["startTime"] as? Number)?.toLong() ?: 0L,
                    endTime = (m["endTime"] as? Number)?.toLong() ?: 0L,
                    isAllDay = m["isAllDay"] as? Boolean ?: false,
                    category = m["category"] as? String ?: "GENERAL",
                    linkedCarRecordId = (m["linkedCarRecordId"] as? Number)?.toLong()
                )
            } ?: emptyList()

            val todosList = (root["todos"] as? List<Map<String, Any?>>)?.map { m ->
                TodoItemEntity(
                    id = (m["id"] as? Number)?.toLong() ?: 0L,
                    title = m["title"] as? String ?: "",
                    dueDate = (m["dueDate"] as? Number)?.toLong(),
                    priority = (m["priority"] as? Number)?.toInt() ?: 2,
                    isCompleted = m["isCompleted"] as? Boolean ?: false,
                    completedAt = (m["completedAt"] as? Number)?.toLong()
                )
            } ?: emptyList()

            val routinesList = (root["routines"] as? List<Map<String, Any?>>)?.map { m ->
                RoutineEntity(
                    id = (m["id"] as? Number)?.toLong() ?: 0L,
                    title = m["title"] as? String ?: "",
                    repeatDaysMask = (m["repeatDaysMask"] as? Number)?.toInt() ?: 0x7F,
                    targetTime = m["targetTime"] as? String,
                    category = m["category"] as? String ?: "DAILY",
                    createdAt = (m["createdAt"] as? Number)?.toLong() ?: 0L
                )
            } ?: emptyList()

            val routineLogsList = (root["routineLogs"] as? List<Map<String, Any?>>)?.map { m ->
                RoutineLogEntity(
                    id = (m["id"] as? Number)?.toLong() ?: 0L,
                    routineId = (m["routineId"] as? Number)?.toLong() ?: 0L,
                    date = m["date"] as? String ?: "",
                    isCompleted = m["isCompleted"] as? Boolean ?: false
                )
            } ?: emptyList()

            val vehiclesList = (root["vehicles"] as? List<Map<String, Any?>>)?.map { m ->
                VehicleEntity(
                    id = (m["id"] as? Number)?.toLong() ?: 0L,
                    name = m["name"] as? String ?: "",
                    model = m["model"] as? String ?: "",
                    fuelType = m["fuelType"] as? String ?: "GASOLINE",
                    initialOdometer = (m["initialOdometer"] as? Number)?.toDouble() ?: 0.0,
                    currentOdometer = (m["currentOdometer"] as? Number)?.toDouble() ?: 0.0,
                    isDefault = m["isDefault"] as? Boolean ?: false
                )
            } ?: emptyList()

            val expensesList = (root["expenses"] as? List<Map<String, Any?>>)?.map { m ->
                CarExpenseLogEntity(
                    id = (m["id"] as? Number)?.toLong() ?: 0L,
                    vehicleId = (m["vehicleId"] as? Number)?.toLong() ?: 0L,
                    date = (m["date"] as? Number)?.toLong() ?: 0L,
                    type = m["type"] as? String ?: "OTHER",
                    amount = (m["amount"] as? Number)?.toDouble() ?: 0.0,
                    odometer = (m["odometer"] as? Number)?.toDouble() ?: 0.0,
                    memo = m["memo"] as? String,
                    receiptImageUri = m["receiptImageUri"] as? String
                )
            } ?: emptyList()

            val fuelDetailsList = (root["fuelDetails"] as? List<Map<String, Any?>>)?.map { m ->
                FuelDetailEntity(
                    expenseId = (m["expenseId"] as? Number)?.toLong() ?: 0L,
                    unitPrice = (m["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                    liters = (m["liters"] as? Number)?.toDouble() ?: 0.0,
                    isFullTank = m["isFullTank"] as? Boolean ?: true,
                    calculatedEfficiency = (m["calculatedEfficiency"] as? Number)?.toDouble()
                )
            } ?: emptyList()

            val maintenanceItemsList = (root["maintenanceItems"] as? List<Map<String, Any?>>)?.map { m ->
                MaintenanceItemEntity(
                    id = (m["id"] as? Number)?.toLong() ?: 0L,
                    vehicleId = (m["vehicleId"] as? Number)?.toLong() ?: 0L,
                    partName = m["partName"] as? String ?: "",
                    cycleKm = (m["cycleKm"] as? Number)?.toDouble() ?: 0.0,
                    cycleMonths = (m["cycleMonths"] as? Number)?.toInt() ?: 0,
                    lastReplacedDate = (m["lastReplacedDate"] as? Number)?.toLong() ?: 0L,
                    lastReplacedOdometer = (m["lastReplacedOdometer"] as? Number)?.toDouble() ?: 0.0,
                    nextDueDate = (m["nextDueDate"] as? Number)?.toLong() ?: 0L,
                    scheduledEventId = (m["scheduledEventId"] as? Number)?.toLong()
                )
            } ?: emptyList()

            return DatabaseSnapshot(
                version = version,
                exportedAt = exportedAt,
                events = eventsList,
                todos = todosList,
                routines = routinesList,
                routineLogs = routineLogsList,
                vehicles = vehiclesList,
                expenses = expensesList,
                fuelDetails = fuelDetailsList,
                maintenanceItems = maintenanceItemsList
            )
        }
    }
}

private fun String?.toJsonValue(): String =
    if (this == null) "null" else "\"${this.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")}\""

private fun String.toJsonString(): String =
    "\"${this.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")}\""

/**
 * Lightweight, zero-dependency pure Kotlin JSON parser.
 */
class MiniJsonParser(private val src: String) {
    private var pos = 0

    fun parse(): Any? {
        skipWhitespace()
        if (pos >= src.length) return null
        return parseValue()
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        if (pos >= src.length) return null
        return when (val ch = src[pos]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            '-', in '0'..'9' -> parseNumber()
            else -> throw IllegalArgumentException("Unexpected character '$ch' at position $pos")
        }
    }

    private fun parseObject(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        expect('{')
        skipWhitespace()
        if (pos < src.length && src[pos] == '}') {
            pos++
            return map
        }
        while (pos < src.length) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            map[key] = value
            skipWhitespace()
            if (pos < src.length && src[pos] == '}') {
                pos++
                break
            }
            expect(',')
        }
        return map
    }

    private fun parseArray(): List<Any?> {
        val list = mutableListOf<Any?>()
        expect('[')
        skipWhitespace()
        if (pos < src.length && src[pos] == ']') {
            pos++
            return list
        }
        while (pos < src.length) {
            val value = parseValue()
            list.add(value)
            skipWhitespace()
            if (pos < src.length && src[pos] == ']') {
                pos++
                break
            }
            expect(',')
        }
        return list
    }

    private fun parseString(): String {
        expect('"')
        val sb = StringBuilder()
        while (pos < src.length) {
            val ch = src[pos++]
            if (ch == '"') return sb.toString()
            if (ch == '\\') {
                if (pos >= src.length) break
                when (val esc = src[pos++]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        val hex = src.substring(pos, pos + 4)
                        pos += 4
                        sb.append(hex.toInt(16).toChar())
                    }
                    else -> sb.append(esc)
                }
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun parseNumber(): Number {
        val start = pos
        if (src[pos] == '-') pos++
        while (pos < src.length && (src[pos] in '0'..'9' || src[pos] == '.' || src[pos] == 'e' || src[pos] == 'E' || src[pos] == '+' || src[pos] == '-')) {
            if ((src[pos] == '+' || src[pos] == '-') && src[pos - 1] != 'e' && src[pos - 1] != 'E') break
            pos++
        }
        val text = src.substring(start, pos)
        return if (text.contains('.') || text.contains('e') || text.contains('E')) {
            text.toDoubleOrNull() ?: 0.0
        } else {
            text.toLongOrNull() ?: 0L
        }
    }

    private fun parseBoolean(): Boolean {
        return if (src.startsWith("true", pos)) {
            pos += 4
            true
        } else if (src.startsWith("false", pos)) {
            pos += 5
            false
        } else {
            throw IllegalArgumentException("Expected boolean at position $pos")
        }
    }

    private fun parseNull(): Any? {
        if (src.startsWith("null", pos)) {
            pos += 4
            return null
        }
        throw IllegalArgumentException("Expected null at position $pos")
    }

    private fun skipWhitespace() {
        while (pos < src.length && src[pos].isWhitespace()) {
            pos++
        }
    }

    private fun expect(ch: Char) {
        skipWhitespace()
        if (pos >= src.length || src[pos] != ch) {
            throw IllegalArgumentException("Expected '$ch' at position $pos, found '${if (pos < src.length) src[pos] else "EOF"}'")
        }
        pos++
    }
}
