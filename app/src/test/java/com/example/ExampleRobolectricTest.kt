package com.example

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.ime.ImePanelMode
import com.example.ime.SmartKeyboardImeService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `app name matches SmartKey Clip`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("SmartKey Clip", context.getString(R.string.app_name))
  }

  @Test
  fun `main activity navigates across all four studio tabs and switches theme`() {
    composeRule.waitForIdle()
    composeRule.onNodeWithText("System Keyboard Activation").assertIsDisplayed()

    composeRule.onNodeWithTag("keyboard_setup_list").performScrollToIndex(2)
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("test_mode_password").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("ime_test_password_input").assertIsDisplayed()

    composeRule.onNodeWithTag("keyboard_setup_list").performScrollToIndex(3)
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("theme_chip_light").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("theme_chip_dark").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("tab_clipboard").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Super Clipboard").assertIsDisplayed()

    composeRule.onNodeWithTag("tab_telegram").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Optional Telegram Sync").assertIsDisplayed()

    composeRule.onNodeWithTag("tab_audit").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Security, Privacy & Audit").assertIsDisplayed()
    composeRule.onNodeWithTag("audit_list").performScrollToNode(hasTestTag("toggle_protect_passwords"))
    composeRule.onNodeWithTag("toggle_protect_passwords").assertIsDisplayed()
  }

  @Test
  fun `ime service initializes lifecycle and input view for arabic and password editors`() {
    val controller = Robolectric.buildService(SmartKeyboardImeService::class.java).create()
    val service = controller.get()
    val inputView = service.onCreateInputView()
    assertNotNull(inputView)

    val normalEditor = EditorInfo().apply {
      inputType = InputType.TYPE_CLASS_TEXT
    }
    service.onStartInputView(normalEditor, false)

    val passwordEditor = EditorInfo().apply {
      inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }
    service.onStartInputView(passwordEditor, false)
    service.onFinishInputView(true)
    controller.destroy()
  }
}
