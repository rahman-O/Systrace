# SysTrace

Android MDM agent for device monitoring, background snapshot collection, and remote command execution.

## Features

- Device snapshot collection (hardware, security, SIM, identifiers, presence)
- Foreground background service with periodic sync
- Online/Offline presence detection
- WorkManager watchdog for service resilience
- Multi-module Clean Architecture (Hilt, Room-ready, Compose UI)

## Modules

- `app` — Application entry point
- `core/common` — Shared models and collectors
- `core/domain` — Domain layer
- `core/database` — Room database
- `data` — Repositories and data sources
- `service` — Foreground service and workers
- `presentation` — Compose UI

## Build

```bash
./gradlew :app:assembleDebug
```

Requires JDK 17+ and Android SDK 36.
