# TEST_READY — E2E Test Suite Summary & Execution Guide

## Overview
The E2E Test Suite for the Cross-Platform Schedule & Car Logbook App (KMP/CMP) is ready. It covers all 4 tiers across Schedule/Calendar, To-Do, Routines/Streaks, Vehicle Odometer, Fueling Engine, Dual-Metric Maintenance, and Annual Lifecycle Simulations.

## Test Inventory (50+ Verifications across 4 Test Files)
1. `ScheduleDomainE2ETest.kt`:
   - `testCalendar_CreateSingleDayEventWithTime`: Single-day event scheduling with start and end times.
   - `testCalendar_CreateAllDayEvent`: All-day event configuration.
   - `testCalendar_QueryEventsForSpecificDay`: Single-day event range queries.
   - `testCalendar_QueryEventsForMonthlyRange`: Multi-week/monthly view event querying.
   - `testCalendar_UpdateAndDeleteEvent`: Event lifecycle mutation.
   - `testTodo_CreateWithPriorityAndDueDate`: Task scheduling with priority levels.
   - `testTodo_CompleteLifecycle_SetsCompletedAt`: Mark complete lifecycle setting completion timestamp.
   - `testTodo_UncompleteLifecycle_ResetsCompletedAt`: Unmark task resetting timestamp.
   - `testTodo_FilterCompletedVsPending`: Active vs finished tasks filtering.
   - `testTodo_SortByPriorityDescending`: Priority sorting.
   - `testRoutine_WeekdayMaskFiltering`: Mon-Sun bitmask scheduling verification.
   - `testRoutine_DailyRoutine_1DayStreak`: Single-day streak baseline.
   - `testRoutine_DailyRoutine_MultiDayConsecutiveStreak`: Multi-day consecutive streak tracking.
   - `testRoutine_ZeroStreak_WhenNoLogsExist`: Empty logs safety.
   - `testRoutine_InactiveRoutine_MaintainsDefinition`: Inactive habit preservation.
   - `testBoundary_EventEndBeforeStart_ThrowsException`: End-before-start validation.
   - `testBoundary_ZeroDurationEvent_Allowed`: Zero duration event handling.
   - `testBoundary_MultiDayAllDayEvent_SpansMonthBoundary`: Month crossover multi-day event retrieval.
   - `testBoundary_EmptyScheduleRange_ReturnsEmptyList`: Empty date range query gracefully returns empty.
   - `testBoundary_RoutineStreak_PreservedAcrossNonScheduledWeekend`: Non-scheduled weekdays do not break streak continuity.
   - `testBoundary_RoutineStreak_BrokenOnMissedScheduledDay`: Streak resets to 0 when scheduled day is missed.
   - `testBoundary_RoutineStreak_AcrossYearTransition`: Dec 31 to Jan 1 streak rollover.

2. `CarBookDomainE2ETest.kt`:
   - `testVehicle_RegistrationAndInitialOdometer`: Initial vehicle state registration.
   - `testVehicle_OdometerProgression_ValidUpdate`: Cumulative distance progression.
   - `testVehicle_MultiVehicle_DefaultSelection`: Multi-car profile support.
   - `testFuel_FirstFullTank_BaselineYieldsNullEfficiency`: Initial fuel record baseline contract.
   - `testFuel_ConsecutiveFullTanks_CalculatesExactKmPerL`: Full-to-full km/L and cost/km calculation.
   - `testMaintenance_DistanceDominates_CalculatesDistanceWear`: Distance-bound wear percentage.
   - `testMaintenance_TimeDominates_CalculatesTimeWear`: Time-bound wear percentage.
   - `testExpense_MonthlyCategoryAggregation`: Expense ledger summation by category.
   - `testBoundary_OdometerEntry_LessThanPrevious_ThrowsException`: Anti-rollback odometer enforcement.
   - `testBoundary_ConsecutivePartialRefills_FollowedByFullRefill`: Delta Odometer over sum of partial+full liters ($\Delta O / \sum V$).
   - `testBoundary_MaintenanceWear_OverdueClampingExceeds100Percent`: Clamping above 100% for overdue service items.
   - `testBoundary_SingleMetric_ZeroCycleKm_TimeOnly`: Support for time-only service items (e.g., annual safety inspection).
   - `testBoundary_SingleMetric_ZeroCycleMonths_DistanceOnly`: Support for distance-only service items (e.g., brake pads).
   - `testBoundary_LeapYear_DateTransition`: Leap day (Feb 29) elapsed months calculation.

3. `MaintenanceSyncE2ETest.kt`:
   - `testSync_LogMaintenanceExpense_ResetsWear_ReschedulesCalendarEvent`: Maintenance expense -> odometer update -> wear baseline reset -> next due date -> Calendar `ScheduleEvent` rescheduled.
   - `testSync_RefuelingUpdatesOdometer_TriggersMaintenanceWarning`: Fuel log odometer update pushes wear past threshold and triggers warning.
   - `testSync_UnifiedDailyTimeline_CombinesScheduleRoutineAndCarMaintenance`: Unified daily view combines routine, calendar event, and maintenance item.

4. `AnnualWorkloadSimulationE2ETest.kt`:
   - `testTier4_OneYearOwnershipLifecycleSimulation`: 365-day full lifecycle simulation covering vehicle purchase, daily weekday commute routine, bi-weekly fueling logs (mixed partial and full refills), 6-month engine oil servicing, annual auto insurance, and year-end financial ledger aggregation.

## How to Run Tests
```bash
# Run all unit and domain tests across all KMP targets
./gradlew :composeApp:allTests

# Run common JVM unit tests
./gradlew :composeApp:testDebugUnitTest

# Run iOS Simulator tests
./gradlew :composeApp:iosSimulatorArm64Test
```
