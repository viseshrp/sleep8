# Sleep8
[![CI](https://github.com/<OWNER>/<REPO>/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/<OWNER>/<REPO>/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/<OWNER>/<REPO>/branch/main/graph/badge.svg?token=<CODECOV_TOKEN>)](https://codecov.io/gh/<OWNER>/<REPO>)

Sleep8 is a lightweight, offline-first Android app that helps users set an alarm automatically based on when they actually go to sleep.

When the user arms the app (in-app button or Quick Settings tile) and the device is inside the configured night window, Sleep8 watches for screen-off events. If the screen stays off for the configured confirmation period (default 10 minutes), Sleep8 requests the OS clock app to create an alarm for `screen_off_time + 8 hours`. The latest screen-off before confirmation wins; multiple alarms per session are allowed.

Key goals
- Automation: arm once and let the app handle setting the alarm.
- Reliability: foreground monitoring during the night window, exact alarm scheduling and an internal backstop.
- Privacy: strictly offline — no network activity.

Highlights
- Min SDK: 31 (Android 12+)
- Kotlin + Jetpack Compose
- DI: Hilt
- Persistence: Room
- Scheduling: AlarmManager + OS Clock intent (`AlarmClock.ACTION_SET_ALARM`)

---

**Contents**
- Features
- Quickstart (build & run)
- Architecture overview
- Data model & behavior rules
- Permissions & reliability notes
- Testing & manual test plan
- Contributing
- License

---

Features
- Arm/disarm via app UI and Quick Settings tile
- Foreground `NightMonitorService` while armed inside the night window
- Detect `SCREEN_OFF` events and run a confirmation timer (default 10 minutes)
- Create an OS Clock alarm at `screen_off_time + 8 hours` when confirmation succeeds
- Auto-arm schedule with its own start/end times (separate from the night window)
- Internal backstop exact alarm for resilience and telemetry
- Persistent audit log of arm sessions, screen events, and alarm records (Room DB)

Quickstart
Prerequisites
- Android Studio (Giraffe or later) or command-line Gradle
- Java 17 / Kotlin 1.9+
- Device or emulator running Android 12+ (minSdk 31)

Build & run (terminal)
```bash
./gradlew assembleDebug
./gradlew installDebug
```

Run unit tests
```bash
./gradlew testDebugUnitTest
```

Run instrumentation tests (device/emulator)
```bash
./gradlew connectedDebugAndroidTest
```

Project layout (short)
- `app/` — Android application module
- `docs/` — design docs, implementation plan, test plans
- `app/src/main/...` — Kotlin source split by `ui`, `domain`, `service`, `data`, `util`
- `app/build.gradle.kts` — compileSdk=35, minSdk=31, Kotlin + Hilt + Room + Compose

Architecture overview
Sleep8 follows a layered design (Presentation → Domain → Service → Data):
- Presentation: `MainActivity`, `SettingsActivity`, Compose UI, Quick Settings Tile
- Domain: `ArmManager`, `StateMachineManager`, `ConfirmOffScheduler`, `OsAlarmCreator`
- Service: `NightMonitorService` (foreground), `ScreenStateReceiver`, `BootReceiver`
- Data: Room database with `settings`, `arm_sessions`, `screen_events`, `alarm_records`

State machine summary
- `DISARMED` → `ARMED_IDLE` (arm)
- `ARMED_IDLE` → `ARMED_PENDING_CONFIRM` on `SCREEN_OFF` inside night window
- `ARMED_PENDING_CONFIRM` → `ARMED_ALARM_SET` if confirmation timer elapses while screen remains off
- `ARMED_PENDING_CONFIRM` → `ARMED_IDLE` if screen turns on before confirmation
- Any armed state → `DISARMED` on disarm

Behavior rules (concise)
- Night window enforces when screen events are considered; handles windows across midnight.
- Latest screen-off before confirmation wins; the confirmation timer restarts on a new screen-off.
- Alarm created via `AlarmClock.ACTION_SET_ALARM`; also schedule an internal exact backstop using `AlarmManager.setExactAndAllowWhileIdle`.
- After reboot, active session and pending confirmations are restored from DB and resumed when within the night window.

Data model (high level)
- `settings` — night window, confirm minutes (default 10), snooze, defaults
- `arm_sessions` — session lifecycle (armed_at, disarmed_at, source)
- `screen_events` — recorded SCREEN_OFF / SCREEN_ON events per session
- `alarm_records` — persisted alarm metadata (screen_off_ts, confirmed_at, scheduled_alarm_ts, os_alarm_intent_resolved, backstop)

Permissions & reliability
- Foreground service permissions (auto-granted); `RECEIVE_BOOT_COMPLETED` for reboot restore
- `SCHEDULE_EXACT_ALARM` may be required for exact alarm behavior on some platforms
- Battery-optimization guidance is shown; users may need to exclude the app on some OEMs for maximum reliability

OEM notes
- Some OEM clock apps ignore the `EXTRA_SKIP_UI` behavior or require manual confirmation. Sleep8 detects whether the clock intent resolves and records whether UI confirmation is required; it also schedules an internal backstop alarm.

Testing
- Unit tests: JUnit 5 + MockK (see `app/src/test`) — logic for `NightWindowValidator`, `TimeUtils`, `StateMachineManager`, etc.
- Integration: Robolectric for service + DB integration tests
- Manual tests: `docs/MANUAL_TESTS.md` contains detailed P0/P1 test cases (reboot flows, OEM clock behavior, window crossing, etc.)

Coverage (local)
```bash
make coverage
```
Coverage reports are written to `app/build/reports/jacoco/jacocoTestReport/`:
- `html/index.html` for human-readable coverage
- `jacocoTestReport.xml` for CI/Codecov upload

CI/CD
- GitHub Actions runs on pull requests and on pushes to `main` and `release/**`.
- CI uses the Makefile targets for lint, unit, integration, UI tests, and coverage.
- Codecov uploads require `CODECOV_TOKEN` in GitHub Secrets.
- Coverage configuration lives in `codecov.yml`.

Where to read more
- Architecture and design rationale: `docs/ARCHITECTURE.md`
- Implementation plan and milestone breakdown: `docs/IMPLEMENTATION_PLAN.md`
- Full spec: `docs/SPEC.md`
- Test plan: `docs/TEST_PLAN.md`

Contributing
- See `docs/DEV_NOTES.md` for build/run/test instructions.
- Suggested workflow:
	1. Fork & branch from `main` as `feature/your-feature`.
	2. Write small commits with clear messages.
	3. Add tests for new logic; keep behavior deterministic for unit tests.
	4. Open a PR and reference related docs/milestones.

Recommended next improvements
- Add CI with `./gradlew test` and lint checks
- Add badges (build, test coverage) to this README
- Add small onboarding checklist for QA (quick steps for OEM testing)

License
- See `LICENSE` in repository root.

Questions / Contact
- For questions about design decisions, see `docs/ARCHITECTURE.md` and `docs/SPEC.md` or open an issue.
