# Known Issues & Physical Device Testing Pending Items

## 1. Environment-Constrained Items (Cannot Be Executed in Container)
Because this cloud build container has **no Android Emulator (`adb`) or physical device**:
- Live on-screen IME typing inside third-party apps (WhatsApp, Chrome, Notes) has **not** yet been executed on a physical device.
- Android system **Settings → Languages & Input → Manage keyboards** activation and IME switching UX require manual verification on device.
- Hardware-backed `AndroidKeyStore` StrongBox/TEE key generation and real `api.telegram.org` round-trip latency with a user-owned test bot require device execution.
- Actual RAM (`dumpsys meminfo`), battery drain, and cold-open IME frame timing (`gfxinfo`) on physical hardware are pending device testing.

## 2. Android OS Platform Boundaries (By Design)
- **Background Clipboard Monitoring (Android 10+ / API 29+):** Android blocks background apps from reading `ClipboardManager.primaryClip`. SmartKey Clip reads the system clipboard only when the IME or companion app has focus, or when the user taps **Save Clip** / **Paste Clipboard**.
- **Telegram Bot API Encryption:** Telegram Bot API uses HTTPS/TLS transport encryption and server-side storage—**not** end-to-end Secret Chat encryption. Sensitive clips (OTPs, tokens, private keys, `EXTRA_IS_SENSITIVE`) are blocked from Telegram upload.
- **No Cloud Backup by Default:** `android:allowBackup="false"` prevents unencrypted Google Drive backup of local clipboard records and Keystore metadata; users should use the manual **Export JSON** feature in Super Clipboard.
