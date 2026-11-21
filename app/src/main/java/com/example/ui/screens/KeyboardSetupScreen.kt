package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.security.VaultSettingsSnapshot
import com.example.ui.ImeRegistrationStatus
import com.example.ui.theme.AmberContainerDark
import com.example.ui.theme.AmberContainerLight
import com.example.ui.theme.AmberOnContainerDark
import com.example.ui.theme.AmberOnContainerLight
import com.example.ui.theme.ArabicBodyFont
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.EmeraldOkDark
import com.example.ui.theme.Radius
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.SmartKeyCard
import com.example.ui.theme.Spacing
import com.example.ui.theme.StatusPill
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberDark

private enum class TestPadMode(val label: String) {
  TEXT("Bilingual Text"),
  PASSWORD("Password"),
  OTP("OTP Code"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyboardSetupScreen(
  imeStatus: ImeRegistrationStatus,
  vaultSettings: VaultSettingsSnapshot,
  totalClips: Int,
  onRefreshImeStatus: () -> Unit,
  onSetDefaultLanguage: (String) -> Unit,
  onSetThemeMode: (String) -> Unit,
  onSetKeyboardHeightMode: (String) -> Unit,
  onSetOneHandedMode: (String) -> Unit,
  onSetShowNumberRow: (Boolean) -> Unit,
  onSetShowEmojiTool: (Boolean) -> Unit,
  onSetShowSearchTool: (Boolean) -> Unit,
  onSetHaptic: (Boolean) -> Unit,
) {
  val context = LocalContext.current
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val okTint = if (isDark) EmeraldOkDark else EmeraldOk
  val warnTint = if (isDark) WarmAmberDark else WarmAmber
  val warnBg = if (isDark) AmberContainerDark else AmberContainerLight
  val warnFg = if (isDark) AmberOnContainerDark else AmberOnContainerLight

  var padMode by remember { mutableStateOf(TestPadMode.TEXT) }
  var testText by remember { mutableStateOf("") }
  var testPassword by remember { mutableStateOf("") }
  var testOtp by remember { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("keyboard_setup_list"),
    contentPadding = PaddingValues(Spacing.lg),
    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Editorial Header
    item {
      Row(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "SmartKey Clip",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Arabic & English Keyboard • $totalClips clips saved locally",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        StatusPill(
          text = if (imeStatus.isImeSelectedDefault) "Active IME" else "Setup Needed",
          icon = if (imeStatus.isImeSelectedDefault) Icons.Filled.CheckCircle else Icons.Filled.Warning,
          containerColor = if (imeStatus.isImeSelectedDefault) {
            MaterialTheme.colorScheme.secondaryContainer
          } else {
            warnBg
          },
          contentColor = if (imeStatus.isImeSelectedDefault) {
            MaterialTheme.colorScheme.onSecondaryContainer
          } else {
            warnFg
          },
        )
      }
    }

    // 1. IME Activation Card
    item {
      SmartKeyCard(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Filled.Keyboard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            }
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
              Text("System Keyboard Activation", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "2-step Android Input Method setup",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          IconButton(
            onClick = onRefreshImeStatus,
            modifier = Modifier.testTag("refresh_ime_status_button"),
          ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh IME Status")
          }
        }

        StepStatusRow(
          step = "1",
          title = "Enable SmartKey Clip in System Settings",
          done = imeStatus.isImeEnabled,
          doneText = "Enabled",
          pendingText = "Pending",
          okColor = okTint,
          warnColor = warnTint,
        )
        StepStatusRow(
          step = "2",
          title = "Select SmartKey Clip as Active Keyboard",
          done = imeStatus.isImeSelectedDefault,
          doneText = "Selected",
          pendingText = "Not active",
          okColor = okTint,
          warnColor = warnTint,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
          Button(
            onClick = {
              val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              context.startActivity(intent)
            },
            shape = RoundedCornerShape(Radius.chip),
            modifier = Modifier.weight(1f).testTag("enable_ime_settings_button"),
          ) {
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Enable IME")
          }

          OutlinedButton(
            onClick = {
              val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
              imm?.showInputMethodPicker()
            },
            shape = RoundedCornerShape(Radius.chip),
            modifier = Modifier.weight(1f).testTag("switch_ime_picker_button"),
          ) {
            Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Switch IME")
          }
        }
      }
    }

    // 2. Compact Interactive Test Pad (with Mode Toggle to prevent visual crowding)
    item {
      SmartKeyCard(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        SectionHeader(
          title = "Live Keyboard Test Pad",
          subtitle = "Test bilingual typing, Tashkeel diacritics, inline clipboard paste, and password protection.",
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
          verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
          TestPadMode.entries.forEach { mode ->
            FilterChip(
              selected = padMode == mode,
              onClick = { padMode = mode },
              label = { Text(mode.label) },
              modifier = Modifier.testTag("test_mode_${mode.name.lowercase()}"),
            )
          }
        }

        when (padMode) {
          TestPadMode.TEXT -> {
            val containsArabic = remember(testText) { testText.any { it in '\u0600'..'\u06FF' } }
            CompositionLocalProvider(
              LocalLayoutDirection provides if (containsArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
              OutlinedTextField(
                value = testText,
                onValueChange = { testText = it },
                label = { Text("Arabic & English Editor") },
                placeholder = { Text("اكتب هنا أو الصق من الحافظة...") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                  fontFamily = if (containsArabic) ArabicBodyFont else MaterialTheme.typography.bodyLarge.fontFamily,
                ),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(Radius.chip),
                modifier = Modifier.fillMaxWidth().testTag("ime_test_editor_input"),
              )
            }
          }
          TestPadMode.PASSWORD -> {
            OutlinedTextField(
              value = testPassword,
              onValueChange = { testPassword = it },
              label = { Text("Protected Password Field") },
              leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              singleLine = true,
              shape = RoundedCornerShape(Radius.chip),
              modifier = Modifier.fillMaxWidth().testTag("ime_test_password_input"),
            )
          }
          TestPadMode.OTP -> {
            OutlinedTextField(
              value = testOtp,
              onValueChange = { testOtp = it },
              label = { Text("Protected OTP Code") },
              leadingIcon = { Icon(Icons.Filled.Pin, contentDescription = null) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
              singleLine = true,
              shape = RoundedCornerShape(Radius.chip),
              modifier = Modifier.fillMaxWidth().testTag("ime_test_otp_input"),
            )
          }
        }
      }
    }

    // 3. Appearance & Ergonomics
    item {
      SmartKeyCard(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        SectionHeader(
          title = "Appearance & Ergonomics",
          subtitle = "Applies live to both the studio interface and the native Android keyboard.",
        )

        // Theme Mode
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("App & Keyboard Theme", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(
              Triple("SYSTEM", "System", Icons.Filled.Settings),
              Triple("LIGHT", "Light", Icons.Filled.LightMode),
              Triple("DARK", "Dark", Icons.Filled.DarkMode),
            ).forEach { (code, label, icon) ->
              FilterChip(
                selected = vaultSettings.themeMode == code,
                onClick = { onSetThemeMode(code) },
                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text(label) },
                modifier = Modifier.testTag("theme_chip_${code.lowercase()}"),
              )
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Default Language
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Default Keyboard Language", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
          Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(
              selected = vaultSettings.activeLanguageCode == "AR",
              onClick = { onSetDefaultLanguage("AR") },
              label = { Text("العربية (AR)") },
              modifier = Modifier.testTag("lang_chip_ar"),
            )
            FilterChip(
              selected = vaultSettings.activeLanguageCode == "EN",
              onClick = { onSetDefaultLanguage("EN") },
              label = { Text("English (EN)") },
              modifier = Modifier.testTag("lang_chip_en"),
            )
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Keyboard Height
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Keyboard Height", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf("COMPACT" to "Compact", "STANDARD" to "Standard", "TALL" to "Tall").forEach { (code, label) ->
              FilterChip(
                selected = vaultSettings.keyboardHeightMode == code,
                onClick = { onSetKeyboardHeightMode(code) },
                label = { Text(label) },
                modifier = Modifier.testTag("height_chip_${code.lowercase()}"),
              )
            }
          }
        }

        // One-Handed Mode
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("One-Handed Reach Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf("OFF" to "Full Width", "LEFT" to "Left-Handed", "RIGHT" to "Right-Handed").forEach { (code, label) ->
              FilterChip(
                selected = vaultSettings.oneHandedMode == code,
                onClick = { onSetOneHandedMode(code) },
                label = { Text(label) },
                modifier = Modifier.testTag("one_handed_chip_${code.lowercase()}"),
              )
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        KeyboardToggleRow(
          icon = Icons.Filled.Pin,
          title = "Dedicated Number Row",
          subtitle = "Show 1–0 quick digits above letter rows.",
          checked = vaultSettings.showNumberRow,
          onCheckedChange = onSetShowNumberRow,
          testTag = "toggle_number_row",
        )
        KeyboardToggleRow(
          icon = Icons.Filled.Search,
          title = "Toolbar Clipboard Search Shortcut",
          subtitle = "Pin one-tap clipboard search on the top keyboard bar.",
          checked = vaultSettings.showSearchTool,
          onCheckedChange = onSetShowSearchTool,
          testTag = "toggle_search_tool",
        )
        KeyboardToggleRow(
          icon = Icons.Filled.EmojiEmotions,
          title = "Toolbar Emoji Picker Shortcut",
          subtitle = "Pin the categorized Emoji panel button on the toolbar.",
          checked = vaultSettings.showEmojiTool,
          onCheckedChange = onSetShowEmojiTool,
          testTag = "toggle_emoji_tool",
        )
        KeyboardToggleRow(
          icon = Icons.Filled.Vibration,
          title = "Key-Press Haptic Feedback",
          subtitle = "Crisp vibration on key tap and swipe actions.",
          checked = vaultSettings.hapticFeedbackEnabled,
          onCheckedChange = onSetHaptic,
          testTag = "toggle_haptic",
        )
      }
    }
  }
}

@Composable
private fun StepStatusRow(
  step: String,
  title: String,
  done: Boolean,
  doneText: String,
  pendingText: String,
  okColor: androidx.compose.ui.graphics.Color,
  warnColor: androidx.compose.ui.graphics.Color,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Box(
        modifier = Modifier
          .size(22.dp)
          .clip(CircleShape)
          .background((if (done) okColor else warnColor).copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = step,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = if (done) okColor else warnColor,
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
    Text(
      text = if (done) doneText else pendingText,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.SemiBold,
      color = if (done) okColor else warnColor,
    )
  }
}

@Composable
private fun KeyboardToggleRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.width(Spacing.md))
    Column(modifier = Modifier.weight(1f).padding(end = Spacing.md)) {
      Text(text = title, style = MaterialTheme.typography.bodyLarge)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}
