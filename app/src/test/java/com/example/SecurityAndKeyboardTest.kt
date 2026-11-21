package com.example

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.test.core.app.ApplicationProvider
import com.example.ime.ImePanelMode
import com.example.ime.KeyboardLanguage
import com.example.ime.KeyboardLayoutMode
import com.example.ime.KeyboardLayouts
import com.example.ime.KeyboardStateController
import com.example.ime.ShiftState
import com.example.ime.SuggestionEngine
import com.example.security.AesGcmCryptoEngine
import com.example.security.CryptoProvider
import com.example.security.LogRedactor
import com.example.security.SecretVault
import com.example.security.SensitiveContentDetector
import com.example.ui.theme.resolveIsDarkTheme
import javax.crypto.KeyGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityAndKeyboardTest {

  @Test
  fun `aes gcm crypto engine encrypts with random iv and detects tampering`() {
    val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    val crypto = AesGcmCryptoEngine(keyProvider = { key })

    val secret = "987654321:AAFakeTelegramBotTokenForUnitTestingOnly12345"
    val enc1 = crypto.encrypt(secret)
    val enc2 = crypto.encrypt(secret)

    assertNotEquals(enc1, enc2)
    assertFalse(enc1.contains(secret))
    assertEquals(secret, crypto.decrypt(enc1))
    assertEquals(secret, crypto.decrypt(enc2))

    // Tamper payload
    val tampered = enc1.dropLast(2) + "XX"
    assertNull(crypto.decrypt(tampered))
  }

  @Test
  fun `log redactor strips bot tokens from urls and exceptions`() {
    val raw = "Failed calling https://api.telegram.org/bot12345678:AAHxyzSecretTokenValue_0123456789/getMe"
    val redacted = LogRedactor.redact(raw)
    assertFalse(redacted.contains("AAHxyzSecretTokenValue"))
    assertTrue(redacted.contains("/bot[REDACTED]/getMe"))

    val noSlash = "Relative: bot111222333:AAFakeTestTokenForMockServerOnly00001/getMe"
    val redactedNoSlash = LogRedactor.redact(noSlash)
    assertFalse(redactedNoSlash.contains("AAFakeTestTokenForMockServerOnly00001"))
    assertTrue(redactedNoSlash.contains("/bot[REDACTED]/getMe"))
  }

  @Test
  fun `sensitive detector flags password fields, otp fields, and tokens`() {
    val pwdInfo = EditorInfo().apply {
      inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }
    val pwdProfile = SensitiveContentDetector.inspectEditorInfo(pwdInfo)
    assertTrue(pwdProfile.isPasswordField)
    assertTrue(pwdProfile.shouldDisableSuggestions)
    assertTrue(pwdProfile.shouldPauseClipboardAutoCapture)

    val normalInfo = EditorInfo().apply {
      inputType = InputType.TYPE_CLASS_TEXT
    }
    val normalProfile = SensitiveContentDetector.inspectEditorInfo(normalInfo)
    assertFalse(normalProfile.isSensitiveField)
    assertFalse(normalProfile.shouldDisableSuggestions)

    assertTrue(SensitiveContentDetector.isSensitiveClipboardText("482910"))
    assertTrue(SensitiveContentDetector.isSensitiveClipboardText("123456789:ABCDEFGHIJKLMNOPQRSTUVWXYZ12345678"))
    assertFalse(SensitiveContentDetector.isSensitiveClipboardText("السلام عليكم ورحمة الله"))
  }

  @Test
  fun `keyboard state controller handles layouts, panels, in-ime clipboard search, and suggestions`() {
    val controller = KeyboardStateController()
    val normalEditor = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }

    controller.onStartInput(normalEditor, KeyboardLanguage.ARABIC)
    assertEquals(KeyboardLanguage.ARABIC, controller.language)
    assertEquals(ImePanelMode.KEYBOARD, controller.panelMode)
    assertTrue(KeyboardLayouts.arabicTashkeelAndForms.contains("\u064E"))

    controller.toggleTashkeelBar()
    assertTrue(controller.showTashkeelBar)

    controller.onCharacterCommitted("ا")
    controller.onCharacterCommitted("ل")
    val arSuggestions = SuggestionEngine.suggest(
      prefix = controller.composingWord,
      language = controller.language,
      securityProfile = controller.securityProfile,
    )
    assertTrue(arSuggestions.isNotEmpty())

    // Panel transitions & in-IME clipboard search state
    controller.setPanel(ImePanelMode.CLIPBOARD, openClipboardSearch = true)
    assertEquals(ImePanelMode.CLIPBOARD, controller.panelMode)
    assertTrue(controller.showClipboardPanel)
    assertTrue(controller.isClipboardSearchActive)

    controller.appendClipboardSearch("ك")
    controller.appendClipboardSearch("ت")
    assertEquals("كت", controller.clipboardSearchQuery)
    controller.backspaceClipboardSearch()
    assertEquals("ك", controller.clipboardSearchQuery)
    controller.clearClipboardSearchQuery()
    assertEquals("", controller.clipboardSearchQuery)

    controller.setPanel(ImePanelMode.EMOJI)
    assertEquals(ImePanelMode.EMOJI, controller.panelMode)
    assertFalse(controller.isClipboardSearchActive)

    controller.setPanel(ImePanelMode.EMOJI) // toggles back to KEYBOARD
    assertEquals(ImePanelMode.KEYBOARD, controller.panelMode)

    controller.toggleLanguage()
    assertEquals(KeyboardLanguage.ENGLISH, controller.language)

    controller.toggleShift()
    assertEquals(ShiftState.ONCE, controller.shiftState)
    controller.onCharacterCommitted("H")
    assertEquals(ShiftState.OFF, controller.shiftState)

    controller.toggleShift()
    controller.toggleShift()
    assertEquals(ShiftState.CAPS_LOCK, controller.shiftState)

    controller.setLayout(KeyboardLayoutMode.SYMBOLS_1)
    assertEquals(KeyboardLayoutMode.SYMBOLS_1, controller.layoutMode)

    // Password editor suppresses suggestions
    val pwdEditor = EditorInfo().apply {
      inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }
    controller.onStartInput(pwdEditor, KeyboardLanguage.ENGLISH)
    val pwdSuggestions = SuggestionEngine.suggest(
      prefix = "pa",
      language = controller.language,
      securityProfile = controller.securityProfile,
    )
    assertTrue(pwdSuggestions.isEmpty())
  }

  @Test
  fun `theme mode resolution and secret vault appearance preferences persist`() {
    assertFalse(resolveIsDarkTheme("LIGHT", systemDark = true))
    assertTrue(resolveIsDarkTheme("DARK", systemDark = false))
    assertTrue(resolveIsDarkTheme("SYSTEM", systemDark = true))
    assertFalse(resolveIsDarkTheme("SYSTEM", systemDark = false))

    val context = ApplicationProvider.getApplicationContext<Context>()
    val stubCrypto = object : CryptoProvider {
      override fun encrypt(plaintext: String): String = plaintext
      override fun decrypt(encodedPayload: String): String = encodedPayload
      override fun isHardwareOrKeystoreBacked(): Boolean = true
    }
    val vault = SecretVault(context, stubCrypto)
    vault.setThemeMode("light")
    vault.setShowNumberRow(false)
    vault.setOneHandedMode("left")
    vault.setKeyboardHeightMode("tall")
    vault.setShowEmojiTool(false)
    vault.setShowSearchTool(false)

    val snapshot = vault.settingsFlow.value
    assertEquals("LIGHT", snapshot.themeMode)
    assertFalse(snapshot.showNumberRow)
    assertEquals("LEFT", snapshot.oneHandedMode)
    assertEquals("TALL", snapshot.keyboardHeightMode)
    assertFalse(snapshot.showEmojiTool)
    assertFalse(snapshot.showSearchTool)
  }
}
