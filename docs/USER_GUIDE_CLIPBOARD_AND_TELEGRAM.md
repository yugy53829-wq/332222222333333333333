# User Guide — Super Clipboard & Optional Telegram Sync

## 1. Activating SmartKey Clip Keyboard (Native Android IME)
1. Open **SmartKey Clip** → **Keyboard** tab.
2. Tap **Enable IME** (opens Android *Manage on-screen keyboards*) and toggle **SmartKey Clip Keyboard** ON.
3. Return to the app and tap **Switch IME**, then select **SmartKey Clip Keyboard**.
4. Test typing Arabic, English, **تشكيل** (diacritics), Password, and OTP fields in the **Live Keyboard Test Pad** or any external app, and customize Theme (`System`/`Light`/`Dark`), Height, One-Handed Mode, and Toolbar shortcuts under **Appearance & Ergonomics**.

## 2. Using Arabic Tashkeel, Gestures & Bilingual Switching
- Tap **EN / AR** on the bottom row to switch between Arabic and English.
- Tap **تشكيل** in the top toolbar (Arabic mode) to toggle the diacritics strip (`َ ً ُ ٌ ِ ٍ ْ ّ ـ أ إ آ ى ئ ؤ`).
- **Spacebar Cursor Scrubbing:** Drag horizontally across the spacebar to move the cursor smoothly without lifting your finger.
- **Hold-to-Repeat Backspace:** Press and hold Backspace for rapid character deletion.
- Tap the globe icon (if multiple system keyboards are enabled) to switch back to another system keyboard, or tap **More Tools** (`Tune`) to open quick height, theme, number row, and one-handed controls directly inside the IME.

## 3. Using Super Clipboard & Emoji Panel
- **Inside Any App (IME Drawer):** Tap **Clips** or the **Search** icon on the keyboard toolbar to open the inline clipboard drawer. Filter by folder or Pinned, search snippets using the built-in mini-keyboard without leaving the host app, tap **Save** to capture the current system clipboard, pin important clips, or tap any clip to paste immediately.
- **Emoji Panel:** Tap the **Emoji** icon on the toolbar for categorized Smileys, Gestures, Symbols, and Work emojis.
- **In Companion App:** Search across thousands of clips, long-press to multi-select (move folders / bulk delete), and open the top-right **More** menu (`⋮`) to create custom folders, export/import JSON backups via Android's file picker, or run the on-device Room Scale Benchmark.

## 4. Optional Telegram Sync Setup
> **Security Note:** Telegram sync is **OFF by default**. Telegram Bot API uses HTTPS/TLS but is **not** end-to-end encrypted like Secret Chats.
1. Message `@BotFather` on Telegram and run `/newbot` to create a **separate personal bot**.
2. Copy the Bot Token into the **Telegram** tab → tap **Encrypt & Save** (stored via Android Keystore AES-256-GCM).
3. Tap **Verify (getMe)** to confirm the bot token works.
4. Open your new bot in Telegram and send `/start`.
5. Return to SmartKey Clip → tap **Detect Chat** → tap your chat to bind your `Chat ID`.
6. Toggle **Enable Telegram Cloud Sync** ON when ready. Use **Sync Now** to upload queued non-sensitive clips and **Restore** to pull messages sent to your bot.
