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


## Phase 2 additions

- SQLite repository catalog.
- Create, list, rename and delete private repository folders.
- No storage permission or network access required.
- Git initialization, commits and branches are reserved for Phase 3.


## Phase 3 additions

- JGit local Git engine.
- Initialize Git in a selected local repository.
- Show current branch and working-tree status.
- Stage new, modified and removed files.
- Create commits with a local default author identity.
- Display recent commit history.
- All operations are local and do not require network access.

Phase 3 does not yet include branch creation/switching, merge, diff UI, or conflict resolution.


## Phase 4 additions

- Native offline text/code editor using Jetpack Compose.
- Select a local repository and browse root-level files.
- Create new files, open files, edit text, and save locally.
- Monospace editing surface and unsaved-change indicator.
- File names are restricted to the repository root in this phase.
- No network access or cloud service is required.

Limitations: syntax highlighting, multi-file tabs, nested folders, diff viewer,
and advanced editor tooling are planned for later phases.
