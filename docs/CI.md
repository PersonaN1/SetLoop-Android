# CI and releases

## Workflows

- `.github/workflows/ci.yml` - runs on pull requests and pushes to `main`. JDK 21 (Temurin), Gradle cache via `gradle/actions/setup-gradle`, installs Android platform 37.1 if the runner lacks it, then runs `./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`. Test and lint reports are uploaded as an artifact on failure. Read-only permissions.
- `.github/workflows/release.yml` - runs when a `v*` tag is pushed (or manually via `workflow_dispatch` with an existing tag). Decodes the keystore, builds a signed release APK and AAB, verifies the APK signature with `apksigner`, and creates a GitHub Release with:
  - `SetLoop-<version>.apk`
  - `SetLoop.apk` (stable name: `https://github.com/PersonaN1/SetLoop-Android/releases/latest/download/SetLoop.apk`)
  - `SetLoop-<version>.aab`
  - `SHA256SUMS.txt`

  Release notes come from the annotated tag message, or are generated if the tag is lightweight. The workflow fails early if any signing secret is missing and always deletes the decoded keystore.

Actions are pinned by commit SHA with the version in a comment.

## Repository secrets

| Secret | Value |
| --- | --- |
| `SETLOOP_KEYSTORE_BASE64` | Base64 of the release keystore |
| `SETLOOP_KEYSTORE_PASSWORD` | Keystore password |
| `SETLOOP_KEY_ALIAS` | Key alias |
| `SETLOOP_KEY_PASSWORD` | Key password |

```
base64 -i ~/Keys/setloop/setloop-release.jks | gh secret set SETLOOP_KEYSTORE_BASE64
gh secret set SETLOOP_KEYSTORE_PASSWORD
gh secret set SETLOOP_KEY_ALIAS
gh secret set SETLOOP_KEY_PASSWORD
```

The workflow passes `SETLOOP_KEYSTORE_FILE` (path to the decoded keystore), `SETLOOP_KEYSTORE_PASSWORD`, `SETLOOP_KEY_ALIAS` and `SETLOOP_KEY_PASSWORD` to Gradle as environment variables. Keep the keystore backed up outside the repo: losing it means users cannot update the app.

## Cutting a release

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`, merge to `main`.
2. Tag and push (annotated tag; its message becomes the release notes):

   ```
   git tag -a vX.Y.Z -m "Release notes here"
   git push origin vX.Y.Z
   ```

3. Check the Release workflow run and the new GitHub Release.
