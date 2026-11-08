# Test Infrastructure & Philosophy — Cross-Platform Schedule & Car Logbook App (KMP/CMP)

## 1. Testing Philosophy
The Schedule & Car Logbook application integrates high-frequency daily productivity (Calendar, To-Do, Routines) with financial and mechanical lifecycle data (Vehicle odometer, Fuel full-to-full efficiency, Maintenance wear dual-metrics).

To guarantee stability across Android and iOS without platform-specific drift:
- **Opaque-Box & Domain-Centric**: Tests exercise public domain use cases, repositories, and calculation engines in `commonTest` without mocking business logic.
- **Progressive Testability & Determinism**: All date/time transitions use explicit `kotlinx-datetime` values to verify boundary conditions (month transitions, leap years, year rollovers).
- **Zero-Tolerance Financial & Calculation Precision**: Fuel efficiency (km/L), per-km cost, and dual-metric maintenance wear (km and elapsed months) must match exact mathematical contracts.

---

## 2. Test Architecture & Directory Layout

```
composeApp/
└── src/
    ├── commonMain/kotlin/com/scheduleapp/
    │   ├── domain/
    │   │   ├── model/         # ScheduleEvent, TodoItem, Routine, Vehicle, FuelDetail, MaintenanceItem
    │   │   ├── engine/        # FuelEfficiencyEngine, MaintenanceWearEngine, RoutineStreakEngine
    │   │   ├── repository/    # ScheduleRepository, CarBookRepository
    │   │   └── usecase/       # SyncMaintenanceToScheduleUseCase, IntegratedDailyTimelineUseCase
    └── commonTest/kotlin/com/scheduleapp/test/
        ├── ScheduleDomainE2ETest.kt          # Calendar, Todo, Routine habit tracker & streaks
        ├── CarBookDomainE2ETest.kt           # Vehicle, Fuel efficiency, Maintenance wear, Expense reports
        ├── MaintenanceSyncE2ETest.kt         # Pairwise cross-feature interactions & calendar sync
        └── AnnualWorkloadSimulationE2ETest.kt # 1-Year ownership simulation & stress workload
```

---

## 3. Systematic Test Tiers

| Tier | Name | Target Coverage & Characteristics |
|---|---|---|
| **Tier 1** | **Feature Coverage** | $\ge 5$ tests per feature area covering primary behaviors and API contracts. |
| **Tier 2** | **Boundary & Corner Cases** | Negative inputs, empty states, leap years, single-metric maintenance, overdue clamping, partial fuel refuels. |
| **Tier 3** | **Cross-Feature Interactions** | Pairwise interactions: Maintenance expense $\to$ Odometer $\to$ Wear reset $\to$ Calendar rescheduling; Refueling $\to$ Maintenance warning alert. |
| **Tier 4** | **Real-World Workloads** | 365-day end-to-end lifecycle simulation executing daily commute routines, bi-weekly fuelings, 6-month servicing, and annual reports. |

---

## 4. Coverage Thresholds & Quality Gates
- **Domain Engine Logic**: 100% branch and statement coverage on:
  - `FuelEfficiencyEngine` (Full-to-full delta calculation, accumulated partial refills)
  - `MaintenanceWearEngine` (Dual-metric distance & month wear, overdue clamping, single-metric fallback)
  - `RoutineStreakEngine` (Mask filtering, skipped non-scheduled days)
- **Aggregation & Reporting**: 100% accuracy on monthly/annual expense breakdowns.
- **Multiplatform Verification**: Pass on JVM, Android unit test, and iOS simulator ARM64.
