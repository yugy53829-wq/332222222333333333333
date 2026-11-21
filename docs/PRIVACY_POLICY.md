# Privacy Policy — SmartKey Clip

**Last Updated:** 2026-11-21

## 1. Local-First Data Storage
SmartKey Clip is a native Android Input Method Editor (IME) and Super Clipboard manager designed to keep your data on your device:
- **Keystrokes:** Typed keystrokes are committed directly to the active Android `InputConnection` and are **never logged, recorded, or transmitted** to any server.
- **Clipboard History:** Saved snippets and folders are stored locally in an on-device SQLite (Room) database (`smart_clipboard.db`).
- **No Telemetry or Ads:** SmartKey Clip contains zero advertising SDKs, zero analytics trackers, and zero crash-telemetry uploads.

## 2. Android Clipboard & Sensitive-Field Protection
- **OS Clipboard Boundaries:** Starting with Android 10 (API 29) and Android 13 (API 33), Android restricts background clipboard reading. SmartKey Clip only inspects the system clipboard when the keyboard or companion app has active focus, or when you explicitly tap **Save Clip** / **Paste Clipboard**.
- **Password & OTP Fields:** When an editor declares password or OTP input flags (`TYPE_TEXT_VARIATION_PASSWORD`, `TYPE_NUMBER_VARIATION_PASSWORD`, `IME_FLAG_NO_PERSONALIZED_LEARNING`, or OTP cues), SmartKey Clip automatically disables word suggestions and pauses clipboard auto-capture.
- **Sensitive Text Masking:** Standalone 4–8 digit OTP codes, private keys, JWTs, Bot Tokens, and clips flagged by Android (`ClipDescription.EXTRA_IS_SENSITIVE`) are marked sensitive, masked in the UI by default, excluded from JSON exports by default, and **blocked from Telegram sync**.

## 3. Optional Telegram Bot Sync (Disabled by Default)
- **Opt-In Only:** Telegram synchronization is **disabled by default**.
- **Credential Protection:** Your Bot Token and Chat ID are encrypted at rest using **Android Keystore AES-256-GCM** (`AndroidKeyStore`) and never written in plaintext or logged to Logcat.
- **Transport vs. End-to-End Encryption:** Requests go directly from your device over HTTPS (`TLS`) to `api.telegram.org`. **Important:** Telegram Bot API does **not** provide end-to-end Secret Chat encryption; messages sent through a Bot are stored on Telegram servers per Telegram's cloud architecture. Do not sync passwords, private keys, or regulated secrets.

## 4. Backups & Permissions
- **Cloud Backup Excluded:** `android:allowBackup="false"` and explicit `data_extraction_rules.xml` / `backup_rules.xml` prevent unencrypted cloud backup of your Keystore vault and clipboard database.
- **Zero Broad Storage Permissions:** JSON export/import uses Android's Storage Access Framework (`CreateDocument` / `OpenDocument`) with no `READ_EXTERNAL_STORAGE` or `READ_MEDIA_*` permissions.
