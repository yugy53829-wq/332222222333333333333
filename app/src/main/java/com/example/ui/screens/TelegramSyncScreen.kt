package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.security.VaultSettingsSnapshot
import com.example.telegram.DetectedChat
import com.example.ui.theme.AmberContainerDark
import com.example.ui.theme.AmberContainerLight
import com.example.ui.theme.AmberOnContainerDark
import com.example.ui.theme.AmberOnContainerLight
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.EmeraldOkDark
import com.example.ui.theme.Radius
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.SmartKeyCard
import com.example.ui.theme.Spacing
import com.example.ui.theme.StatusPill
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberDark

@Composable
fun TelegramSyncScreen(
  vaultSettings: VaultSettingsSnapshot,
  pendingSyncCount: Int,
  failedSyncCount: Int,
  isBusy: Boolean,
  statusMessage: String?,
  detectedChats: List<DetectedChat>,
  onSaveCredentials: (String, String) -> Unit,
  onSetSyncEnabled: (Boolean) -> Unit,
  onVerifyBot: () -> Unit,
  onDetectChats: () -> Unit,
  onSelectChat: (String) -> Unit,
  onSyncPending: () -> Unit,
  onRestoreUpdates: () -> Unit,
  onClearCredentials: () -> Unit,
) {
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val okTint = if (isDark) EmeraldOkDark else EmeraldOk
  val warnTint = if (isDark) WarmAmberDark else WarmAmber
  val warnBg = if (isDark) AmberContainerDark else AmberContainerLight
  val warnFg = if (isDark) AmberOnContainerDark else AmberOnContainerLight

  var tokenInput by remember { mutableStateOf("") }
  var chatIdInput by remember(vaultSettings.chatId) { mutableStateOf(vaultSettings.chatId) }

  val isReady = vaultSettings.telegramSyncEnabled && vaultSettings.hasBotToken && vaultSettings.chatId.isNotEmpty()

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(Spacing.lg),
    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    item {
      Row(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Optional Telegram Sync",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Off by default. Non-sensitive clips only.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        StatusPill(
          text = when {
            isReady -> "Sync Active"
            vaultSettings.hasBotToken -> "Configured"
            else -> "Local Only"
          },
          icon = if (isReady) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
          containerColor = if (isReady) {
            MaterialTheme.colorScheme.secondaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceVariant
          },
          contentColor = if (isReady) {
            MaterialTheme.colorScheme.onSecondaryContainer
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        )
      }
    }

    // Opt-in Toggle & Live Queue Card
    item {
      SmartKeyCard(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = Spacing.md)) {
            Text("Enable Telegram Cloud Sync", style = MaterialTheme.typography.titleMedium)
            Text(
              text = if (vaultSettings.telegramSyncEnabled) {
                "Opt-in active • Pending: $pendingSyncCount • Failed: $failedSyncCount"
              } else {
                "Disabled by default • All clips remain strictly on-device"
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = vaultSettings.telegramSyncEnabled,
            onCheckedChange = onSetSyncEnabled,
            modifier = Modifier.testTag("telegram_sync_enabled_switch"),
          )
        }

        if (isBusy) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (!statusMessage.isNullOrBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(Radius.chip),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = statusMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(Spacing.md),
            )
          }
        }
      }
    }

    // Keystore Credentials Card
    item {
      SmartKeyCard(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Shield, contentDescription = null, tint = okTint)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Bot Credentials (Keystore AES-256-GCM)", style = MaterialTheme.typography.titleMedium)
        }

        if (vaultSettings.hasBotToken) {
          Text(
            text = "Stored Token: ${vaultSettings.maskedBotToken}" +
              if (vaultSettings.botUsername.isNotEmpty()) " (@${vaultSettings.botUsername})" else "",
            style = MaterialTheme.typography.labelSmall,
            color = okTint,
          )
        }

        OutlinedTextField(
          value = tokenInput,
          onValueChange = { tokenInput = it },
          label = { Text("Bot Token (from @BotFather)") },
          placeholder = { Text(if (vaultSettings.hasBotToken) "Leave blank to keep existing" else "123456:ABC-DEF...") },
          leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          shape = RoundedCornerShape(Radius.chip),
          modifier = Modifier.fillMaxWidth().testTag("telegram_token_input"),
        )

        OutlinedTextField(
          value = chatIdInput,
          onValueChange = { chatIdInput = it },
          label = { Text("Target Chat ID") },
          placeholder = { Text("e.g. 123456789") },
          singleLine = true,
          shape = RoundedCornerShape(Radius.chip),
          modifier = Modifier.fillMaxWidth().testTag("telegram_chat_id_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
          Button(
            onClick = {
              onSaveCredentials(tokenInput, chatIdInput)
              tokenInput = ""
            },
            enabled = tokenInput.isNotBlank() || chatIdInput.isNotBlank(),
            shape = RoundedCornerShape(Radius.chip),
            modifier = Modifier.weight(1f).testTag("save_telegram_credentials_button"),
          ) {
            Text("Encrypt & Save")
          }

          OutlinedButton(
            onClick = onClearCredentials,
            shape = RoundedCornerShape(Radius.chip),
            modifier = Modifier.testTag("clear_telegram_credentials_button"),
          ) {
            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Erase")
          }
        }
      }
    }

    // Verification, Chat Discovery & Sync Actions
    item {
      SmartKeyCard(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        SectionHeader(title = "Bot Verification & Sync Actions")

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
          OutlinedButton(
            onClick = onVerifyBot,
            enabled = !isBusy && vaultSettings.hasBotToken,
            shape = RoundedCornerShape(Radius.chip),
            modifier = Modifier.weight(1f).testTag("verify_bot_button"),
          ) {
            Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Verify (getMe)")
          }

          OutlinedButton(
            onClick = onDetectChats,
            enabled = !isBusy && vaultSettings.hasBotToken,
            shape = RoundedCornerShape(Radius.chip),
            modifier = Modifier.weight(1f).testTag("detect_chats_button"),
          ) {
            Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Detect Chat")
          }
        }

        if (detectedChats.isNotEmpty()) {
          Text("Detected Chats (tap to bind):", style = MaterialTheme.typography.labelMedium)
          detectedChats.forEach { chat ->
            OutlinedButton(
              onClick = {
                chatIdInput = chat.chatId
                onSelectChat(chat.chatId)
              },
              shape = RoundedCornerShape(Radius.chip),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("${chat.displayName} (${chat.type} • ${chat.chatId})")
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
          Button(
            onClick = onSyncPending,
            enabled = isReady && !isBusy,
            shape = RoundedCornerShape(Radius.chip),
            modifier = Modifier.weight(1f).testTag("sync_pending_button"),
          ) {
            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sync Now ($pendingSyncCount)")
          }

          OutlinedButton(
            onClick = onRestoreUpdates,
            enabled = isReady && !isBusy,
            shape = RoundedCornerShape(Radius.chip),
            modifier = Modifier.weight(1f).testTag("restore_telegram_button"),
          ) {
            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Restore")
          }
        }
      }
    }

    // Security & E2EE Transparency Notice (Light & Dark aware)
    item {
      Surface(
        color = warnBg,
        shape = RoundedCornerShape(Radius.card),
        modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
      ) {
        Row(modifier = Modifier.padding(Spacing.lg), verticalAlignment = Alignment.Top) {
          Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = warnTint,
            modifier = Modifier.padding(top = 2.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = "Telegram Bot API Security Disclosure",
              style = MaterialTheme.typography.titleMedium,
              color = warnFg,
            )
            Text(
              text = "Telegram Bot API uses TLS transport encryption, NOT end-to-end Secret Chat encryption. Sensitive clips (OTPs, tokens, passwords) are automatically blocked from upload. Never share your Bot Token publicly.",
              style = MaterialTheme.typography.bodyMedium,
              color = warnFg,
            )
          }
        }
      }
    }
  }
}
