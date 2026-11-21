package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.AuditReportScreen
import com.example.ui.screens.ClipboardScreen
import com.example.ui.screens.KeyboardSetupScreen
import com.example.ui.screens.TelegramSyncScreen
import com.example.ui.theme.SmartKeyClipTheme

enum class AppTab(
  val label: String,
  val activeIcon: ImageVector,
  val inactiveIcon: ImageVector,
  val testTag: String,
) {
  KEYBOARD("Keyboard", Icons.Filled.Keyboard, Icons.Outlined.Keyboard, "tab_keyboard"),
  CLIPBOARD("Clipboard", Icons.Filled.ContentPaste, Icons.Outlined.ContentPaste, "tab_clipboard"),
  TELEGRAM("Telegram", Icons.Filled.CloudSync, Icons.Outlined.CloudSync, "tab_telegram"),
  AUDIT("Security", Icons.Filled.Security, Icons.Outlined.Security, "tab_audit"),
}

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val vaultSettings by viewModel.vaultSettings.collectAsStateWithLifecycle()
      SmartKeyClipTheme(themeMode = vaultSettings.themeMode, dynamicColor = false) {
        SmartKeyClipRoot(viewModel = viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshImeRegistrationStatus()
  }
}

@Composable
fun SmartKeyClipRoot(viewModel: MainViewModel) {
  var currentTab by rememberSaveable { mutableStateOf(AppTab.KEYBOARD) }
  val snackbarHostState = remember { SnackbarHostState() }

  val imeStatus by viewModel.imeStatus.collectAsStateWithLifecycle()
  val vaultSettings by viewModel.vaultSettings.collectAsStateWithLifecycle()
  val folders by viewModel.folders.collectAsStateWithLifecycle()
  val folderCounts by viewModel.folderCounts.collectAsStateWithLifecycle()
  val totalClips by viewModel.totalClipCount.collectAsStateWithLifecycle()
  val clips by viewModel.clips.collectAsStateWithLifecycle()
  val filter by viewModel.filter.collectAsStateWithLifecycle()
  val feedback by viewModel.feedback.collectAsStateWithLifecycle()
  val pendingSync by viewModel.pendingSyncCount.collectAsStateWithLifecycle()
  val failedSync by viewModel.failedSyncCount.collectAsStateWithLifecycle()

  LaunchedEffect(feedback.bannerMessage) {
    val msg = feedback.bannerMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearBanner()
    }
  }

  BackHandler(enabled = currentTab != AppTab.KEYBOARD) {
    currentTab = AppTab.KEYBOARD
  }

  val screenWidthDp = LocalConfiguration.current.screenWidthDp
  val useRail = screenWidthDp >= 720

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    bottomBar = {
      if (!useRail) {
        Column {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
          ) {
            AppTab.entries.forEach { tab ->
              val selected = currentTab == tab
              NavigationBarItem(
                selected = selected,
                onClick = { currentTab = tab },
                icon = {
                  Icon(
                    imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                    contentDescription = tab.label,
                  )
                },
                label = {
                  Text(
                    text = tab.label,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.testTag(tab.testTag),
              )
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      if (useRail) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface,
        ) {
          AppTab.entries.forEach { tab ->
            val selected = currentTab == tab
            NavigationRailItem(
              selected = selected,
              onClick = { currentTab = tab },
              icon = {
                Icon(
                  imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                  contentDescription = tab.label,
                )
              },
              label = {
                Text(
                  text = tab.label,
                  fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
              },
              colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
              ),
              modifier = Modifier.testTag(tab.testTag),
            )
          }
        }
        VerticalDivider(
          modifier = Modifier.fillMaxHeight(),
          color = MaterialTheme.colorScheme.outlineVariant,
          thickness = 1.dp,
        )
      }

      Box(modifier = Modifier.weight(1f).fillMaxSize()) {
        when (currentTab) {
          AppTab.KEYBOARD -> KeyboardSetupScreen(
            imeStatus = imeStatus,
            vaultSettings = vaultSettings,
            totalClips = totalClips,
            onRefreshImeStatus = viewModel::refreshImeRegistrationStatus,
            onSetDefaultLanguage = viewModel::setDefaultLanguage,
            onSetThemeMode = viewModel::setThemeMode,
            onSetKeyboardHeightMode = viewModel::setKeyboardHeightMode,
            onSetOneHandedMode = viewModel::setOneHandedMode,
            onSetShowNumberRow = viewModel::setShowNumberRow,
            onSetShowEmojiTool = viewModel::setShowEmojiTool,
            onSetShowSearchTool = viewModel::setShowSearchTool,
            onSetHaptic = viewModel::setHapticFeedback,
          )
          AppTab.CLIPBOARD -> ClipboardScreen(
            clips = clips,
            folders = folders,
            folderCounts = folderCounts,
            totalClipCount = totalClips,
            filter = filter,
            maskSensitive = vaultSettings.maskSensitiveClips,
            isBenchmarkRunning = feedback.isBenchmarkRunning,
            latestBenchmark = feedback.latestBenchmark,
            onQueryChange = viewModel::updateSearchQuery,
            onSelectFolder = viewModel::selectFolder,
            onTogglePinnedOnly = viewModel::togglePinnedFilter,
            onSaveNewClip = viewModel::saveNewClip,
            onCaptureSystemClipboard = viewModel::captureSystemClipboardFromApp,
            onCopyClip = viewModel::copyClipToSystemClipboard,
            onEditClip = viewModel::editClip,
            onTogglePin = viewModel::togglePin,
            onDeleteClips = viewModel::deleteClips,
            onMoveClips = viewModel::moveClipsToFolder,
            onCreateFolder = viewModel::createFolder,
            onBuildExportJson = { viewModel.buildExportJson(includeSensitive = false) },
            onImportJson = viewModel::importFromJson,
            onRunBenchmark = viewModel::runBenchmark,
            onClearBenchmarkRows = viewModel::clearBenchmarkRows,
          )
          AppTab.TELEGRAM -> TelegramSyncScreen(
            vaultSettings = vaultSettings,
            pendingSyncCount = pendingSync,
            failedSyncCount = failedSync,
            isBusy = feedback.isTelegramBusy,
            statusMessage = feedback.telegramStatusMessage,
            detectedChats = feedback.detectedChats,
            onSaveCredentials = viewModel::saveTelegramCredentials,
            onSetSyncEnabled = viewModel::setTelegramSyncEnabled,
            onVerifyBot = viewModel::verifyTelegramBot,
            onDetectChats = viewModel::detectTelegramChats,
            onSelectChat = viewModel::selectDetectedChat,
            onSyncPending = viewModel::syncPendingToTelegram,
            onRestoreUpdates = viewModel::restoreFromTelegram,
            onClearCredentials = viewModel::clearTelegramCredentials,
          )
          AppTab.AUDIT -> AuditReportScreen(
            vaultSettings = vaultSettings,
            imeStatus = imeStatus,
            plaintextLeakCheckPassed = feedback.plaintextLeakCheckPassed,
            totalClips = totalClips,
            onSetAutoCapture = viewModel::setAutoCapture,
            onSetProtectPasswords = viewModel::setProtectPasswordFields,
            onSetMaskSensitive = viewModel::setMaskSensitiveClips,
          )
        }
      }
    }
  }
}
