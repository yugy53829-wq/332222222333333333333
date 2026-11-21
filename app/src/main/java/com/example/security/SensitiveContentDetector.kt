package com.example.security

import android.os.Build
import android.text.InputType
import android.view.inputmethod.EditorInfo

data class EditorSecurityProfile(
  val isPasswordField: Boolean,
  val isOtpOrNumericCode: Boolean,
  val isSensitiveField: Boolean,
  val shouldDisableSuggestions: Boolean,
  val shouldPauseClipboardAutoCapture: Boolean,
)

object SensitiveContentDetector {

  private val BOT_TOKEN_REGEX = Regex("""\b\d{6,12}:[A-Za-z0-9_-]{30,}\b""")
  private val PRIVATE_KEY_REGEX = Regex("""-----BEGIN [A-Z ]*PRIVATE KEY-----""")
  private val JWT_REGEX = Regex("""\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b""")
  private val STANDALONE_OTP_REGEX = Regex("""^\d{4,8}$""")

  fun inspectEditorInfo(info: EditorInfo?): EditorSecurityProfile {
    if (info == null) {
      return EditorSecurityProfile(
        isPasswordField = false,
        isOtpOrNumericCode = false,
        isSensitiveField = false,
        shouldDisableSuggestions = false,
        shouldPauseClipboardAutoCapture = false,
      )
    }

    val inputType = info.inputType
    val klass = inputType and InputType.TYPE_MASK_CLASS
    val variation = inputType and InputType.TYPE_MASK_VARIATION
    val flags = inputType and InputType.TYPE_MASK_FLAGS

    val isTextPassword = klass == InputType.TYPE_CLASS_TEXT && (
      variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
        variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
      )
    val isNumberPassword = klass == InputType.TYPE_CLASS_NUMBER &&
      variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD

    val isNoPersonalized = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      (info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
    } else {
      false
    }
    val hintText = info.hintText?.toString()?.lowercase().orEmpty()
    val fieldName = info.fieldName?.lowercase().orEmpty()
    val combinedCue = "$hintText $fieldName"

    val hasPasswordHint = combinedCue.contains("password") ||
      combinedCue.contains("passcode") ||
      combinedCue.contains("كلمة المرور") ||
      combinedCue.contains("كلمة السر")
    val isOtpHint = combinedCue.contains("otp") ||
      combinedCue.contains("one_time") ||
      combinedCue.contains("onetime") ||
      combinedCue.contains("verification") ||
      combinedCue.contains("2fa") ||
      combinedCue.contains("رمز التحقق") ||
      combinedCue.contains("رمز التأكيد")

    val isPassword = isTextPassword || isNumberPassword || hasPasswordHint
    val isOtp = isOtpHint || (klass == InputType.TYPE_CLASS_NUMBER && isNumberPassword)
    val isNoSuggestionsFlag = (flags and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0

    val isSensitive = isPassword || isOtp || isNoPersonalized
    return EditorSecurityProfile(
      isPasswordField = isPassword,
      isOtpOrNumericCode = isOtp,
      isSensitiveField = isSensitive,
      shouldDisableSuggestions = isSensitive || isNoSuggestionsFlag,
      shouldPauseClipboardAutoCapture = isSensitive,
    )
  }

  fun isSensitiveClipboardText(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return false
    return BOT_TOKEN_REGEX.containsMatchIn(trimmed) ||
      PRIVATE_KEY_REGEX.containsMatchIn(trimmed) ||
      JWT_REGEX.containsMatchIn(trimmed) ||
      STANDALONE_OTP_REGEX.matches(trimmed)
  }

  fun containsBotToken(text: String): Boolean = BOT_TOKEN_REGEX.containsMatchIn(text)

  fun maskForDisplay(text: String): String {
    val trimmed = text.trim()
    return when {
      trimmed.length <= 4 -> "••••"
      trimmed.length <= 12 -> "${trimmed.take(2)}••••${trimmed.takeLast(2)}"
      else -> "${trimmed.take(4)}••••••••${trimmed.takeLast(4)}"
    }
  }
}

object LogRedactor {
  private val TELEGRAM_PATH_TOKEN_REGEX = Regex("""/?bot\d{6,12}:[A-Za-z0-9_-]+""")
  private val RAW_BOT_TOKEN_REGEX = Regex("""\d{6,12}:[A-Za-z0-9_-]{25,}""")

  fun redact(input: String?): String {
    if (input.isNullOrEmpty()) return ""
    return input
      .replace(TELEGRAM_PATH_TOKEN_REGEX, "/bot[REDACTED]")
      .replace(RAW_BOT_TOKEN_REGEX, "[REDACTED_TOKEN]")
  }
}
