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


## Persistent APK update signing (important)

The release build now uses a persistent keystore supplied through GitHub Actions
repository secrets. Keep the keystore file and its passwords backed up securely.
Never commit the keystore or passwords to Git.

Required repository secrets:
- `ANB_KEYSTORE_BASE64`: Base64-encoded contents of the `.jks` file
- `ANB_KEYSTORE_PASSWORD`: keystore password
- `ANB_KEY_ALIAS`: key alias
- `ANB_KEY_PASSWORD`: key password

Create the key once on a trusted machine (Termux with a JDK is acceptable):
```sh
keytool -genkeypair -v -keystore "$HOME/anb-git-release.jks" \
  -alias anb-git -keyalg RSA -keysize 2048 -validity 10000
```
Use the alias `anb-git`. Then encode the keystore for the GitHub secret:
```sh
base64 "$HOME/anb-git-release.jks" | tr -d '\n'
```
Copy that single output into the GitHub repository secret `ANB_KEYSTORE_BASE64`.
Set the three remaining secrets to the exact values used when creating the key.
The key is a permanent asset: losing it prevents future in-place APK updates.

The release workflow deliberately fails if signing secrets are missing. Pull
requests run unit tests but do not receive a signed release APK.

Version code is now 5, which is greater than the earlier Phase 4 build's
version code 1. The application ID remains `com.anb.git`.

## Migration warning for the already-installed pre-signing APK

Earlier CI workflows produced debug APKs without preserving the runner's
debug signing key. A new persistent release key cannot sign an update over an
APK signed by a different key. Do not uninstall the current app until its
repository data has been exported and verified. The project must add and test
an in-app export/restore path before any one-time signature migration is
attempted. If the original signing certificate/private key is recovered and
matches, this migration is not needed.


### Signing configuration fix

Debug unit tests no longer require release-signing secrets. The release signing
configuration is activated only when all four signing environment variables
are present, and `assembleRelease` explicitly fails if they are missing. This
allows `testDebugUnitTest` to run before the keystore restore step while
preventing an accidental unsigned release artifact.


### Follow-up Gradle task-scope correction

The `assembleRelease` guard now checks the four environment variables directly
inside the task action. It no longer references a variable declared inside the
Android configuration scope, so debug unit-test configuration can compile.


### Release task registration correction

Removed the eager `tasks.named("assembleRelease")` lookup because Android Gradle
Plugin registers that task after this script's early configuration point.
The workflow now validates all four signing secrets before invoking
`assembleRelease`; the Gradle Android signing config is activated only when
the signing environment is present. Unit tests can configure without secrets,
and release jobs stop before Gradle if any signing secret is missing.
