# ANB Git v1.0 – Phase 1

Native Android foundation for an offline-first Git repository manager.

## Confirmed decisions

- Android application only
- Package ID: `com.anb.git`
- Storage: app-private internal storage
- UI: Clean White + Royal Blue
- No cloud backend
- No Internet permission in the manifest
- APK built with GitHub Actions (not on the phone)

## Phase 1 scope

- Jetpack Compose app shell
- Dashboard and bottom navigation
- Repository, Editor and Settings placeholder screens
- App-private `filesDir/repositories` directory initialization
- GitHub Actions workflow for unit tests and debug APK build

Repository creation, Git operations, Room database and code editing are intentionally deferred to later phases.

## Build requirements

- JDK 17
- Gradle 8.11.1
- Android Gradle Plugin 8.9.2
- Android SDK Platform 35

## Build commands

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

The GitHub Actions workflow installs Gradle and builds the APK remotely.

## Android private storage

The app initializes:

```text
/data/user/0/com.anb.git/files/repositories/
```

The exact path is Android-managed and may vary by device. App-private data is removed when the app is uninstalled, so later phases must provide export/backup before users store important projects.

## Important

A successful CI build is not proof of device-level correctness. Install and test the APK on a real Android device before marking Phase 1 complete.
