package com.example.ime

import android.view.inputmethod.EditorInfo
import com.example.security.EditorSecurityProfile
import com.example.security.SensitiveContentDetector

enum class KeyboardLanguage {
  ARABIC,
  ENGLISH,
}

enum class KeyboardLayoutMode {
  LETTERS,
  SYMBOLS_1,
  SYMBOLS_2,
}

enum class ShiftState {
  OFF,
  ONCE,
  CAPS_LOCK,
}

enum class ImePanelMode {
  KEYBOARD,
  CLIPBOARD,
  EMOJI,
  TOOLS,
}

data class KeySpec(
  val primary: String,
  val secondary: String? = null,
  val weight: Float = 1f,
  val action: KeyAction = KeyAction.CommitText(primary),
)

sealed interface KeyAction {
  data class CommitText(val text: String) : KeyAction
  data object Backspace : KeyAction
  data object Space : KeyAction
  data object Enter : KeyAction
  data object Shift : KeyAction
  data object SwitchLanguage : KeyAction
  data object SwitchToNextIme : KeyAction
  data class MoveCursor(val delta: Int) : KeyAction
  data class SwitchLayout(val target: KeyboardLayoutMode) : KeyAction
}

object EmojiCatalog {
  val categories: List<Pair<String, List<String>>> = listOf(
    "Smileys" to listOf(
      "😀", "😃", "😄", "😁", "😊", "🙂", "😉", "😍",
      "🥰", "😘", "🤔", "🤨", "😎", "🥳", "🤩", "😴",
      "😅", "😂", "🤣", "🥹", "😢", "😭", "😤", "🙏",
    ),
    "Gestures" to listOf(
      "👍", "👎", "👏", "🙌", "🤝", "👌", "✌️", "🤞",
      "🤟", "👋", "🫶", "💪", "☝️", "👉", "👈", "🫡",
    ),
    "Symbols" to listOf(
      "❤️", "🩵", "💚", "💛", "🧡", "🤍", "✨", "🔥",
      "⭐", "✅", "⚡", "🎯", "📌", "🔒", "🔑", "💡",
    ),
    "Work" to listOf(
      "📱", "💻", "⌨️", "📋", "📝", "📁", "📅", "⏰",
      "📧", "📞", "🚀", "🛠️", "📊", "🌍", "☕", "🎉",
    ),
  )
}

object KeyboardLayouts {

  val arabicTashkeelAndForms: List<String> = listOf(
    "\u064E", // Fatha
    "\u064B", // Tanwin Fath
    "\u064F", // Damma
    "\u064C", // Tanwin Damm
    "\u0650", // Kasra
    "\u064D", // Tanwin Kasr
    "\u0652", // Sukun
    "\u0651", // Shadda
    "\u0640", // Tatweel
    "أ",
    "إ",
    "آ",
    "ى",
    "ئ",
    "ؤ",
  )

  val numberQuickRow: List<String> = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

  fun rowsFor(
    language: KeyboardLanguage,
    mode: KeyboardLayoutMode,
    shiftState: ShiftState,
  ): List<List<KeySpec>> {
    return when (mode) {
      KeyboardLayoutMode.LETTERS -> {
        if (language == KeyboardLanguage.ARABIC) arabicLetterRows()
        else englishLetterRows(shiftState != ShiftState.OFF)
      }
      KeyboardLayoutMode.SYMBOLS_1 -> symbolsPrimaryRows(language)
      KeyboardLayoutMode.SYMBOLS_2 -> symbolsExtendedRows(language)
    }
  }

  private fun arabicLetterRows(): List<List<KeySpec>> {
    val row1 = listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د")
      .map { KeySpec(it) }
    val row2 = listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط")
      .map { KeySpec(it) }
    val row3 = buildList {
      add(KeySpec("ئ", weight = 1f))
      add(KeySpec("ء", weight = 1f))
      add(KeySpec("ؤ", weight = 1f))
      add(KeySpec("ر", weight = 1f))
      add(KeySpec("لا", weight = 1.15f))
      add(KeySpec("ى", weight = 1f))
      add(KeySpec("ة", weight = 1f))
      add(KeySpec("و", weight = 1f))
      add(KeySpec("ز", weight = 1f))
      add(KeySpec("ظ", weight = 1f))
      add(KeySpec("BS", weight = 1.55f, action = KeyAction.Backspace))
    }
    val row4 = listOf(
      KeySpec("؟123", weight = 1.45f, action = KeyAction.SwitchLayout(KeyboardLayoutMode.SYMBOLS_1)),
      KeySpec("EN", weight = 1.2f, action = KeyAction.SwitchLanguage),
      KeySpec("،", weight = 0.85f),
      KeySpec("العربية", weight = 4.1f, action = KeyAction.Space),
      KeySpec(".", weight = 0.85f),
      KeySpec("؟", weight = 0.85f),
      KeySpec("ENTER", weight = 1.65f, action = KeyAction.Enter),
    )
    return listOf(row1, row2, row3, row4)
  }

  private fun englishLetterRows(upperCase: Boolean): List<List<KeySpec>> {
    fun ch(c: String) = if (upperCase) c.uppercase() else c
    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map { KeySpec(ch(it)) }
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").map { KeySpec(ch(it)) }
    val row3 = buildList {
      add(KeySpec("SHIFT", weight = 1.45f, action = KeyAction.Shift))
      listOf("z", "x", "c", "v", "b", "n", "m").forEach { add(KeySpec(ch(it), weight = 1f)) }
      add(KeySpec("BS", weight = 1.45f, action = KeyAction.Backspace))
    }
    val row4 = listOf(
      KeySpec("?123", weight = 1.45f, action = KeyAction.SwitchLayout(KeyboardLayoutMode.SYMBOLS_1)),
      KeySpec("AR", weight = 1.2f, action = KeyAction.SwitchLanguage),
      KeySpec(",", weight = 0.85f),
      KeySpec("English", weight = 4.1f, action = KeyAction.Space),
      KeySpec(".", weight = 0.85f),
      KeySpec("ENTER", weight = 1.65f, action = KeyAction.Enter),
    )
    return listOf(row1, row2, row3, row4)
  }

  private fun symbolsPrimaryRows(language: KeyboardLanguage): List<List<KeySpec>> {
    val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { KeySpec(it) }
    val row2 = listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/").map { KeySpec(it) }
    val row3 = buildList {
      add(KeySpec("=\\<", weight = 1.45f, action = KeyAction.SwitchLayout(KeyboardLayoutMode.SYMBOLS_2)))
      listOf("*", "\"", "'", ":", ";", "!", "?").forEach { add(KeySpec(it, weight = 1f)) }
      add(KeySpec("BS", weight = 1.45f, action = KeyAction.Backspace))
    }
    val abcLabel = if (language == KeyboardLanguage.ARABIC) "أبج" else "ABC"
    val row4 = listOf(
      KeySpec(abcLabel, weight = 1.5f, action = KeyAction.SwitchLayout(KeyboardLayoutMode.LETTERS)),
      KeySpec(if (language == KeyboardLanguage.ARABIC) "EN" else "AR", weight = 1.2f, action = KeyAction.SwitchLanguage),
      KeySpec(",", weight = 0.85f),
      KeySpec("Space", weight = 3.9f, action = KeyAction.Space),
      KeySpec(".", weight = 0.85f),
      KeySpec("ENTER", weight = 1.65f, action = KeyAction.Enter),
    )
    return listOf(row1, row2, row3, row4)
  }

  private fun symbolsExtendedRows(language: KeyboardLanguage): List<List<KeySpec>> {
    val row1 = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆").map { KeySpec(it) }
    val row2 = listOf("£", "€", "¥", "¢", "^", "°", "=", "{", "}", "\\").map { KeySpec(it) }
    val row3 = buildList {
      add(KeySpec("123", weight = 1.45f, action = KeyAction.SwitchLayout(KeyboardLayoutMode.SYMBOLS_1)))
      listOf("%", "©", "®", "™", "✓", "[", "]").forEach { add(KeySpec(it, weight = 1f)) }
      add(KeySpec("BS", weight = 1.45f, action = KeyAction.Backspace))
    }
    val abcLabel = if (language == KeyboardLanguage.ARABIC) "أبج" else "ABC"
    val row4 = listOf(
      KeySpec(abcLabel, weight = 1.5f, action = KeyAction.SwitchLayout(KeyboardLayoutMode.LETTERS)),
      KeySpec(if (language == KeyboardLanguage.ARABIC) "EN" else "AR", weight = 1.2f, action = KeyAction.SwitchLanguage),
      KeySpec("<", weight = 0.85f),
      KeySpec("Space", weight = 3.9f, action = KeyAction.Space),
      KeySpec(">", weight = 0.85f),
      KeySpec("ENTER", weight = 1.65f, action = KeyAction.Enter),
    )
    return listOf(row1, row2, row3, row4)
  }
}

object SuggestionEngine {
  private val englishDictionary = listOf(
    "hello", "thanks", "thank", "please", "confirm", "update", "project", "schedule",
    "meeting", "address", "email", "message", "android", "keyboard", "clipboard",
    "secure", "verified", "testing", "report", "tomorrow", "today", "working",
    "ready", "sending", "receive", "account", "backup", "folder", "pinned",
  )

  private val arabicDictionary = listOf(
    "السلام", "عليكم", "مرحبا", "شكرا", "جزيلا", "تطبيق", "لوحة", "المفاتيح",
    "الحافظة", "الذكية", "نسخ", "لصق", "حفظ", "تعديل", "مجلد", "مزامنة",
    "آمن", "ممتاز", "اليوم", "غدا", "إن", "شاء", "الله", "العربية",
    "رسالة", "تأكيد", "بيانات", "تشفير", "إعدادات", "تشغيل",
  )

  fun suggest(
    prefix: String,
    language: KeyboardLanguage,
    securityProfile: EditorSecurityProfile,
    pinnedPhrases: List<String> = emptyList(),
  ): List<String> {
    if (securityProfile.shouldDisableSuggestions) return emptyList()
    val cleanPrefix = prefix.trim()
    if (cleanPrefix.isEmpty()) {
      return when (language) {
        KeyboardLanguage.ARABIC -> listOf("السلام عليكم", "شكراً لك", "تم الحفظ")
        KeyboardLanguage.ENGLISH -> listOf("Hello", "Thank you", "Confirmed")
      }
    }

    val lower = cleanPrefix.lowercase()
    val dict = if (language == KeyboardLanguage.ARABIC) arabicDictionary else englishDictionary
    val matches = LinkedHashSet<String>()

    pinnedPhrases.forEach { phrase ->
      val trimmed = phrase.trim()
      if (trimmed.length in 2..28 && trimmed.lowercase().startsWith(lower)) {
        matches.add(trimmed)
      }
    }
    dict.forEach { word ->
      if (word.lowercase().startsWith(lower) && word.lowercase() != lower) {
        matches.add(word)
      }
    }
    return matches.take(4)
  }
}

class KeyboardStateController {
  var language: KeyboardLanguage = KeyboardLanguage.ARABIC
    private set
  var layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.LETTERS
    private set
  var shiftState: ShiftState = ShiftState.OFF
    private set
  var panelMode: ImePanelMode = ImePanelMode.KEYBOARD
    private set
  var showTashkeelBar: Boolean = false
    private set
  var isClipboardSearchActive: Boolean = false
    private set
  var clipboardSearchQuery: String = ""
    private set
  var composingWord: String = ""
    private set
  var securityProfile: EditorSecurityProfile = SensitiveContentDetector.inspectEditorInfo(null)
    private set
  var enterActionLabel: String = "↵"
    private set

  val showClipboardPanel: Boolean
    get() = panelMode == ImePanelMode.CLIPBOARD

  fun onStartInput(editorInfo: EditorInfo?, defaultLanguage: KeyboardLanguage) {
    securityProfile = SensitiveContentDetector.inspectEditorInfo(editorInfo)
    language = defaultLanguage
    layoutMode = if (securityProfile.isOtpOrNumericCode) {
      KeyboardLayoutMode.SYMBOLS_1
    } else {
      KeyboardLayoutMode.LETTERS
    }
    panelMode = ImePanelMode.KEYBOARD
    isClipboardSearchActive = false
    clipboardSearchQuery = ""
    shiftState = ShiftState.OFF
    composingWord = ""
    enterActionLabel = resolveActionLabel(editorInfo)
  }

  fun toggleLanguage() {
    language = if (language == KeyboardLanguage.ARABIC) {
      KeyboardLanguage.ENGLISH
    } else {
      KeyboardLanguage.ARABIC
    }
    layoutMode = KeyboardLayoutMode.LETTERS
    shiftState = ShiftState.OFF
    composingWord = ""
  }

  fun setLayout(target: KeyboardLayoutMode) {
    layoutMode = target
    composingWord = ""
  }

  fun toggleShift() {
    shiftState = when (shiftState) {
      ShiftState.OFF -> ShiftState.ONCE
      ShiftState.ONCE -> ShiftState.CAPS_LOCK
      ShiftState.CAPS_LOCK -> ShiftState.OFF
    }
  }

  fun toggleTashkeelBar() {
    showTashkeelBar = !showTashkeelBar
  }

  fun setPanel(mode: ImePanelMode, openClipboardSearch: Boolean = false) {
    panelMode = if (panelMode == mode && !openClipboardSearch) ImePanelMode.KEYBOARD else mode
    if (panelMode != ImePanelMode.CLIPBOARD) {
      isClipboardSearchActive = false
      clipboardSearchQuery = ""
    } else if (openClipboardSearch) {
      isClipboardSearchActive = true
    }
  }

  fun toggleClipboardPanel(forceValue: Boolean? = null) {
    val open = forceValue ?: (panelMode != ImePanelMode.CLIPBOARD)
    panelMode = if (open) ImePanelMode.CLIPBOARD else ImePanelMode.KEYBOARD
    if (!open) {
      isClipboardSearchActive = false
      clipboardSearchQuery = ""
    }
  }

  fun setClipboardSearchMode(active: Boolean) {
    isClipboardSearchActive = active
    if (!active) clipboardSearchQuery = ""
  }

  fun appendClipboardSearch(text: String) {
    clipboardSearchQuery += text
  }

  fun backspaceClipboardSearch() {
    if (clipboardSearchQuery.isNotEmpty()) {
      clipboardSearchQuery = clipboardSearchQuery.dropLast(1)
    }
  }

  fun clearClipboardSearchQuery() {
    clipboardSearchQuery = ""
  }

  fun onCharacterCommitted(text: String) {
    if (text.length == 1 && (text[0].isLetter() || text[0] in '\u064B'..'\u0652')) {
      composingWord += text
    } else {
      composingWord = ""
    }
    if (shiftState == ShiftState.ONCE) {
      shiftState = ShiftState.OFF
    }
  }

  fun onBackspace() {
    if (composingWord.isNotEmpty()) {
      composingWord = composingWord.dropLast(1)
    }
  }

  fun clearComposingWord() {
    composingWord = ""
  }

  companion object {
    fun resolveActionLabel(editorInfo: EditorInfo?): String {
      if (editorInfo == null) return "↵"
      return when (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION) {
        EditorInfo.IME_ACTION_SEARCH -> "Search"
        EditorInfo.IME_ACTION_GO -> "Go"
        EditorInfo.IME_ACTION_SEND -> "Send"
        EditorInfo.IME_ACTION_NEXT -> "Next"
        EditorInfo.IME_ACTION_DONE -> "Done"
        else -> "↵"
      }
    }
  }
}
