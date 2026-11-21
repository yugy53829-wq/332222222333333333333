# Security Audit Report — SmartKey Clip

## 1. Threat Model & Entry Points
- **Untrusted Inputs:**
  - System clipboard contents (`ClipboardManager.primaryClip`)
  - Imported JSON backups (`ActivityResultContracts.OpenDocument`)
  - Remote Telegram Bot API responses (`getMe`, `getUpdates`, `sendMessage`)
  - User-entered Bot Token & Chat ID
  - Third-party editor `EditorInfo` metadata when IME opens
- **Trust Boundaries:**
  - Android IME process vs. host app text editors (passwords/OTPs)
  - Local SQLite/Room database & Keystore vault vs. cloud backups
  - Device vs. `api.telegram.org` HTTPS transport

## 2. Verified Controls & Remediations
1. **No Deprecated `EncryptedSharedPreferences`:** Replaced with direct **Android Keystore AES-256-GCM** (`AndroidKeystoreCryptoManager`) using 256-bit `KeyGenParameterSpec`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`, and randomized 96-bit IV per write.
2. **Bot Token Redaction & Zero URL Logging:** Because Telegram Bot API embeds the Bot Token in the URL path (`/bot<TOKEN>/...`), `TelegramApiService` intentionally omits `HttpLoggingInterceptor`. All error descriptions and exceptions pass through `LogRedactor.redact(...)`.
3. **Zero Hardcoded Secrets:** Confirmed via codebase scan; no Bot Tokens or API keys exist in source code or `app-debug.apk`.
4. **Backup & Device Transfer Exclusion:** `android:allowBackup="false"` in `AndroidManifest.xml`, backed by `<exclude>` rules in `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml` for `sharedpref`, `database`, and `datastore`.
5. **Password & OTP Field Protection:** `SensitiveContentDetector.inspectEditorInfo` detects `TYPE_TEXT_VARIATION_PASSWORD`, `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`, `TYPE_TEXT_VARIATION_WEB_PASSWORD`, `TYPE_NUMBER_VARIATION_PASSWORD`, `IME_FLAG_NO_PERSONALIZED_LEARNING`, and Arabic/English password/OTP cues—automatically pausing clipboard auto-capture and suppressing word suggestions.
6. **Sensitive Clip Isolation:** Standalone 4–8 digit OTPs, Bot Tokens, JWTs, PEM private keys, and Android 13+ `ClipDescription.EXTRA_IS_SENSITIVE` items are flagged `isSensitive = true`, masked in UI, excluded from default JSON exports, and blocked from Telegram uploads.
7. **Telegram Sync Opt-In Default:** Disabled by default (`telegramSyncEnabled = false`).
8. **Input Size & Schema Validation:** `ClipboardJsonBackup.decode` enforces a 15 MB safety limit and `schemaVersion == 1`, rejecting malformed payloads.

## 3. Residual Risks & Disclosures
- **Telegram Bot API Not E2EE:** Telegram Bot API uses TLS in transit and Telegram server-side storage, NOT Secret Chat end-to-end encryption. Users are warned in-app and in documentation.
- **Unencrypted Local Room DB Body:** Clip bodies in `smart_clipboard.db` rely on Android OS File-Based Encryption (FBE) rather than SQLCipher; sensitive items are flagged/masked and excluded from sync/backup, while full SQLCipher integration remains a candidate enhancement if regulatory compliance demands app-layer DB encryption.
- **On-Device StrongBox Verification Pending:** `AndroidKeystoreCryptoManager` uses standard `AndroidKeyStore`; physical device testing is needed to confirm hardware TEE/StrongBox behavior across OEMs.
