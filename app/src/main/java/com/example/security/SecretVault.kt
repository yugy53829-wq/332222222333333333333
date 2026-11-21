package com.example.security

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VaultSettingsSnapshot(
  val hasBotToken: Boolean = false,
  val maskedBotToken: String = "",
  val chatId: String = "",
  val botUsername: String = "",
  val telegramSyncEnabled: Boolean = false, // Disabled by default per security policy
  val autoCaptureClipboard: Boolean = true,
  val protectPasswordFields: Boolean = true,
  val maskSensitiveClips: Boolean = true,
  val hapticFeedbackEnabled: Boolean = true,
  val activeLanguageCode: String = "AR",
  val themeMode: String = "SYSTEM",
  val showNumberRow: Boolean = true,
  val oneHandedMode: String = "OFF",
  val keyboardHeightMode: String = "STANDARD",
  val showEmojiTool: Boolean = true,
  val showSearchTool: Boolean = true,
  val lastUpdateOffset: Long = 0L,
  val lastSyncEpochMs: Long = 0L,
  val keystoreActive: Boolean = false,
)

class SecretVault(
  context: Context,
  private val cryptoProvider: CryptoProvider = AndroidKeystoreCryptoManager(),
) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _settingsFlow = MutableStateFlow(readSnapshot())
  val settingsFlow: StateFlow<VaultSettingsSnapshot> = _settingsFlow.asStateFlow()

  fun getBotToken(): String {
    val encrypted = prefs.getString(KEY_ENC_BOT_TOKEN, null) ?: return ""
    return cryptoProvider.decrypt(encrypted).orEmpty()
  }

  fun setBotToken(token: String) {
    val trimmed = token.trim()
    val editor = prefs.edit()
    if (trimmed.isEmpty()) {
      editor.remove(KEY_ENC_BOT_TOKEN)
      editor.remove(KEY_BOT_USERNAME)
      editor.putBoolean(KEY_SYNC_ENABLED, false)
    } else {
      editor.putString(KEY_ENC_BOT_TOKEN, cryptoProvider.encrypt(trimmed))
    }
    editor.apply()
    refresh()
  }

  fun getChatId(): String {
    val encrypted = prefs.getString(KEY_ENC_CHAT_ID, null) ?: return ""
    return cryptoProvider.decrypt(encrypted).orEmpty()
  }

  fun setChatId(chatId: String) {
    val trimmed = chatId.trim()
    val editor = prefs.edit()
    if (trimmed.isEmpty()) {
      editor.remove(KEY_ENC_CHAT_ID)
    } else {
      editor.putString(KEY_ENC_CHAT_ID, cryptoProvider.encrypt(trimmed))
    }
    editor.apply()
    refresh()
  }

  fun setBotUsername(username: String) {
    prefs.edit().putString(KEY_BOT_USERNAME, username.trim()).apply()
    refresh()
  }

  fun setTelegramSyncEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply()
    refresh()
  }

  fun setAutoCaptureClipboard(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_AUTO_CAPTURE, enabled).apply()
    refresh()
  }

  fun setProtectPasswordFields(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_PROTECT_PASSWORDS, enabled).apply()
    refresh()
  }

  fun setMaskSensitiveClips(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_MASK_SENSITIVE, enabled).apply()
    refresh()
  }

  fun setHapticFeedbackEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
    refresh()
  }

  fun setActiveLanguageCode(code: String) {
    val normalized = if (code.uppercase() == "EN") "EN" else "AR"
    prefs.edit().putString(KEY_ACTIVE_LANG, normalized).apply()
    refresh()
  }

  fun setThemeMode(mode: String) {
    val normalized = when (mode.uppercase()) {
      "LIGHT", "DARK" -> mode.uppercase()
      else -> "SYSTEM"
    }
    prefs.edit().putString(KEY_THEME_MODE, normalized).apply()
    refresh()
  }

  fun setShowNumberRow(show: Boolean) {
    prefs.edit().putBoolean(KEY_SHOW_NUMBER_ROW, show).apply()
    refresh()
  }

  fun setOneHandedMode(mode: String) {
    val normalized = when (mode.uppercase()) {
      "LEFT", "RIGHT" -> mode.uppercase()
      else -> "OFF"
    }
    prefs.edit().putString(KEY_ONE_HANDED_MODE, normalized).apply()
    refresh()
  }

  fun setKeyboardHeightMode(mode: String) {
    val normalized = when (mode.uppercase()) {
      "COMPACT", "TALL" -> mode.uppercase()
      else -> "STANDARD"
    }
    prefs.edit().putString(KEY_HEIGHT_MODE, normalized).apply()
    refresh()
  }

  fun setShowEmojiTool(show: Boolean) {
    prefs.edit().putBoolean(KEY_SHOW_EMOJI_TOOL, show).apply()
    refresh()
  }

  fun setShowSearchTool(show: Boolean) {
    prefs.edit().putBoolean(KEY_SHOW_SEARCH_TOOL, show).apply()
    refresh()
  }

  fun setLastUpdateOffset(offset: Long) {
    prefs.edit().putLong(KEY_LAST_UPDATE_OFFSET, offset).apply()
    refresh()
  }

  fun recordSyncTimestamp(epochMs: Long = System.currentTimeMillis()) {
    prefs.edit().putLong(KEY_LAST_SYNC_EPOCH, epochMs).apply()
    refresh()
  }

  fun clearTelegramCredentials() {
    prefs.edit()
      .remove(KEY_ENC_BOT_TOKEN)
      .remove(KEY_ENC_CHAT_ID)
      .remove(KEY_BOT_USERNAME)
      .remove(KEY_LAST_UPDATE_OFFSET)
      .putBoolean(KEY_SYNC_ENABLED, false)
      .apply()
    refresh()
  }

  fun hasPlaintextBotTokenInPrefs(): Boolean {
    val allEntries = prefs.all
    return allEntries.values.any { value ->
      value is String && SensitiveContentDetector.containsBotToken(value)
    }
  }

  private fun refresh() {
    _settingsFlow.value = readSnapshot()
  }

  private fun readSnapshot(): VaultSettingsSnapshot {
    val token = getBotToken()
    val masked = if (token.length >= 10) {
      "${token.take(5)}•••••${token.takeLast(4)}"
    } else if (token.isNotEmpty()) {
      "•••••"
    } else {
      ""
    }
    return VaultSettingsSnapshot(
      hasBotToken = token.isNotEmpty(),
      maskedBotToken = masked,
      chatId = getChatId(),
      botUsername = prefs.getString(KEY_BOT_USERNAME, "").orEmpty(),
      telegramSyncEnabled = prefs.getBoolean(KEY_SYNC_ENABLED, false),
      autoCaptureClipboard = prefs.getBoolean(KEY_AUTO_CAPTURE, true),
      protectPasswordFields = prefs.getBoolean(KEY_PROTECT_PASSWORDS, true),
      maskSensitiveClips = prefs.getBoolean(KEY_MASK_SENSITIVE, true),
      hapticFeedbackEnabled = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true),
      activeLanguageCode = prefs.getString(KEY_ACTIVE_LANG, "AR") ?: "AR",
      themeMode = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM",
      showNumberRow = prefs.getBoolean(KEY_SHOW_NUMBER_ROW, true),
      oneHandedMode = prefs.getString(KEY_ONE_HANDED_MODE, "OFF") ?: "OFF",
      keyboardHeightMode = prefs.getString(KEY_HEIGHT_MODE, "STANDARD") ?: "STANDARD",
      showEmojiTool = prefs.getBoolean(KEY_SHOW_EMOJI_TOOL, true),
      showSearchTool = prefs.getBoolean(KEY_SHOW_SEARCH_TOOL, true),
      lastUpdateOffset = prefs.getLong(KEY_LAST_UPDATE_OFFSET, 0L),
      lastSyncEpochMs = prefs.getLong(KEY_LAST_SYNC_EPOCH, 0L),
      keystoreActive = cryptoProvider.isHardwareOrKeystoreBacked(),
    )
  }

  companion object {
    const val PREFS_NAME = "smartkey_secret_vault"
    private const val KEY_ENC_BOT_TOKEN = "enc_bot_token"
    private const val KEY_ENC_CHAT_ID = "enc_chat_id"
    private const val KEY_BOT_USERNAME = "bot_username"
    private const val KEY_SYNC_ENABLED = "telegram_sync_enabled"
    private const val KEY_AUTO_CAPTURE = "auto_capture_clipboard"
    private const val KEY_PROTECT_PASSWORDS = "protect_password_fields"
    private const val KEY_MASK_SENSITIVE = "mask_sensitive_clips"
    private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback_enabled"
    private const val KEY_ACTIVE_LANG = "active_language_code"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_SHOW_NUMBER_ROW = "show_number_row"
    private const val KEY_ONE_HANDED_MODE = "one_handed_mode"
    private const val KEY_HEIGHT_MODE = "keyboard_height_mode"
    private const val KEY_SHOW_EMOJI_TOOL = "show_emoji_tool"
    private const val KEY_SHOW_SEARCH_TOOL = "show_search_tool"
    private const val KEY_LAST_UPDATE_OFFSET = "last_update_offset"
    private const val KEY_LAST_SYNC_EPOCH = "last_sync_epoch_ms"
  }
}
