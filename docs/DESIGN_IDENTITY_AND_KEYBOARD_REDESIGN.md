# Checkpoint 3 — SmartKey Clip Visual Identity & Native Keyboard Redesign

## 1. Visual Identity ("SmartKey Clip")
- **Design Philosophy:** Calm Modern Utility & Material Design 3 — crisp borders (`1.dp`), high-legibility spacing (`8.dp` grid), no glassmorphism, no neon noise, no emojis on UI navigation or toolbar buttons.
- **Bilingual Typography:** `Plus Jakarta Sans` (English & UI headings), `Noto Sans Arabic` (RTL Arabic clips and Arabic keyboard labels), `JetBrains Mono` (counters, cryptographic/benchmark telemetry).
- **Light Theme (Daylight Alabaster):** Background `#F6F8FC`, Surface `#FFFFFF`, Surface Container `#EDF2F7`, Primary Cobalt `#0F6CBD`, Secondary Mineral Teal `#0D9488`, Text Primary `#0F172A`.
- **Dark Theme (Neutral Graphite):** Background `#111318`, Surface `#1B1F27`, Surface Container `#242A35`, Primary Sky Cobalt `#6CB4FF`, Secondary Soft Teal `#4FD1C5`, Text Primary `#F1F5F9`.
- **Keyboard Palettes (`LightKeyboardPalette` / `DarkKeyboardPalette`):** Dedicated keycap fill, pressed highlight, subtle `1.5.dp` bottom keycap lip shadow, primary accent Enter key, and protected password/OTP banner contrast.

## 2. Native Android IME Redesign (`com.example.ime`)
- **Dual Suggestion Strip / Customizable Toolbar:** Displays live Arabic/English suggestions while composing (`SuggestionEngine`), and flips cleanly to a customizable Gboard-style quick toolbar (`Clips` pill, optional `Search`, optional `Emoji`, Arabic `تشكيل`, `More Tools`, `Globe` IME switch, `Settings`).
- **Keycap Ergonomics & Gestures:**
  - Refined `1.5.dp` bottom keycap lip and pressed feedback.
  - Material icons for Shift/CapsLock, Backspace, Return/Search/Send/Done, Globe, Search, and Emoji.
  - **Hold-to-Repeat Backspace:** Initial `340ms` delay followed by `55ms` rapid deletion steps.
  - **Spacebar Cursor Scrubbing:** Horizontal drag gestures emit `KeyAction.MoveCursor` (`KEYCODE_DPAD_LEFT` / `KEYCODE_DPAD_RIGHT`).
  - **Optional Dedicated Number Row (`1–0`)** & **3 Keyboard Heights** (`COMPACT`, `STANDARD`, `TALL`).
  - **One-Handed Reach Mode** (`OFF`, `LEFT`, `RIGHT`) with a side collapse/flip rail.
- **In-IME Panels:**
  - `CLIPBOARD`: Folder/Pinned filter chips, one-tap **Save** capture, RTL-aware clip cards with sensitive masking, and **In-IME Clipboard Search** driven by a compact built-in mini-keyboard without stealing host `InputConnection` focus.
  - `EMOJI`: Categorized (`Smileys`, `Gestures`, `Symbols`, `Work`) 7-column emoji grid with quick backspace.
  - `TOOLS`: Quick toggles for Theme (`System`/`Light`/`Dark`), Height, Number Row, One-Handed Mode, and Companion Studio launcher.

## 3. Companion Studio Reorganization
- **Keyboard (Home):** Activation status (`StatusPill`), 2-step Enable/Switch buttons, single-mode **Live Keyboard Test Pad** (`Bilingual Text` / `Password` / `OTP Code`), and unified **Appearance & Ergonomics** card.
- **Clipboard:** Top overflow menu (`New Folder`, `Export JSON`, `Import JSON`, `Room Scale Benchmark` dialog), search input, folder/pinned filter chips, RTL/Arabic typography detection per clip, and multi-select bulk move/delete bar.
- **Telegram:** Opt-in switch (`OFF` by default), Keystore AES-256-GCM credentials card, `getMe`/`getUpdates` verification & sync actions, and Light/Dark-aware Bot API security disclosure.
- **Security (Audit):** Moved clipboard capture & password/OTP masking toggles here alongside live Keystore/backup posture checks, privacy summary, and quick start guide.
