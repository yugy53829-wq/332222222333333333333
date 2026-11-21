package com.example.ime

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.SmartKeyboardApp
import com.example.data.ClipboardClipEntity
import com.example.data.DefaultFolders
import com.example.security.SensitiveContentDetector
import com.example.ui.theme.SmartKeyClipTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SmartKeyboardImeService :
  InputMethodService(),
  LifecycleOwner,
  ViewModelStoreOwner,
  SavedStateRegistryOwner {

  private val lifecycleRegistry = LifecycleRegistry(this)
  private val store = ViewModelStore()
  private val savedStateController = SavedStateRegistryController.create(this)

  override val lifecycle: Lifecycle
    get() = lifecycleRegistry

  override val viewModelStore: ViewModelStore
    get() = store

  override val savedStateRegistry: SavedStateRegistry
    get() = savedStateController.savedStateRegistry

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val stateController = KeyboardStateController()
  private val uiState = MutableStateFlow(ImeUiState())
  private var lastCapturedClipboardText: String = ""
  private var pinnedPhrasesCache: List<String> = emptyList()

  override fun onCreate() {
    super.onCreate()
    savedStateController.performRestore(null)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    attachWindowTreeOwners()
    observeRepositoryAndVault()
  }

  override fun onCreateInputView(): View {
    attachWindowTreeOwners()
    val composeView = ComposeView(this).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setViewTreeLifecycleOwner(this@SmartKeyboardImeService)
      setViewTreeViewModelStoreOwner(this@SmartKeyboardImeService)
      setViewTreeSavedStateRegistryOwner(this@SmartKeyboardImeService)
      setContent {
        val state by uiState.collectAsState()
        SmartKeyClipTheme(themeMode = state.themeMode, dynamicColor = false) {
          ImeKeyboardRoot(
            uiState = state,
            onKeyAction = ::handleKeyAction,
            onToggleTashkeel = {
              stateController.toggleTashkeelBar()
              syncUiState()
            },
            onSelectPanel = { mode, openSearch ->
              stateController.setPanel(mode, openClipboardSearch = openSearch)
              if (stateController.showClipboardPanel) {
                refreshClipboardClips()
              }
              syncUiState()
            },
            onToggleClipboardSearchMode = { active ->
              stateController.setClipboardSearchMode(active)
              refreshClipboardClips()
              syncUiState()
            },
            onClearClipboardSearch = {
              stateController.clearClipboardSearchQuery()
              refreshClipboardClips()
              syncUiState()
            },
            onTogglePinnedClipsOnly = {
              uiState.update { it.copy(pinnedClipsOnly = !it.pinnedClipsOnly) }
              refreshClipboardClips()
            },
            onCaptureClipboardNow = {
              captureSystemClipboard(explicitTrigger = true)
            },
            onPasteClip = ::pasteClipIntoEditor,
            onTogglePinClip = ::toggleClipPin,
            onSelectFolder = { folderId ->
              uiState.update { it.copy(selectedFolderId = folderId) }
              refreshClipboardClips()
            },
            onApplySuggestion = ::applySuggestion,
            onOpenSettingsApp = ::openCompanionApp,
            onCycleOneHandedMode = ::cycleOneHandedMode,
            onToggleNumberRow = ::toggleNumberRow,
            onCycleHeightMode = ::cycleHeightMode,
            onCycleThemeMode = ::cycleThemeMode,
          )
        }
      }
    }
    return composeView
  }

  override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
    super.onStartInputView(info, restarting)
    attachWindowTreeOwners()
    if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    val vault = app().secretVault
    val settings = vault.settingsFlow.value
    val defaultLang = if (settings.activeLanguageCode == "EN") {
      KeyboardLanguage.ENGLISH
    } else {
      KeyboardLanguage.ARABIC
    }
    stateController.onStartInput(info, defaultLang)

    if (settings.autoCaptureClipboard &&
      !(settings.protectPasswordFields && stateController.securityProfile.shouldPauseClipboardAutoCapture)
    ) {
      captureSystemClipboard(explicitTrigger = false)
    }

    refreshClipboardClips()
    syncUiState()
  }

  override fun onWindowShown() {
    super.onWindowShown()
    attachWindowTreeOwners()
    if (lifecycleRegistry.currentState == Lifecycle.State.STARTED) {
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
  }

  override fun onFinishInputView(finishingInput: Boolean) {
    super.onFinishInputView(finishingInput)
    stateController.toggleClipboardPanel(forceValue = false)
    stateController.clearComposingWord()
    if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }
    syncUiState()
  }

  override fun onWindowHidden() {
    super.onWindowHidden()
    if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }
  }

  override fun onDestroy() {
    serviceScope.cancel()
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    store.clear()
    super.onDestroy()
  }

  private fun app(): SmartKeyboardApp = applicationContext as SmartKeyboardApp

  private fun attachWindowTreeOwners() {
    window?.window?.decorView?.let { decor ->
      decor.setViewTreeLifecycleOwner(this)
      decor.setViewTreeViewModelStoreOwner(this)
      decor.setViewTreeSavedStateRegistryOwner(this)
    }
  }

  private fun observeRepositoryAndVault() {
    val container = app()
    serviceScope.launch {
      container.clipboardRepository.folders.collect { folders ->
        uiState.update { it.copy(folders = folders) }
      }
    }
    serviceScope.launch {
      container.clipboardRepository.totalClipCount.collect { count ->
        uiState.update { it.copy(totalClipCount = count) }
      }
    }
    serviceScope.launch {
      container.clipboardRepository.observeClips(
        query = "",
        folderId = null,
        pinnedOnly = true,
        limit = 20,
      ).collect { pinned ->
        pinnedPhrasesCache = pinned.filterNot { it.isSensitive }.map { it.previewText }
        syncUiState()
      }
    }
    serviceScope.launch {
      container.secretVault.settingsFlow.collect { settings ->
        uiState.update {
          it.copy(
            maskSensitiveClips = settings.maskSensitiveClips,
            themeMode = settings.themeMode,
            showNumberRow = settings.showNumberRow,
            oneHandedMode = settings.oneHandedMode,
            keyboardHeightMode = settings.keyboardHeightMode,
            showEmojiTool = settings.showEmojiTool,
            showSearchTool = settings.showSearchTool,
          )
        }
      }
    }
  }

  private fun refreshClipboardClips() {
    val folderFilter = uiState.value.selectedFolderId
    val pinnedOnly = uiState.value.pinnedClipsOnly
    val query = stateController.clipboardSearchQuery
    serviceScope.launch {
      val list = app().clipboardRepository.getQuickClipsForIme(
        query = query,
        folderId = folderFilter,
        limit = 50,
      )
      val filtered = if (pinnedOnly) list.filter { it.isPinned } else list
      uiState.update { it.copy(clips = filtered) }
    }
  }

  private fun captureSystemClipboard(explicitTrigger: Boolean) {
    val settings = app().secretVault.settingsFlow.value
    if (settings.protectPasswordFields && stateController.securityProfile.shouldPauseClipboardAutoCapture) {
      if (explicitTrigger) {
        showTransientBanner("Paused in password/OTP field")
      }
      return
    }

    val cm = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clip = cm.primaryClip ?: return
    if (clip.itemCount == 0) return

    val text = clip.getItemAt(0).coerceToText(this)?.toString()?.trim().orEmpty()
    if (text.isEmpty()) return
    if (!explicitTrigger && text == lastCapturedClipboardText) return

    val flaggedByOs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      clip.description?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true
    } else {
      false
    }
    val sensitive = flaggedByOs || SensitiveContentDetector.isSensitiveClipboardText(text)
    lastCapturedClipboardText = text

    serviceScope.launch {
      val res = app().clipboardRepository.saveClip(
        rawText = text,
        folderId = DefaultFolders.GENERAL,
        queueForTelegramSync = settings.telegramSyncEnabled && !sensitive,
        forceSensitiveFlag = if (flaggedByOs) true else null,
      )
      refreshClipboardClips()
      if (explicitTrigger && res != null) {
        showTransientBanner(if (res.wasDuplicate) "Clip refreshed" else "Saved to Super Clipboard")
      }
    }
  }

  private fun handleKeyAction(action: KeyAction) {
    performHapticIfEnabled()

    // When in-IME clipboard search is active, route character & backspace presses to the clip filter
    if (stateController.showClipboardPanel && stateController.isClipboardSearchActive) {
      when (action) {
        is KeyAction.CommitText -> {
          stateController.appendClipboardSearch(action.text)
          refreshClipboardClips()
          syncUiState()
          return
        }
        KeyAction.Backspace -> {
          stateController.backspaceClipboardSearch()
          refreshClipboardClips()
          syncUiState()
          return
        }
        KeyAction.SwitchLanguage -> {
          stateController.toggleLanguage()
          syncUiState()
          return
        }
        else -> Unit
      }
    }

    val ic = currentInputConnection
    when (action) {
      is KeyAction.CommitText -> {
        ic?.commitText(action.text, 1)
        stateController.onCharacterCommitted(action.text)
      }
      KeyAction.Backspace -> {
        ic?.deleteSurroundingText(1, 0)
        stateController.onBackspace()
      }
      KeyAction.Space -> {
        ic?.commitText(" ", 1)
        stateController.clearComposingWord()
      }
      KeyAction.Enter -> {
        stateController.clearComposingWord()
        val handled = sendDefaultEditorAction(true)
        if (!handled) {
          ic?.commitText("\n", 1)
        }
      }
      KeyAction.Shift -> {
        stateController.toggleShift()
      }
      KeyAction.SwitchLanguage -> {
        stateController.toggleLanguage()
        val code = if (stateController.language == KeyboardLanguage.ARABIC) "AR" else "EN"
        app().secretVault.setActiveLanguageCode(code)
      }
      KeyAction.SwitchToNextIme -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          switchToNextInputMethod(false)
        } else {
          @Suppress("DEPRECATION")
          switchToNextInputMethod(false)
        }
      }
      is KeyAction.MoveCursor -> {
        val keyCode = if (action.delta < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        sendDownUpKeyEvents(keyCode)
      }
      is KeyAction.SwitchLayout -> {
        stateController.setLayout(action.target)
      }
    }
    syncUiState()
  }

  private fun applySuggestion(suggestion: String) {
    performHapticIfEnabled()
    val ic = currentInputConnection ?: return
    val currentWordLen = stateController.composingWord.length
    if (currentWordLen > 0) {
      ic.deleteSurroundingText(currentWordLen, 0)
    }
    ic.commitText("$suggestion ", 1)
    stateController.clearComposingWord()
    syncUiState()
  }

  private fun pasteClipIntoEditor(clip: ClipboardClipEntity) {
    performHapticIfEnabled()
    currentInputConnection?.commitText(clip.content, 1)
    stateController.clearComposingWord()
    showTransientBanner("Pasted into field")
  }

  private fun toggleClipPin(clip: ClipboardClipEntity) {
    serviceScope.launch {
      app().clipboardRepository.togglePin(clip)
      refreshClipboardClips()
    }
  }

  private fun cycleOneHandedMode() {
    val vault = app().secretVault
    val next = when (vault.settingsFlow.value.oneHandedMode) {
      "OFF" -> "RIGHT"
      "RIGHT" -> "LEFT"
      else -> "OFF"
    }
    vault.setOneHandedMode(next)
  }

  private fun toggleNumberRow() {
    val vault = app().secretVault
    vault.setShowNumberRow(!vault.settingsFlow.value.showNumberRow)
  }

  private fun cycleHeightMode() {
    val vault = app().secretVault
    val next = when (vault.settingsFlow.value.keyboardHeightMode) {
      "COMPACT" -> "STANDARD"
      "STANDARD" -> "TALL"
      else -> "COMPACT"
    }
    vault.setKeyboardHeightMode(next)
  }

  private fun cycleThemeMode() {
    val vault = app().secretVault
    val next = when (vault.settingsFlow.value.themeMode) {
      "SYSTEM" -> "LIGHT"
      "LIGHT" -> "DARK"
      else -> "SYSTEM"
    }
    vault.setThemeMode(next)
  }

  private fun openCompanionApp() {
    val intent = Intent(this, MainActivity::class.java).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
  }

  private fun showTransientBanner(msg: String) {
    uiState.update { it.copy(statusBanner = msg) }
    serviceScope.launch {
      kotlinx.coroutines.delay(1800)
      uiState.update { if (it.statusBanner == msg) it.copy(statusBanner = null) else it }
    }
  }

  private fun performHapticIfEnabled() {
    if (!app().secretVault.settingsFlow.value.hapticFeedbackEnabled) return
    val view = window?.window?.decorView
    if (view != null) {
      view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    } else {
      val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
      } else {
        @Suppress("DEPRECATION")
        getSystemService(VIBRATOR_SERVICE) as? Vibrator
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
      }
    }
  }

  private fun syncUiState() {
    val canSwitch = runCatching { shouldOfferSwitchingToNextInputMethod() }.getOrDefault(false)
    val suggestions = SuggestionEngine.suggest(
      prefix = stateController.composingWord,
      language = stateController.language,
      securityProfile = stateController.securityProfile,
      pinnedPhrases = pinnedPhrasesCache,
    )
    uiState.update {
      it.copy(
        language = stateController.language,
        layoutMode = stateController.layoutMode,
        shiftState = stateController.shiftState,
        panelMode = stateController.panelMode,
        showTashkeelBar = stateController.showTashkeelBar,
        isClipboardSearchActive = stateController.isClipboardSearchActive,
        clipboardSearchQuery = stateController.clipboardSearchQuery,
        enterLabel = stateController.enterActionLabel,
        securityProfile = stateController.securityProfile,
        suggestions = suggestions,
        canSwitchNextIme = canSwitch,
      )
    }
  }
}
