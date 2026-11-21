# Release & Signing Guide — SmartKey Clip

## 1. Debug Build (Verification & Local Testing)
```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```
- Output APK: `app/build/outputs/apk/debug/app-debug.apk`
- Signed with `debugConfig` (`debug.keystore`, generated automatically by the build environment).

## 2. Installing on a Physical Android Device or Emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Or transfer `app-debug.apk` to an Android 7.0+ (`minSdk 24`) device and install via package installer.

## 3. Release Signing Configuration (Never Commit Keystores!)
`app/build.gradle.kts` reads release signing credentials exclusively from environment variables:
- `KEYSTORE_PATH` (path to `.jks` upload/release keystore outside git)
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- Key alias: `upload`

Build a signed Release AAB/APK locally or in CI secrets:
```bash
export KEYSTORE_PATH=/secure/path/upload-keystore.jks
export STORE_PASSWORD="***"
export KEY_PASSWORD="***"
gradle :app:bundleRelease :app:assembleRelease
```
Verify APK signature with Android SDK `apksigner`:
```bash
$ANDROID_SDK_ROOT/build-tools/<version>/apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

## 4. Google Play Store Pre-Submission Checklist
Before claiming Play Store readiness, verify all external requirements:
- [ ] Complete **Device-Tested Beta** checklist on at least 2 physical Android devices (e.g., Android 13/14/15) across WhatsApp, Chrome, Notes, and banking/password fields.
- [ ] Verify Play Console **Data Safety Form** accurately declares optional user-configured Telegram HTTPS sync and local-only storage.
- [ ] Verify Store Listing metadata: Title `SmartKey Clip` (13 chars <= 30 char limit, no emojis/buzzwords), adaptive icon verified, Privacy Policy URL hosted publicly.
- [ ] Verify `isMinifyEnabled = true` testing with ProGuard/R8 rules for Moshi/Retrofit/Room if enabling shrinking for production release.

## 5. Downloading the APK from GitHub After Pushing from AI Studio
Source repositories on GitHub do not commit compiled `.apk` binaries into the file tree. Instead, `.github/workflows/android-ci.yml` automatically builds and publishes the APK after every push (on any branch) or manual run:
1. Push your project from AI Studio to GitHub (**Push to GitHub**).
2. Wait 2–4 minutes for the **Android CI - Build, Test & Publish APK** workflow in the **Actions** tab to finish with a green checkmark.
3. Open **Releases** on your GitHub repository page and download **`SmartKey-Clip-debug.apk`** directly under **Latest APK** (a backup zip is also saved in **Actions → Artifacts → smartkey-clip-debug-apk**).
4. If GitHub Releases creation is blocked by repository defaults, enable **Settings → Actions → General → Workflow permissions → Read and write permissions**, then click **Actions → Run workflow**.

