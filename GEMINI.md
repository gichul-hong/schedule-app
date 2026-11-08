# Project Overview: Cross-Platform Schedule & Car Logbook App

## Architecture & Tech Stack
- **Platform**: Android & iOS Cross-Platform via **Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP)** (Material 3)
- **Architecture**: MVVM + Clean Architecture (`commonMain` shared UI, Domain, and Data layers)
- **Database**: Room KMP (Offline-first SQLite) + DataStore KMP + Future Cloud Backup (Google Drive / iCloud / Firebase)
- **Features**:
  - **Schedule & Productivity**: Calendar (Month/Week/Day) + To-Do integration, Routine/Habit Tracker & Analytics Dashboard, System Calendar sync (Android `CalendarContract` / iOS `EventKit`).
  - **Car Logbook (차계부)**: Fuel logging with automatic fuel efficiency (km/L) & fuel cost statistics, Maintenance/consumables (engine oil, tires, etc.) replacement cycle management with **automatic Calendar event integration**, Monthly/Annual vehicle maintenance cost report charts (fuel, maintenance, parking, tolls, insurance/installments).

## Update Log
- **2026-11-06**: Initialized project documentation and created development plan in `docs/implementation_plan.md`.
- **2026-11-08**: Updated project scope and `docs/implementation_plan.md` to support Android & iOS cross-platform development using Kotlin Multiplatform (Compose Multiplatform) and integrated Car Logbook (차계부) features with Calendar synchronization.
