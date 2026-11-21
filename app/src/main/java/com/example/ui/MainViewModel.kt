package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SmartKeyboardApp
import com.example.data.BenchmarkReport
import com.example.data.ClipboardClipEntity
import com.example.data.ClipboardFolderEntity
import com.example.data.DefaultFolders
import com.example.data.FolderCountTuple
import com.example.data.SyncStatus
import com.example.security.VaultSettingsSnapshot
import com.example.telegram.DetectedChat
import com.example.telegram.TelegramOpResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImeRegistrationStatus(
  val isImeEnabled: Boolean = false,
  val isImeSelectedDefault: Boolean = false,
  val currentDefaultId: String = "",
)

data class ClipboardFilterState(
  val query: String = "",
  val folderId: String? = null,
  val pinnedOnly: Boolean = false,
)

data class MainUiFeedback(
  val bannerMessage: String? = null,
  val telegramStatusMessage: String? = null,
  val detectedChats: List<DetectedChat> = emptyList(),
  val isTelegramBusy: Boolean = false,
  val isBenchmarkRunning: Boolean = false,
  val latestBenchmark: BenchmarkReport? = null,
  val plaintextLeakCheckPassed: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application as SmartKeyboardApp
  private val repo = app.clipboardRepository
  private val vault = app.secretVault
  private val telegram = app.telegramSyncManager

  private val _filter = MutableStateFlow(ClipboardFilterState())
  val filter: StateFlow<ClipboardFilterState> = _filter.asStateFlow()

  private val _imeStatus = MutableStateFlow(ImeRegistrationStatus())
  val imeStatus: StateFlow<ImeRegistrationStatus> = _imeStatus.asStateFlow()

  private val _feedback = MutableStateFlow(MainUiFeedback())
  val feedback: StateFlow<MainUiFeedback> = _feedback.asStateFlow()

  val vaultSettings: StateFlow<VaultSettingsSnapshot> = vault.settingsFlow

  val folders: StateFlow<List<ClipboardFolderEntity>> = repo.folders
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val totalClipCount: StateFlow<Int> = repo.totalClipCount
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val folderCounts: StateFlow<List<FolderCountTuple>> = repo.folderCounts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val pendingSyncCount: StateFlow<Int> = repo.pendingSyncCount
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val failedSyncCount: StateFlow<Int> = repo.failedSyncCount
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val clips: StateFlow<List<ClipboardClipEntity>> = _filter
    .flatMapLatest { f ->
      repo.observeClips(
        query = f.query,
        folderId = f.folderId,
        pinnedOnly = f.pinnedOnly,
        limit = 300,
      )
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  init {
    viewModelScope.launch {
      repo.ensureDefaultFolders()
      refreshImeRegistrationStatus()
      _feedback.update {
        it.copy(plaintextLeakCheckPassed = !vault.hasPlaintextBotTokenInPrefs())
      }
    }
  }

  fun refreshImeRegistrationStatus() {
    val imm = app.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val enabledList = runCatching { imm?.enabledInputMethodList.orEmpty() }.getOrDefault(emptyList())
    val expectedPackage = app.packageName
    val isEnabled = enabledList.any { it.packageName == expectedPackage }

    val defaultIme = runCatching {
      Settings.Secure.getString(app.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()
    }.getOrDefault("")
    val isSelected = defaultIme.startsWith("$expectedPackage/")

    _imeStatus.value = ImeRegistrationStatus(
      isImeEnabled = isEnabled,
      isImeSelectedDefault = isSelected,
      currentDefaultId = defaultIme,
    )
  }

  fun updateSearchQuery(query: String) {
    _filter.update { it.copy(query = query) }
  }

  fun selectFolder(folderId: String?) {
    _filter.update { it.copy(folderId = folderId) }
  }

  fun togglePinnedFilter() {
    _filter.update { it.copy(pinnedOnly = !it.pinnedOnly) }
  }

  fun saveNewClip(content: String, folderId: String, isPinned: Boolean) {
    viewModelScope.launch {
      val syncEnabled = vault.settingsFlow.value.telegramSyncEnabled
      val res = repo.saveClip(
        rawText = content,
        folderId = folderId,
        isPinned = isPinned,
        queueForTelegramSync = syncEnabled,
      )
      val msg = when {
        res == null -> "Clip text cannot be empty."
        res.wasDuplicate -> "Duplicate clip merged and moved to top."
        res.flaggedSensitive -> "Saved locally as Sensitive (excluded from Telegram sync)."
        else -> "Clip saved to Super Clipboard."
      }
      _feedback.update { it.copy(bannerMessage = msg) }
    }
  }

  fun captureSystemClipboardFromApp() {
    val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clip = cm.primaryClip
    if (clip == null || clip.itemCount == 0) {
      _feedback.update { it.copy(bannerMessage = "System clipboard is empty or restricted by Android.") }
      return
    }
    val text = clip.getItemAt(0).coerceToText(app)?.toString()?.trim().orEmpty()
    if (text.isEmpty()) {
      _feedback.update { it.copy(bannerMessage = "Clipboard has no text item.") }
      return
    }
    val flaggedByOs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      clip.description?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true
    } else {
      false
    }
    viewModelScope.launch {
      val syncEnabled = vault.settingsFlow.value.telegramSyncEnabled
      val res = repo.saveClip(
        rawText = text,
        folderId = DefaultFolders.GENERAL,
        queueForTelegramSync = syncEnabled && !flaggedByOs,
        forceSensitiveFlag = if (flaggedByOs) true else null,
      )
      _feedback.update {
        it.copy(
          bannerMessage = if (res?.wasDuplicate == true) {
            "Clipboard text already saved (timestamp updated)."
          } else {
            "Captured from system clipboard."
          }
        )
      }
    }
  }

  fun copyClipToSystemClipboard(clip: ClipboardClipEntity) {
    val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clipData = ClipData.newPlainText("SmartKey Clip", clip.content)
    if (clip.isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      clipData.description.extras = PersistableBundle().apply {
        putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
      }
    }
    cm.setPrimaryClip(clipData)
    _feedback.update { it.copy(bannerMessage = "Copied to system clipboard.") }
  }

  fun editClip(clipId: Long, newContent: String) {
    viewModelScope.launch {
      val ok = repo.editClip(clipId, newContent)
      _feedback.update {
        it.copy(bannerMessage = if (ok) "Clip updated." else "Update failed or duplicate text exists.")
      }
    }
  }

  fun togglePin(clip: ClipboardClipEntity) {
    viewModelScope.launch { repo.togglePin(clip) }
  }

  fun deleteClips(ids: List<Long>) {
    viewModelScope.launch {
      repo.deleteClips(ids)
      _feedback.update { it.copy(bannerMessage = "Deleted ${ids.size} clip(s).") }
    }
  }

  fun moveClipsToFolder(ids: List<Long>, folderId: String) {
    viewModelScope.launch {
      repo.moveClipsToFolder(ids, folderId)
      _feedback.update { it.copy(bannerMessage = "Moved ${ids.size} clip(s).") }
    }
  }

  fun createFolder(name: String) {
    viewModelScope.launch {
      val created = repo.createFolder(name)
      _feedback.update {
        it.copy(bannerMessage = if (created != null) "Folder '${created.name}' created." else "Invalid folder name.")
      }
    }
  }

  fun deleteFolder(folderId: String) {
    viewModelScope.launch {
      repo.deleteFolder(folderId)
      if (_filter.value.folderId == folderId) {
        _filter.update { it.copy(folderId = null) }
      }
      _feedback.update { it.copy(bannerMessage = "Folder deleted; clips moved to General.") }
    }
  }

  suspend fun buildExportJson(includeSensitive: Boolean = false): String {
    return repo.exportJson(includeSensitive)
  }

  fun importFromJson(rawJson: String) {
    viewModelScope.launch {
      runCatching { repo.importJson(rawJson) }
        .onSuccess { summary ->
          _feedback.update {
            it.copy(
              bannerMessage = "Imported ${summary.importedCount} clips (${summary.skippedDuplicates} duplicates skipped, ${summary.foldersCreated} folders)."
            )
          }
        }
        .onFailure { err ->
          _feedback.update { it.copy(bannerMessage = "Import failed: ${err.message}") }
        }
    }
  }

  fun runBenchmark(count: Int) {
    viewModelScope.launch {
      _feedback.update { it.copy(isBenchmarkRunning = true) }
      val report = repo.runStressBenchmark(count)
      _feedback.update {
        it.copy(
          isBenchmarkRunning = false,
          latestBenchmark = report,
          bannerMessage = "Benchmark ($count items): insert ${report.insertDurationMs}ms, search ${report.searchHitDurationMs}ms",
        )
      }
    }
  }

  fun clearBenchmarkRows() {
    viewModelScope.launch {
      repo.clearBenchmarkData()
      _feedback.update { it.copy(bannerMessage = "Benchmark sample rows cleared.") }
    }
  }

  // Keyboard, Appearance & Security Settings
  fun setDefaultLanguage(code: String) = vault.setActiveLanguageCode(code)
  fun setThemeMode(mode: String) = vault.setThemeMode(mode)
  fun setShowNumberRow(show: Boolean) = vault.setShowNumberRow(show)
  fun setOneHandedMode(mode: String) = vault.setOneHandedMode(mode)
  fun setKeyboardHeightMode(mode: String) = vault.setKeyboardHeightMode(mode)
  fun setShowEmojiTool(show: Boolean) = vault.setShowEmojiTool(show)
  fun setShowSearchTool(show: Boolean) = vault.setShowSearchTool(show)
  fun setAutoCapture(enabled: Boolean) = vault.setAutoCaptureClipboard(enabled)
  fun setProtectPasswordFields(enabled: Boolean) = vault.setProtectPasswordFields(enabled)
  fun setMaskSensitiveClips(enabled: Boolean) = vault.setMaskSensitiveClips(enabled)
  fun setHapticFeedback(enabled: Boolean) = vault.setHapticFeedbackEnabled(enabled)

  // Telegram Sync Operations
  fun saveTelegramCredentials(tokenInput: String, chatIdInput: String) {
    if (tokenInput.isNotBlank()) {
      vault.setBotToken(tokenInput)
    }
    if (chatIdInput.isNotBlank()) {
      vault.setChatId(chatIdInput)
    }
    _feedback.update {
      it.copy(
        telegramStatusMessage = "Credentials encrypted with Android Keystore AES-256-GCM.",
        plaintextLeakCheckPassed = !vault.hasPlaintextBotTokenInPrefs(),
      )
    }
  }

  fun setTelegramSyncEnabled(enabled: Boolean) {
    vault.setTelegramSyncEnabled(enabled)
    _feedback.update {
      it.copy(
        telegramStatusMessage = if (enabled) {
          "Telegram sync enabled (opt-in)."
        } else {
          "Telegram sync disabled. All clips stay local."
        }
      )
    }
  }

  fun clearTelegramCredentials() {
    vault.clearTelegramCredentials()
    _feedback.update {
      it.copy(
        telegramStatusMessage = "Bot Token and Chat ID erased from Keystore vault.",
        detectedChats = emptyList(),
      )
    }
  }

  fun verifyTelegramBot() {
    viewModelScope.launch {
      _feedback.update { it.copy(isTelegramBusy = true) }
      val res = telegram.verifyBot()
      _feedback.update {
        it.copy(isTelegramBusy = false, telegramStatusMessage = describeTelegramResult(res))
      }
    }
  }

  fun detectTelegramChats() {
    viewModelScope.launch {
      _feedback.update { it.copy(isTelegramBusy = true) }
      val res = telegram.detectChatsFromUpdates()
      _feedback.update { current ->
        when (res) {
          is TelegramOpResult.ChatsDetected -> current.copy(
            isTelegramBusy = false,
            detectedChats = res.chats,
            telegramStatusMessage = if (res.chats.isEmpty()) {
              "No recent bot messages found. Send /start to your bot first, then tap Detect."
            } else {
              "Found ${res.chats.size} chat(s). Tap one below to bind."
            },
          )
          else -> current.copy(
            isTelegramBusy = false,
            telegramStatusMessage = describeTelegramResult(res),
          )
        }
      }
    }
  }

  fun selectDetectedChat(chatId: String) {
    vault.setChatId(chatId)
    _feedback.update { it.copy(telegramStatusMessage = "Bound Chat ID $chatId (encrypted).") }
  }

  fun syncPendingToTelegram() {
    viewModelScope.launch {
      _feedback.update { it.copy(isTelegramBusy = true) }
      val res = telegram.syncPendingClips()
      _feedback.update {
        it.copy(isTelegramBusy = false, telegramStatusMessage = describeTelegramResult(res))
      }
    }
  }

  fun restoreFromTelegram() {
    viewModelScope.launch {
      _feedback.update { it.copy(isTelegramBusy = true) }
      val res = telegram.restoreFromUpdates()
      _feedback.update {
        it.copy(isTelegramBusy = false, telegramStatusMessage = describeTelegramResult(res))
      }
    }
  }

  fun clearBanner() {
    _feedback.update { it.copy(bannerMessage = null) }
  }

  private fun describeTelegramResult(res: TelegramOpResult): String {
    return when (res) {
      is TelegramOpResult.BotVerified -> "Verified @${res.username} (${res.firstName}, id=${res.botId})"
      is TelegramOpResult.ChatsDetected -> "Found ${res.chats.size} chat(s)."
      is TelegramOpResult.SyncCompleted ->
        "Synced ${res.uploadedCount} clip(s); skipped ${res.skippedSensitive} sensitive; failed ${res.failedCount}."
      is TelegramOpResult.RestoreCompleted ->
        "Restored ${res.importedCount} clip(s) (${res.skippedDuplicates} duplicates skipped)."
      is TelegramOpResult.RateLimited -> res.message
      is TelegramOpResult.InvalidCredentials -> res.message
      is TelegramOpResult.NetworkFailure -> res.message
      is TelegramOpResult.DisabledOrUnconfigured -> res.message
    }
  }
}
