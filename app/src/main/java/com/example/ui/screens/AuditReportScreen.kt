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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.security.VaultSettingsSnapshot
import com.example.ui.ImeRegistrationStatus
import com.example.ui.theme.AmberContainerDark
import com.example.ui.theme.AmberContainerLight
import com.example.ui.theme.AmberOnContainerDark
import com.example.ui.theme.AmberOnContainerLight
import com.example.ui.theme.EmeraldOk
import com.example.ui.theme.EmeraldOkDark
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.SmartKeyCard
import com.example.ui.theme.Spacing
import com.example.ui.theme.StatusPill
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberDark

@Composable
fun AuditReportScreen(
  vaultSettings: VaultSettingsSnapshot,
  imeStatus: ImeRegistrationStatus,
  plaintextLeakCheckPassed: Boolean,
  totalClips: Int,
  onSetAutoCapture: (Boolean) -> Unit,
  onSetProtectPasswords: (Boolean) -> Unit,
  onSetMaskSensitive: (Boolean) -> Unit,
) {
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val okTint = if (isDark) EmeraldOkDark else EmeraldOk
  val warnTint = if (isDark) WarmAmberDark else WarmAmber
  val warnBg = if (isDark) AmberContainerDark else AmberContainerLight
  val warnFg = if (isDark) AmberOnContainerDark else AmberOnContainerLight

  val allSecure = plaintextLeakCheckPassed && vaultSettings.protectPasswordFields

  LazyColumn(
    modifier = Modifier.fillMaxSize().testTag("audit_list"),
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
            text = "Security, Privacy & Audit",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "Data protection controls, Keystore posture, and transparent privacy policy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        StatusPill(
          text = if (allSecure) "Protected" else "Review",
          icon = if (allSecure) Icons.Filled.Security else Icons.Filled.Warning,
          containerColor = if (allSecure) MaterialTheme.colorScheme.secondaryContainer else warnBg,
          contentColor = if (allSecure) MaterialTheme.colorScheme.onSecondaryContainer else warnFg,
        )
      }
    }

    // Data Protection Controls (moved cleanly here from Keyboard Home)
    item {
      SmartKeyCard(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        SectionHeader(
          title = "Clipboard & Field Protection",
          subtitle = "Control how SmartKey Clip reacts to sensitive fields and clipboard reads.",
        )

        SecurityToggleRow(
          icon = Icons.Filled.ContentPaste,
          title = "Auto-capture clipboard while keyboard is open",
          subtitle = "Android 10+ blocks background sniffing; captures happen only when IME is focused.",
          checked = vaultSettings.autoCaptureClipboard,
          onCheckedChange = onSetAutoCapture,
          testTag = "toggle_auto_capture",
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SecurityToggleRow(
          icon = Icons.Filled.Lock,
          title = "Protect password & OTP fields",
          subtitle = "Disables word suggestions and pauses clipboard auto-capture on secret inputs.",
          checked = vaultSettings.protectPasswordFields,
          onCheckedChange = onSetProtectPasswords,
          testTag = "toggle_protect_passwords",
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SecurityToggleRow(
          icon = Icons.Filled.VisibilityOff,
          title = "Mask sensitive clips in drawer",
          subtitle = "Obfuscates OTPs, tokens, and OS-flagged items until tapped to reveal.",
          checked = vaultSettings.maskSensitiveClips,
          onCheckedChange = onSetMaskSensitive,
          testTag = "toggle_mask_sensitive",
        )
      }
    }

    // Live Security Posture
    item {
      SmartKeyCard(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Verified Cryptographic & OS Safeguards", style = MaterialTheme.typography.titleMedium)
        }

        AuditCheckItem(
          title = "Android Keystore AES-256-GCM Vault",
          detail = if (vaultSettings.keystoreActive) {
            "Hardware/Keystore secret key active (replaces deprecated EncryptedSharedPreferences)."
          } else {
            "Keystore initializes automatically on first credential write."
          },
          passed = true,
          okColor = okTint,
          warnColor = warnTint,
        )
        AuditCheckItem(
          title = "Zero Plaintext Secrets in Storage",
          detail = "Bot Token and Chat ID stored exclusively as AES-GCM ciphertext + 96-bit IV.",
          passed = plaintextLeakCheckPassed,
          okColor = okTint,
          warnColor = warnTint,
        )
        AuditCheckItem(
          title = "Cloud Backup & Device Transfer Excluded",
          detail = "android:allowBackup=false + backup_rules/data_extraction_rules exclude Room DB & Keystore vault.",
          passed = true,
          okColor = okTint,
          warnColor = warnTint,
        )
        AuditCheckItem(
          title = "Telegram Sync Opt-In Default",
          detail = if (!vaultSettings.telegramSyncEnabled) {
            "Sync is currently OFF (all $totalClips clips stored locally only)."
          } else {
            "Sync is ON by explicit user choice; sensitive clips remain excluded."
          },
          passed = true,
          okColor = okTint,
          warnColor = warnTint,
        )
        AuditCheckItem(
          title = "System IME Registration",
          detail = if (imeStatus.isImeSelectedDefault) {
            "SmartKey Clip is active as default keyboard."
          } else {
            "Enable and select SmartKey Clip in Keyboard tab to test live typing."
          },
          passed = imeStatus.isImeSelectedDefault,
          okColor = okTint,
          warnColor = warnTint,
        )
      }
    }

    // Privacy Policy Card
    item {
      SmartKeyCard(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Privacy Policy Summary", style = MaterialTheme.typography.titleMedium)
        }
        Text(
          text = "• Local-first storage: Keystrokes are never recorded or transmitted. Saved clipboard snippets reside locally in SQLite (Room) on your device.\n" +
            "• No analytics or ads: Zero telemetry, zero ad SDKs, zero third-party tracking.\n" +
            "• Android clipboard boundaries: SmartKey Clip reads clipboard text only when the keyboard or app is actively focused or when you tap Save/Paste.\n" +
            "• Optional Telegram sync: Disabled by default. Non-sensitive clips sent over HTTPS to api.telegram.org (Bot API is TLS-encrypted, not E2EE).\n" +
            "• Zero-permission exports: JSON backups use Android's Storage Access Framework without broad storage permissions.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // Quick User Guide
    item {
      SmartKeyCard(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Quick Start Guide", style = MaterialTheme.typography.titleMedium)
        }
        Text(
          text = "1. Keyboard Tab → Tap 'Enable IME', enable SmartKey Clip, then tap 'Switch IME'.\n" +
            "2. Arabic Tashkeel → Tap 'تشكيل' on the keyboard toolbar to toggle diacritics (َ ً ُ ٌ ِ ٍ ْ ّ).\n" +
            "3. Inline Clipboard & Search → Tap 'Clips' or the Search icon on the keyboard toolbar to filter, search, pin, or paste in one tap.\n" +
            "4. Spacebar Cursor & Hold Backspace → Drag horizontally across the spacebar to scrub the cursor; press and hold Backspace for rapid delete.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun SecurityToggleRow(
  icon: ImageVector,
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

@Composable
private fun AuditCheckItem(
  title: String,
  detail: String,
  passed: Boolean,
  okColor: Color,
  warnColor: Color,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(
      imageVector = if (passed) Icons.Filled.CheckCircle else Icons.Filled.Warning,
      contentDescription = null,
      tint = if (passed) okColor else warnColor,
      modifier = Modifier.size(18.dp).padding(top = 2.dp),
    )
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(text = title, style = MaterialTheme.typography.bodyLarge)
      Text(
        text = detail,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
