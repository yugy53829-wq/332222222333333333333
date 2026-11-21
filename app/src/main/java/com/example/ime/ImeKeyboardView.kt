package com.example.ime

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ClipboardClipEntity
import com.example.data.ClipboardFolderEntity
import com.example.security.EditorSecurityProfile
import com.example.security.SensitiveContentDetector
import com.example.ui.theme.ArabicBodyFont
import com.example.ui.theme.DarkKeyboardPalette
import com.example.ui.theme.DisplaySansFont
import com.example.ui.theme.KeyboardPalette
import com.example.ui.theme.LightKeyboardPalette
import com.example.ui.theme.Radius
import com.example.ui.theme.resolveIsDarkTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ImeUiState(
  val language: KeyboardLanguage = KeyboardLanguage.ARABIC,
  val layoutMode: KeyboardLayoutMode = KeyboardLayoutMode.LETTERS,
  val shiftState: ShiftState = ShiftState.OFF,
  val panelMode: ImePanelMode = ImePanelMode.KEYBOARD,
  val showTashkeelBar: Boolean = false,
  val isClipboardSearchActive: Boolean = false,
  val clipboardSearchQuery: String = "",
  val enterLabel: String = "↵",
  val securityProfile: EditorSecurityProfile = SensitiveContentDetector.inspectEditorInfo(null),
  val suggestions: List<String> = emptyList(),
  val clips: List<ClipboardClipEntity> = emptyList(),
  val folders: List<ClipboardFolderEntity> = emptyList(),
  val selectedFolderId: String? = null,
  val pinnedClipsOnly: Boolean = false,
  val totalClipCount: Int = 0,
  val maskSensitiveClips: Boolean = true,
  val canSwitchNextIme: Boolean = false,
  val themeMode: String = "SYSTEM",
  val showNumberRow: Boolean = true,
  val oneHandedMode: String = "OFF",
  val keyboardHeightMode: String = "STANDARD",
  val showEmojiTool: Boolean = true,
  val showSearchTool: Boolean = true,
  val statusBanner: String? = null,
)

@Composable
fun ImeKeyboardRoot(
  uiState: ImeUiState,
  onKeyAction: (KeyAction) -> Unit,
  onToggleTashkeel: () -> Unit,
  onSelectPanel: (ImePanelMode, Boolean) -> Unit,
  onToggleClipboardSearchMode: (Boolean) -> Unit,
  onClearClipboardSearch: () -> Unit,
  onTogglePinnedClipsOnly: () -> Unit,
  onCaptureClipboardNow: () -> Unit,
  onPasteClip: (ClipboardClipEntity) -> Unit,
  onTogglePinClip: (ClipboardClipEntity) -> Unit,
  onSelectFolder: (String?) -> Unit,
  onApplySuggestion: (String) -> Unit,
  onOpenSettingsApp: () -> Unit,
  onCycleOneHandedMode: () -> Unit,
  onToggleNumberRow: () -> Unit,
  onCycleHeightMode: () -> Unit,
  onCycleThemeMode: () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val isDark = resolveIsDarkTheme(uiState.themeMode, systemDark)
  val palette = if (isDark) DarkKeyboardPalette else LightKeyboardPalette
  var forceShowToolbar by remember { mutableStateOf(false) }

  val keyHeight: Dp = when (uiState.keyboardHeightMode) {
    "COMPACT" -> 42.dp
    "TALL" -> 52.dp
    else -> 47.dp
  }

  val showSuggestionsInsteadOfToolbar =
    uiState.panelMode == ImePanelMode.KEYBOARD &&
      !forceShowToolbar &&
      !uiState.securityProfile.isSensitiveField &&
      uiState.suggestions.isNotEmpty() &&
      uiState.statusBanner == null &&
      uiState.suggestions.size > 0 &&
      // Show suggestions strip when user starts typing a word; otherwise show rich quick-tools bar
      !uiState.suggestions.firstOrNull().isNullOrEmpty() &&
      uiState.composingActiveHint()

  // Keep physical key grid LTR so QWERTY & Arabic positions stay predictable; use RTL inside Arabic panels
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(palette.canvasBg)
        .padding(bottom = 6.dp),
    ) {
      // Top Gboard-style Dual Toolbar / Suggestion Strip
      ImeTopStrip(
        uiState = uiState,
        palette = palette,
        showSuggestions = showSuggestionsInsteadOfToolbar,
        onShowToolbar = { forceShowToolbar = true },
        onShowSuggestions = { forceShowToolbar = false },
        onSelectPanel = { mode, search ->
          forceShowToolbar = false
          onSelectPanel(mode, search)
        },
        onToggleTashkeel = onToggleTashkeel,
        onApplySuggestion = onApplySuggestion,
        onSwitchNextIme = { onKeyAction(KeyAction.SwitchToNextIme) },
        onOpenSettingsApp = onOpenSettingsApp,
      )

      HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

      // One-handed wrapper when active
      val contentFraction = if (uiState.oneHandedMode == "OFF") 1f else 0.86f
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (uiState.oneHandedMode == "RIGHT") {
          OneHandedSideRail(
            isLeftSide = true,
            palette = palette,
            onFlipOrExit = onCycleOneHandedMode,
            modifier = Modifier.weight(1f - contentFraction),
          )
        }

        Box(modifier = Modifier.weight(contentFraction)) {
          when (uiState.panelMode) {
            ImePanelMode.KEYBOARD -> {
              ImeMainKeyboardGrid(
                uiState = uiState,
                palette = palette,
                keyHeight = keyHeight,
                onKeyAction = { action ->
                  forceShowToolbar = false
                  onKeyAction(action)
                },
              )
            }
            ImePanelMode.CLIPBOARD -> {
              ImeSuperClipboardPanel(
                uiState = uiState,
                palette = palette,
                onBackToKeyboard = { onSelectPanel(ImePanelMode.KEYBOARD, false) },
                onToggleSearchMode = onToggleClipboardSearchMode,
                onClearSearch = onClearClipboardSearch,
                onTogglePinnedOnly = onTogglePinnedClipsOnly,
                onCaptureNow = onCaptureClipboardNow,
                onPasteClip = onPasteClip,
                onTogglePin = onTogglePinClip,
                onSelectFolder = onSelectFolder,
                onKeyAction = onKeyAction,
              )
            }
            ImePanelMode.EMOJI -> {
              ImeEmojiPanel(
                palette = palette,
                onBackToKeyboard = { onSelectPanel(ImePanelMode.KEYBOARD, false) },
                onEmojiSelected = { emoji -> onKeyAction(KeyAction.CommitText(emoji)) },
                onBackspace = { onKeyAction(KeyAction.Backspace) },
              )
            }
            ImePanelMode.TOOLS -> {
              ImeQuickToolsPanel(
                uiState = uiState,
                palette = palette,
                onBackToKeyboard = { onSelectPanel(ImePanelMode.KEYBOARD, false) },
                onCycleOneHandedMode = onCycleOneHandedMode,
                onToggleNumberRow = onToggleNumberRow,
                onCycleHeightMode = onCycleHeightMode,
                onCycleThemeMode = onCycleThemeMode,
                onOpenSettingsApp = onOpenSettingsApp,
              )
            }
          }
        }

        if (uiState.oneHandedMode == "LEFT") {
          OneHandedSideRail(
            isLeftSide = false,
            palette = palette,
            onFlipOrExit = onCycleOneHandedMode,
            modifier = Modifier.weight(1f - contentFraction),
          )
        }
      }
    }
  }
}

private fun ImeUiState.composingActiveHint(): Boolean {
  // Default starter phrases contain spaces ("السلام عليكم", "Hello"); active word completions are single tokens
  return suggestions.isNotEmpty() && suggestions.all { !it.contains(' ') }
}

@Composable
private fun ImeTopStrip(
  uiState: ImeUiState,
  palette: KeyboardPalette,
  showSuggestions: Boolean,
  onShowToolbar: () -> Unit,
  onShowSuggestions: () -> Unit,
  onSelectPanel: (ImePanelMode, Boolean) -> Unit,
  onToggleTashkeel: () -> Unit,
  onApplySuggestion: (String) -> Unit,
  onSwitchNextIme: () -> Unit,
  onOpenSettingsApp: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(46.dp)
      .background(palette.toolbarBg)
      .padding(horizontal = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (uiState.securityProfile.isSensitiveField) {
      Surface(
        color = palette.protectedBg,
        shape = RoundedCornerShape(Radius.pill),
        modifier = Modifier.padding(end = 6.dp),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "Protected input field",
            tint = palette.protectedFg,
            modifier = Modifier.size(14.dp),
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "Protected field",
            color = palette.protectedFg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = DisplaySansFont,
          )
        }
      }
      Spacer(modifier = Modifier.weight(1f))
      ToolbarIconPill(
        icon = Icons.Filled.ContentPaste,
        contentDescription = "Open Clipboard",
        selected = uiState.panelMode == ImePanelMode.CLIPBOARD,
        palette = palette,
        testTag = "ime_clipboard_toggle",
        onClick = { onSelectPanel(ImePanelMode.CLIPBOARD, false) },
      )
      return
    }

    if (uiState.statusBanner != null) {
      Text(
        text = uiState.statusBanner,
        color = palette.primaryAccentBg,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = DisplaySansFont,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
      )
      return
    }

    if (showSuggestions) {
      // Back-to-tools chevron + 3-4 clean Gboard-style suggestion slots
      IconButton(
        onClick = onShowToolbar,
        modifier = Modifier.size(38.dp).testTag("ime_expand_toolbar"),
      ) {
        Icon(
          imageVector = Icons.Filled.ChevronRight,
          contentDescription = "Show Keyboard Tools",
          tint = palette.keySubText,
          modifier = Modifier.size(20.dp),
        )
      }

      val sugDir = if (uiState.language == KeyboardLanguage.ARABIC) {
        LayoutDirection.Rtl
      } else {
        LayoutDirection.Ltr
      }
      CompositionLocalProvider(LocalLayoutDirection provides sugDir) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          uiState.suggestions.take(3).forEachIndexed { idx, word ->
            if (idx > 0) {
              VerticalDivider(
                color = palette.divider,
                modifier = Modifier.height(20.dp),
              )
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  onApplySuggestion(word)
                  onShowSuggestions()
                },
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = word,
                color = if (idx == 0) palette.primaryAccentBg else palette.keyText,
                fontSize = 14.sp,
                fontWeight = if (idx == 0) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily = if (uiState.language == KeyboardLanguage.ARABIC) ArabicBodyFont else DisplaySansFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp),
              )
            }
          }
        }
      }

      ToolbarIconPill(
        icon = Icons.Filled.ContentPaste,
        contentDescription = "Open Super Clipboard",
        selected = false,
        palette = palette,
        testTag = "ime_clipboard_toggle",
        onClick = { onSelectPanel(ImePanelMode.CLIPBOARD, false) },
      )
    } else {
      // Professional Customizable Keyboard Toolbar
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        // Primary Clipboard Pill (Signature Feature)
        Surface(
          onClick = { onSelectPanel(ImePanelMode.CLIPBOARD, false) },
          color = if (uiState.panelMode == ImePanelMode.CLIPBOARD) palette.toolActiveBg else palette.keyBg,
          shape = RoundedCornerShape(Radius.pill),
          border = BorderStroke(
            width = 1.dp,
            color = if (uiState.panelMode == ImePanelMode.CLIPBOARD) palette.primaryAccentBg else palette.divider,
          ),
          modifier = Modifier.height(34.dp).testTag("ime_clipboard_toggle"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = if (uiState.panelMode == ImePanelMode.CLIPBOARD) Icons.Filled.Keyboard else Icons.Filled.ContentPaste,
              contentDescription = "Super Clipboard",
              tint = if (uiState.panelMode == ImePanelMode.CLIPBOARD) palette.onToolActive else palette.primaryAccentBg,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = if (uiState.panelMode == ImePanelMode.CLIPBOARD) "Keys" else "Clips",
              color = if (uiState.panelMode == ImePanelMode.CLIPBOARD) palette.onToolActive else palette.keyText,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              fontFamily = DisplaySansFont,
            )
          }
        }

        if (uiState.showSearchTool) {
          ToolbarIconPill(
            icon = Icons.Filled.Search,
            contentDescription = "Search Clipboard",
            selected = uiState.panelMode == ImePanelMode.CLIPBOARD && uiState.isClipboardSearchActive,
            palette = palette,
            testTag = "ime_search_tool",
            onClick = { onSelectPanel(ImePanelMode.CLIPBOARD, true) },
          )
        }

        if (uiState.showEmojiTool) {
          ToolbarIconPill(
            icon = Icons.Filled.SentimentSatisfied,
            contentDescription = "Emoji Picker",
            selected = uiState.panelMode == ImePanelMode.EMOJI,
            palette = palette,
            testTag = "ime_emoji_tool",
            onClick = { onSelectPanel(ImePanelMode.EMOJI, false) },
          )
        }

        if (uiState.language == KeyboardLanguage.ARABIC && uiState.panelMode == ImePanelMode.KEYBOARD) {
          Surface(
            onClick = onToggleTashkeel,
            color = if (uiState.showTashkeelBar) palette.toolActiveBg else Color.Transparent,
            shape = RoundedCornerShape(Radius.pill),
            modifier = Modifier.height(34.dp).testTag("ime_tashkeel_toggle"),
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 9.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "تشكيل",
                color = if (uiState.showTashkeelBar) palette.onToolActive else palette.keyText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = ArabicBodyFont,
              )
            }
          }
        }

        ToolbarIconPill(
          icon = Icons.Filled.Tune,
          contentDescription = "More Keyboard Tools",
          selected = uiState.panelMode == ImePanelMode.TOOLS,
          palette = palette,
          testTag = "ime_more_tools",
          onClick = { onSelectPanel(ImePanelMode.TOOLS, false) },
        )
      }

      if (uiState.canSwitchNextIme) {
        ToolbarIconPill(
          icon = Icons.Filled.Language,
          contentDescription = "Switch System Keyboard",
          selected = false,
          palette = palette,
          testTag = "ime_next_keyboard",
          onClick = onSwitchNextIme,
        )
      }

      ToolbarIconPill(
        icon = Icons.Filled.Settings,
        contentDescription = "Keyboard Settings",
        selected = false,
        palette = palette,
        testTag = "ime_open_settings",
        onClick = onOpenSettingsApp,
      )
    }
  }
}

@Composable
private fun ToolbarIconPill(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  selected: Boolean,
  palette: KeyboardPalette,
  testTag: String,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    color = if (selected) palette.toolActiveBg else Color.Transparent,
    shape = CircleShape,
    modifier = Modifier.size(36.dp).testTag(testTag),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (selected) palette.onToolActive else palette.keySubText,
        modifier = Modifier.size(19.dp),
      )
    }
  }
}

@Composable
private fun ImeMainKeyboardGrid(
  uiState: ImeUiState,
  palette: KeyboardPalette,
  keyHeight: Dp,
  onKeyAction: (KeyAction) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // Arabic Tashkeel & Hamza Strip
    if (uiState.language == KeyboardLanguage.ARABIC && uiState.showTashkeelBar) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        KeyboardLayouts.arabicTashkeelAndForms.forEach { diacritic ->
          Surface(
            onClick = { onKeyAction(KeyAction.CommitText(diacritic)) },
            color = palette.keyBg,
            shape = RoundedCornerShape(Radius.key),
            shadowElevation = 1.dp,
            modifier = Modifier.height(36.dp),
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 13.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = diacritic,
                color = palette.primaryAccentBg,
                fontSize = 19.sp,
                fontFamily = ArabicBodyFont,
                fontWeight = FontWeight.Medium,
              )
            }
          }
        }
      }
    }

    // Optional Number Row
    if (uiState.showNumberRow && uiState.layoutMode == KeyboardLayoutMode.LETTERS) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        KeyboardLayouts.numberQuickRow.forEach { digit ->
          KeycapButton(
            keySpec = KeySpec(digit),
            uiState = uiState,
            palette = palette,
            height = (keyHeight - 8.dp).coerceAtLeast(32.dp),
            modifier = Modifier.weight(1f),
            onAction = onKeyAction,
          )
        }
      }
    }

    // 4-Row Keyboard Body
    val rows = remember(uiState.language, uiState.layoutMode, uiState.shiftState) {
      KeyboardLayouts.rowsFor(uiState.language, uiState.layoutMode, uiState.shiftState)
    }

    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      rows.forEach { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          row.forEach { key ->
            KeycapButton(
              keySpec = key,
              uiState = uiState,
              palette = palette,
              height = keyHeight,
              modifier = Modifier.weight(key.weight),
              onAction = onKeyAction,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun KeycapButton(
  keySpec: KeySpec,
  uiState: ImeUiState,
  palette: KeyboardPalette,
  height: Dp,
  modifier: Modifier = Modifier,
  onAction: (KeyAction) -> Unit,
) {
  val scope = rememberCoroutineScope()
  var isPressed by remember { mutableStateOf(false) }
  var spaceDragAccumulator by remember { mutableFloatStateOf(0f) }

  val isEnter = keySpec.action is KeyAction.Enter
  val isBackspace = keySpec.action is KeyAction.Backspace
  val isShift = keySpec.action is KeyAction.Shift
  val isSpace = keySpec.action is KeyAction.Space
  val isSpecial = keySpec.action !is KeyAction.CommitText && !isSpace

  val bgColor = when {
    isPressed -> palette.keyPressedBg
    isEnter -> palette.primaryAccentBg
    isShift && uiState.shiftState != ShiftState.OFF -> palette.toolActiveBg
    isSpecial -> palette.actionKeyBg
    else -> palette.keyBg
  }

  val fgColor = when {
    isEnter -> palette.onPrimaryAccent
    isShift && uiState.shiftState != ShiftState.OFF -> palette.onToolActive
    isSpace -> palette.keySubText
    else -> palette.keyText
  }

  // Subtle Gboard-style bottom lip shadow
  Box(
    modifier = modifier
      .height(height)
      .clip(RoundedCornerShape(Radius.key))
      .background(palette.keyLipShadow)
      .padding(bottom = 1.5.dp)
      .clip(RoundedCornerShape(Radius.key))
      .background(bgColor)
      .then(
        when {
          isBackspace -> {
            Modifier.pointerInput(Unit) {
              detectTapGestures(
                onPress = {
                  isPressed = true
                  onAction(KeyAction.Backspace)
                  val repeatJob: Job = scope.launch {
                    delay(340)
                    while (true) {
                      onAction(KeyAction.Backspace)
                      delay(55)
                    }
                  }
                  tryAwaitRelease()
                  repeatJob.cancel()
                  isPressed = false
                },
              )
            }
          }
          isSpace -> {
            Modifier
              .pointerInput(Unit) {
                detectTapGestures(
                  onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                  },
                  onTap = { onAction(KeyAction.Space) },
                )
              }
              .pointerInput(Unit) {
                detectHorizontalDragGestures(
                  onDragStart = { spaceDragAccumulator = 0f },
                  onHorizontalDrag = { _, dragAmount ->
                    spaceDragAccumulator += dragAmount
                    val stepPx = 24f
                    if (spaceDragAccumulator >= stepPx) {
                      spaceDragAccumulator -= stepPx
                      onAction(KeyAction.MoveCursor(1))
                    } else if (spaceDragAccumulator <= -stepPx) {
                      spaceDragAccumulator += stepPx
                      onAction(KeyAction.MoveCursor(-1))
                    }
                  },
                )
              }
          }
          else -> {
            Modifier.pointerInput(keySpec) {
              detectTapGestures(
                onPress = {
                  isPressed = true
                  tryAwaitRelease()
                  isPressed = false
                },
                onTap = { onAction(keySpec.action) },
              )
            }
          }
        }
      ),
    contentAlignment = Alignment.Center,
  ) {
    when (keySpec.action) {
      KeyAction.Backspace -> {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Backspace,
          contentDescription = "Backspace",
          tint = fgColor,
          modifier = Modifier.size(19.dp),
        )
      }
      KeyAction.Shift -> {
        Icon(
          imageVector = if (uiState.shiftState == ShiftState.CAPS_LOCK) {
            Icons.Filled.KeyboardCapslock
          } else {
            Icons.Filled.ArrowUpward
          },
          contentDescription = "Shift",
          tint = fgColor,
          modifier = Modifier.size(20.dp),
        )
      }
      KeyAction.Enter -> {
        val enterIcon = when (uiState.enterLabel) {
          "Search" -> Icons.Filled.Search
          "Send" -> Icons.AutoMirrored.Filled.Send
          "Done" -> Icons.Filled.Done
          "Next", "Go" -> Icons.AutoMirrored.Filled.ArrowForward
          else -> Icons.AutoMirrored.Filled.KeyboardReturn
        }
        Icon(
          imageVector = enterIcon,
          contentDescription = uiState.enterLabel,
          tint = fgColor,
          modifier = Modifier.size(20.dp),
        )
      }
      else -> {
        Text(
          text = keySpec.primary,
          color = fgColor,
          fontSize = when {
            isSpace -> 12.sp
            keySpec.primary.length > 2 -> 13.sp
            uiState.language == KeyboardLanguage.ARABIC -> 19.sp
            else -> 18.sp
          },
          fontFamily = if (uiState.language == KeyboardLanguage.ARABIC) ArabicBodyFont else DisplaySansFont,
          fontWeight = if (isSpecial || isSpace) FontWeight.Medium else FontWeight.Normal,
          maxLines = 1,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun ImeSuperClipboardPanel(
  uiState: ImeUiState,
  palette: KeyboardPalette,
  onBackToKeyboard: () -> Unit,
  onToggleSearchMode: (Boolean) -> Unit,
  onClearSearch: () -> Unit,
  onTogglePinnedOnly: () -> Unit,
  onCaptureNow: () -> Unit,
  onPasteClip: (ClipboardClipEntity) -> Unit,
  onTogglePin: (ClipboardClipEntity) -> Unit,
  onSelectFolder: (String?) -> Unit,
  onKeyAction: (KeyAction) -> Unit,
) {
  var revealedIds by remember { mutableStateOf(setOf<Long>()) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(if (uiState.isClipboardSearchActive) 330.dp else 258.dp)
      .background(palette.canvasBg)
      .padding(horizontal = 8.dp, vertical = 6.dp),
  ) {
    // Clipboard Header Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      IconButton(
        onClick = onBackToKeyboard,
        modifier = Modifier.size(36.dp).testTag("ime_clipboard_back"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to Keyboard",
          tint = palette.keyText,
          modifier = Modifier.size(20.dp),
        )
      }

      if (uiState.isClipboardSearchActive) {
        Surface(
          color = palette.cardBg,
          shape = RoundedCornerShape(Radius.pill),
          border = BorderStroke(1.dp, palette.primaryAccentBg),
          modifier = Modifier.weight(1f).height(36.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Filled.Search,
              contentDescription = null,
              tint = palette.primaryAccentBg,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = uiState.clipboardSearchQuery.ifEmpty { "Type below to search clips..." },
              color = if (uiState.clipboardSearchQuery.isEmpty()) palette.keySubText else palette.keyText,
              fontSize = 13.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            IconButton(
              onClick = {
                if (uiState.clipboardSearchQuery.isNotEmpty()) onClearSearch()
                else onToggleSearchMode(false)
              },
              modifier = Modifier.size(28.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Clear or exit search",
                tint = palette.keySubText,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      } else {
        Text(
          text = "Super Clipboard (${uiState.totalClipCount})",
          color = palette.keyText,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          fontFamily = DisplaySansFont,
          modifier = Modifier.weight(1f),
        )
        IconButton(
          onClick = { onToggleSearchMode(true) },
          modifier = Modifier.size(36.dp).testTag("ime_clipboard_search_btn"),
        ) {
          Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search saved clips",
            tint = palette.keyText,
            modifier = Modifier.size(19.dp),
          )
        }
      }

      Surface(
        onClick = onCaptureNow,
        color = palette.toolActiveBg,
        shape = RoundedCornerShape(Radius.pill),
        modifier = Modifier.height(32.dp).testTag("ime_save_clip_btn"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Icon(
            imageVector = Icons.Filled.AddCircleOutline,
            contentDescription = null,
            tint = palette.onToolActive,
            modifier = Modifier.size(15.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Save",
            color = palette.onToolActive,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }

    // Folder & Pinned Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      FilterChip(
        selected = uiState.selectedFolderId == null && !uiState.pinnedClipsOnly,
        onClick = {
          onSelectFolder(null)
          if (uiState.pinnedClipsOnly) onTogglePinnedOnly()
        },
        label = { Text("All", fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = palette.toolActiveBg,
          selectedLabelColor = palette.onToolActive,
          labelColor = palette.keySubText,
        ),
      )
      FilterChip(
        selected = uiState.pinnedClipsOnly,
        onClick = onTogglePinnedOnly,
        leadingIcon = {
          Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.size(14.dp))
        },
        label = { Text("Pinned", fontSize = 11.sp) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = palette.toolActiveBg,
          selectedLabelColor = palette.onToolActive,
          labelColor = palette.keySubText,
        ),
      )
      uiState.folders.forEach { folder ->
        FilterChip(
          selected = uiState.selectedFolderId == folder.id,
          onClick = {
            onSelectFolder(if (uiState.selectedFolderId == folder.id) null else folder.id)
          },
          label = { Text(folder.name, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = palette.toolActiveBg,
            selectedLabelColor = palette.onToolActive,
            labelColor = palette.keySubText,
          ),
        )
      }
    }

    // Clip Results List
    if (uiState.clips.isEmpty()) {
      Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        Text(
          text = if (uiState.clipboardSearchQuery.isNotEmpty()) {
            "No clips match \"${uiState.clipboardSearchQuery}\""
          } else {
            "No clips here yet. Tap Save to capture clipboard."
          },
          color = palette.keySubText,
          fontSize = 13.sp,
          fontFamily = DisplaySansFont,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 2.dp),
      ) {
        items(uiState.clips, key = { it.id }) { clip ->
          val isMasked = clip.isSensitive && uiState.maskSensitiveClips && !revealedIds.contains(clip.id)
          val display = if (isMasked) {
            SensitiveContentDetector.maskForDisplay(clip.content)
          } else {
            clip.previewText
          }
          val clipLayoutDir = if (clip.languageTag == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

          Surface(
            color = palette.cardBg,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.8.dp, palette.divider),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onPasteClip(clip) },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (clip.isSensitive) {
                Icon(
                  imageVector = Icons.Filled.Lock,
                  contentDescription = "Sensitive clip",
                  tint = palette.protectedFg,
                  modifier = Modifier.size(16.dp).padding(end = 4.dp),
                )
              }
              CompositionLocalProvider(LocalLayoutDirection provides clipLayoutDir) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = display,
                    color = palette.keyText,
                    fontSize = 13.sp,
                    fontFamily = if (clip.languageTag == "ar") ArabicBodyFont else DisplaySansFont,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "${clip.folderId} • ${clip.charCount} chars",
                    color = palette.keySubText,
                    fontSize = 10.sp,
                  )
                }
              }

              if (clip.isSensitive) {
                IconButton(
                  onClick = {
                    revealedIds = if (revealedIds.contains(clip.id)) {
                      revealedIds - clip.id
                    } else {
                      revealedIds + clip.id
                    }
                  },
                  modifier = Modifier.size(34.dp),
                ) {
                  Icon(
                    imageVector = if (isMasked) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = "Reveal sensitive clip",
                    tint = palette.protectedFg,
                    modifier = Modifier.size(17.dp),
                  )
                }
              }

              IconButton(
                onClick = { onTogglePin(clip) },
                modifier = Modifier.size(34.dp),
              ) {
                Icon(
                  imageVector = if (clip.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                  contentDescription = "Pin clip",
                  tint = if (clip.isPinned) palette.primaryAccentBg else palette.keySubText,
                  modifier = Modifier.size(17.dp),
                )
              }
            }
          }
        }
      }
    }

    // Compact Mini-Keyboard when In-IME Clipboard Search is Active
    if (uiState.isClipboardSearchActive) {
      HorizontalDivider(color = palette.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
      val miniRows = remember(uiState.language) {
        KeyboardLayouts.rowsFor(uiState.language, KeyboardLayoutMode.LETTERS, ShiftState.OFF).take(3)
      }
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        miniRows.forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            row.forEach { key ->
              if (key.action !is KeyAction.Shift) {
                KeycapButton(
                  keySpec = key,
                  uiState = uiState,
                  palette = palette,
                  height = 34.dp,
                  modifier = Modifier.weight(key.weight),
                  onAction = onKeyAction,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ImeEmojiPanel(
  palette: KeyboardPalette,
  onBackToKeyboard: () -> Unit,
  onEmojiSelected: (String) -> Unit,
  onBackspace: () -> Unit,
) {
  var selectedCategoryIdx by remember { mutableIntStateOf(0) }
  val categoryIcons = listOf(
    Icons.Filled.EmojiEmotions,
    Icons.Filled.ThumbUp,
    Icons.Filled.Favorite,
    Icons.Filled.Workspaces,
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(250.dp)
      .background(palette.canvasBg)
      .padding(horizontal = 8.dp, vertical = 6.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(
        onClick = onBackToKeyboard,
        modifier = Modifier.size(36.dp).testTag("ime_emoji_back"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to Keyboard",
          tint = palette.keyText,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EmojiCatalog.categories.forEachIndexed { idx, (label, _) ->
          Surface(
            onClick = { selectedCategoryIdx = idx },
            color = if (selectedCategoryIdx == idx) palette.toolActiveBg else Color.Transparent,
            shape = CircleShape,
            modifier = Modifier.size(36.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = categoryIcons[idx],
                contentDescription = label,
                tint = if (selectedCategoryIdx == idx) palette.onToolActive else palette.keySubText,
                modifier = Modifier.size(19.dp),
              )
            }
          }
        }
      }

      IconButton(
        onClick = onBackspace,
        modifier = Modifier.size(36.dp),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Backspace,
          contentDescription = "Backspace",
          tint = palette.keyText,
          modifier = Modifier.size(19.dp),
        )
      }
    }

    val emojis = EmojiCatalog.categories[selectedCategoryIdx].second
    LazyVerticalGrid(
      columns = GridCells.Fixed(8),
      modifier = Modifier.fillMaxWidth().weight(1f),
      contentPadding = PaddingValues(vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      items(emojis) { emoji ->
        Box(
          modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onEmojiSelected(emoji) },
          contentAlignment = Alignment.Center,
        ) {
          Text(text = emoji, fontSize = 22.sp)
        }
      }
    }
  }
}

@Composable
private fun ImeQuickToolsPanel(
  uiState: ImeUiState,
  palette: KeyboardPalette,
  onBackToKeyboard: () -> Unit,
  onCycleOneHandedMode: () -> Unit,
  onToggleNumberRow: () -> Unit,
  onCycleHeightMode: () -> Unit,
  onCycleThemeMode: () -> Unit,
  onOpenSettingsApp: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .height(238.dp)
      .background(palette.canvasBg)
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBackToKeyboard, modifier = Modifier.size(36.dp)) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to Keyboard",
          tint = palette.keyText,
        )
      }
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Keyboard Quick Tools",
        color = palette.keyText,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = DisplaySansFont,
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      QuickToolTile(
        icon = Icons.Filled.PhoneAndroid,
        label = "One-Handed",
        value = uiState.oneHandedMode,
        palette = palette,
        modifier = Modifier.weight(1f),
        onClick = onCycleOneHandedMode,
      )
      QuickToolTile(
        icon = Icons.Filled.Numbers,
        label = "Number Row",
        value = if (uiState.showNumberRow) "ON" else "OFF",
        palette = palette,
        modifier = Modifier.weight(1f),
        onClick = onToggleNumberRow,
      )
      QuickToolTile(
        icon = Icons.Filled.Height,
        label = "Key Height",
        value = uiState.keyboardHeightMode,
        palette = palette,
        modifier = Modifier.weight(1f),
        onClick = onCycleHeightMode,
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      QuickToolTile(
        icon = Icons.Filled.Palette,
        label = "Theme",
        value = uiState.themeMode,
        palette = palette,
        modifier = Modifier.weight(1f),
        onClick = onCycleThemeMode,
      )
      QuickToolTile(
        icon = Icons.Filled.Settings,
        label = "App Studio",
        value = "OPEN",
        palette = palette,
        modifier = Modifier.weight(1f),
        onClick = onOpenSettingsApp,
      )
    }
  }
}

@Composable
private fun QuickToolTile(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
  palette: KeyboardPalette,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    color = palette.cardBg,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(0.8.dp, palette.divider),
    modifier = modifier.height(74.dp),
  ) {
    Column(
      modifier = Modifier.padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = palette.primaryAccentBg,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label,
        color = palette.keyText,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
      )
      Text(
        text = value,
        color = palette.keySubText,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun OneHandedSideRail(
  isLeftSide: Boolean,
  palette: KeyboardPalette,
  onFlipOrExit: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      onClick = onFlipOrExit,
      color = palette.actionKeyBg,
      shape = CircleShape,
      modifier = Modifier.size(38.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = if (isLeftSide) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
          contentDescription = "Switch or exit one-handed mode",
          tint = palette.keyText,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}
