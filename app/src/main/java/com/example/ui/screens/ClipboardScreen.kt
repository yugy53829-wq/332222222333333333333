package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.BenchmarkReport
import com.example.data.ClipboardClipEntity
import com.example.data.ClipboardFolderEntity
import com.example.data.DefaultFolders
import com.example.data.FolderCountTuple
import com.example.security.SensitiveContentDetector
import com.example.ui.ClipboardFilterState
import com.example.ui.theme.ArabicBodyFont
import com.example.ui.theme.Radius
import com.example.ui.theme.SmartKeyCard
import com.example.ui.theme.Spacing
import com.example.ui.theme.StatusPill
import com.example.ui.theme.WarmAmber
import com.example.ui.theme.WarmAmberDark
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardScreen(
  clips: List<ClipboardClipEntity>,
  folders: List<ClipboardFolderEntity>,
  folderCounts: List<FolderCountTuple>,
  totalClipCount: Int,
  filter: ClipboardFilterState,
  maskSensitive: Boolean,
  isBenchmarkRunning: Boolean,
  latestBenchmark: BenchmarkReport?,
  onQueryChange: (String) -> Unit,
  onSelectFolder: (String?) -> Unit,
  onTogglePinnedOnly: () -> Unit,
  onSaveNewClip: (String, String, Boolean) -> Unit,
  onCaptureSystemClipboard: () -> Unit,
  onCopyClip: (ClipboardClipEntity) -> Unit,
  onEditClip: (Long, String) -> Unit,
  onTogglePin: (ClipboardClipEntity) -> Unit,
  onDeleteClips: (List<Long>) -> Unit,
  onMoveClips: (List<Long>, String) -> Unit,
  onCreateFolder: (String) -> Unit,
  onBuildExportJson: suspend () -> String,
  onImportJson: (String) -> Unit,
  onRunBenchmark: (Int) -> Unit,
  onClearBenchmarkRows: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val warnTint = if (isDark) WarmAmberDark else WarmAmber

  var selectedIds by remember { mutableStateOf(setOf<Long>()) }
  var revealedIds by remember { mutableStateOf(setOf<Long>()) }
  var showAddDialog by remember { mutableStateOf(false) }
  var editingClip by remember { mutableStateOf<ClipboardClipEntity?>(null) }
  var showNewFolderDialog by remember { mutableStateOf(false) }
  var showMoveDialog by remember { mutableStateOf(false) }
  var showOverflowMenu by remember { mutableStateOf(false) }
  var showBenchmarkDialog by remember { mutableStateOf(false) }

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json"),
  ) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        val json = onBuildExportJson()
        withContext(Dispatchers.IO) {
          context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.toByteArray(StandardCharsets.UTF_8))
          }
        }
      }
    }
  }

  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        val content = withContext(Dispatchers.IO) {
          context.contentResolver.openInputStream(uri)?.bufferedReader(StandardCharsets.UTF_8)?.use {
            it.readText()
          }
        }
        if (!content.isNullOrBlank()) {
          onImportJson(content)
        }
      }
    }
  }

  val countMap = remember(folderCounts) { folderCounts.associate { it.folderId to it.clipCount } }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(Radius.card),
        modifier = Modifier.testTag("add_clip_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Add new clip")
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = 92.dp),
      verticalArrangement = Arrangement.spacedBy(Spacing.md),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Header + Search + Overflow Actions
      item {
        Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Super Clipboard",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
              )
              Text(
                text = "$totalClipCount clip(s) indexed locally",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              AssistChip(
                onClick = onCaptureSystemClipboard,
                leadingIcon = {
                  Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = { Text("Paste") },
                modifier = Modifier.testTag("capture_system_clipboard_button"),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Box {
                IconButton(
                  onClick = { showOverflowMenu = true },
                  modifier = Modifier.testTag("clipboard_overflow_button"),
                ) {
                  Icon(Icons.Filled.MoreVert, contentDescription = "Clipboard actions")
                }
                DropdownMenu(
                  expanded = showOverflowMenu,
                  onDismissRequest = { showOverflowMenu = false },
                ) {
                  DropdownMenuItem(
                    text = { Text("New Folder") },
                    leadingIcon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null) },
                    onClick = {
                      showOverflowMenu = false
                      showNewFolderDialog = true
                    },
                    modifier = Modifier.testTag("new_folder_button"),
                  )
                  DropdownMenuItem(
                    text = { Text("Export JSON Backup") },
                    leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                    onClick = {
                      showOverflowMenu = false
                      exportLauncher.launch("smartkey_clipboard_backup.json")
                    },
                    modifier = Modifier.testTag("export_json_button"),
                  )
                  DropdownMenuItem(
                    text = { Text("Import JSON Backup") },
                    leadingIcon = { Icon(Icons.Filled.Upload, contentDescription = null) },
                    onClick = {
                      showOverflowMenu = false
                      importLauncher.launch(arrayOf("application/json", "text/plain"))
                    },
                    modifier = Modifier.testTag("import_json_button"),
                  )
                  DropdownMenuItem(
                    text = { Text("Room Scale Benchmark") },
                    leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null) },
                    onClick = {
                      showOverflowMenu = false
                      showBenchmarkDialog = true
                    },
                    modifier = Modifier.testTag("open_benchmark_button"),
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(Spacing.md))

          OutlinedTextField(
            value = filter.query,
            onValueChange = onQueryChange,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
              if (filter.query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                  Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                }
              }
            },
            placeholder = { Text("Search Arabic or English clips...") },
            singleLine = true,
            shape = RoundedCornerShape(Radius.card),
            modifier = Modifier.fillMaxWidth().testTag("clipboard_search_input"),
          )

          Spacer(modifier = Modifier.height(Spacing.sm))

          // Folder & Pinned filter strip
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            FilterChip(
              selected = filter.folderId == null && !filter.pinnedOnly,
              onClick = {
                onSelectFolder(null)
                if (filter.pinnedOnly) onTogglePinnedOnly()
              },
              label = { Text("All ($totalClipCount)") },
              modifier = Modifier.testTag("folder_chip_all"),
            )
            FilterChip(
              selected = filter.pinnedOnly,
              onClick = onTogglePinnedOnly,
              leadingIcon = {
                Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              label = { Text("Pinned") },
              modifier = Modifier.testTag("folder_chip_pinned"),
            )
            folders.forEach { folder ->
              val cnt = countMap[folder.id] ?: 0
              FilterChip(
                selected = filter.folderId == folder.id,
                onClick = {
                  onSelectFolder(if (filter.folderId == folder.id) null else folder.id)
                },
                label = { Text("${folder.name} ($cnt)") },
              )
            }
          }
        }
      }

      // Multi-select Bulk Toolbar
      if (selectedIds.isNotEmpty()) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(Radius.card),
            modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "${selectedIds.size} selected",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Row {
                TextButton(
                  onClick = { showMoveDialog = true },
                  modifier = Modifier.testTag("bulk_move_button"),
                ) {
                  Icon(Icons.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Move")
                }
                TextButton(
                  onClick = {
                    onDeleteClips(selectedIds.toList())
                    selectedIds = emptySet()
                  },
                  modifier = Modifier.testTag("bulk_delete_button"),
                ) {
                  Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Delete")
                }
                TextButton(onClick = { selectedIds = emptySet() }) {
                  Text("Cancel")
                }
              }
            }
          }
        }
      }

      // Empty state
      if (clips.isEmpty()) {
        item {
          SmartKeyCard(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
            Column(
              modifier = Modifier.padding(vertical = Spacing.lg).fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.ContentPaste,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(34.dp),
              )
              Text("No matching clips", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "Tap + to save a snippet, Paste from system clipboard, or use the keyboard drawer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      // Clip Items
      items(clips, key = { it.id }) { clip ->
        val isSelected = selectedIds.contains(clip.id)
        val isMasked = clip.isSensitive && maskSensitive && !revealedIds.contains(clip.id)
        val displayText = if (isMasked) {
          "Sensitive clip masked: ${SensitiveContentDetector.maskForDisplay(clip.content)}"
        } else {
          clip.previewText
        }
        val containsArabic = remember(displayText) {
          !isMasked && displayText.any { it in '\u0600'..'\u06FF' }
        }

        Surface(
          shape = RoundedCornerShape(Radius.card),
          color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
          } else {
            MaterialTheme.colorScheme.surface
          },
          border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
          ),
          modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxWidth()
            .combinedClickable(
              onClick = {
                if (selectedIds.isNotEmpty()) {
                  selectedIds = if (isSelected) selectedIds - clip.id else selectedIds + clip.id
                } else {
                  onCopyClip(clip)
                }
              },
              onLongClick = {
                selectedIds = if (isSelected) selectedIds - clip.id else selectedIds + clip.id
              },
            ),
        ) {
          Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (selectedIds.isNotEmpty()) {
                Checkbox(
                  checked = isSelected,
                  onCheckedChange = {
                    selectedIds = if (isSelected) selectedIds - clip.id else selectedIds + clip.id
                  },
                )
              }
              if (clip.isSensitive) {
                Icon(
                  imageVector = Icons.Filled.Lock,
                  contentDescription = "Sensitive",
                  tint = warnTint,
                  modifier = Modifier.size(16.dp).padding(end = 6.dp),
                )
              }
              CompositionLocalProvider(
                LocalLayoutDirection provides if (containsArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
              ) {
                Text(
                  text = displayText,
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = if (containsArabic) ArabicBodyFont else MaterialTheme.typography.bodyLarge.fontFamily,
                  ),
                  maxLines = 3,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
              }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                StatusPill(
                  text = clip.folderId,
                  icon = Icons.Filled.DriveFileMove,
                  containerColor = MaterialTheme.colorScheme.surfaceVariant,
                  contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = "${clip.charCount} chars",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                if (clip.isSensitive) {
                  IconButton(
                    onClick = {
                      revealedIds = if (revealedIds.contains(clip.id)) {
                        revealedIds - clip.id
                      } else {
                        revealedIds + clip.id
                      }
                    },
                    modifier = Modifier.size(36.dp),
                  ) {
                    Icon(
                      imageVector = if (isMasked) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                      contentDescription = "Reveal or hide sensitive clip",
                      tint = warnTint,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
                IconButton(
                  onClick = { onCopyClip(clip) },
                  modifier = Modifier.size(36.dp),
                ) {
                  Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { editingClip = clip },
                  modifier = Modifier.size(36.dp),
                ) {
                  Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(
                  onClick = { onTogglePin(clip) },
                  modifier = Modifier.size(36.dp),
                ) {
                  Icon(
                    imageVector = if (clip.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin",
                    tint = if (clip.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showBenchmarkDialog) {
    AlertDialog(
      onDismissRequest = { showBenchmarkDialog = false },
      title = { Text("Room Scale Benchmark") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Measure real SQLite/Room batch insert, indexed search, and top-100 fetch times directly on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(100, 1000, 10000).forEach { count ->
              OutlinedButton(
                enabled = !isBenchmarkRunning,
                onClick = { onRunBenchmark(count) },
                modifier = Modifier.weight(1f).testTag("bench_${count}_button"),
              ) {
                Text("$count")
              }
            }
          }
          if (isBenchmarkRunning) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Running Room transaction benchmark...", style = MaterialTheme.typography.bodyMedium)
            }
          }
          if (latestBenchmark != null) {
            Text(
              text = "Measured (${latestBenchmark.itemCount} rows):\nInsert=${latestBenchmark.insertDurationMs}ms\nSearchHit=${latestBenchmark.searchHitDurationMs}ms (${latestBenchmark.searchHitCount} hits)\nSearchMiss=${latestBenchmark.searchMissDurationMs}ms\nTop100=${latestBenchmark.topQueryDurationMs}ms\nTotalRows=${latestBenchmark.totalRowsAfter}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showBenchmarkDialog = false }) { Text("Close") }
      },
      dismissButton = {
        TextButton(onClick = onClearBenchmarkRows) { Text("Clear Sample Rows") }
      },
    )
  }

  if (showAddDialog) {
    ClipEditorDialog(
      title = "Add Clipboard Snippet",
      initialText = "",
      folders = folders,
      initialFolderId = filter.folderId ?: DefaultFolders.GENERAL,
      showFolderAndPin = true,
      onDismiss = { showAddDialog = false },
      onConfirm = { text, folderId, pinned ->
        onSaveNewClip(text, folderId, pinned)
        showAddDialog = false
      },
    )
  }

  val toEdit = editingClip
  if (toEdit != null) {
    ClipEditorDialog(
      title = "Edit Clip",
      initialText = toEdit.content,
      folders = folders,
      initialFolderId = toEdit.folderId,
      showFolderAndPin = false,
      onDismiss = { editingClip = null },
      onConfirm = { text, _, _ ->
        onEditClip(toEdit.id, text)
        editingClip = null
      },
    )
  }

  if (showNewFolderDialog) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showNewFolderDialog = false },
      title = { Text("New Clipboard Folder") },
      text = {
        OutlinedTextField(
          value = folderName,
          onValueChange = { folderName = it },
          label = { Text("Folder name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("new_folder_name_input"),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onCreateFolder(folderName)
            showNewFolderDialog = false
          },
          modifier = Modifier.testTag("confirm_new_folder_button"),
        ) { Text("Create") }
      },
      dismissButton = {
        TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
      },
    )
  }

  if (showMoveDialog) {
    AlertDialog(
      onDismissRequest = { showMoveDialog = false },
      title = { Text("Move ${selectedIds.size} Clip(s) To") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          folders.forEach { folder ->
            OutlinedButton(
              onClick = {
                onMoveClips(selectedIds.toList(), folder.id)
                selectedIds = emptySet()
                showMoveDialog = false
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(folder.name)
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun ClipEditorDialog(
  title: String,
  initialText: String,
  folders: List<ClipboardFolderEntity>,
  initialFolderId: String,
  showFolderAndPin: Boolean,
  onDismiss: () -> Unit,
  onConfirm: (String, String, Boolean) -> Unit,
) {
  var text by remember { mutableStateOf(initialText) }
  var selectedFolder by remember { mutableStateOf(initialFolderId) }
  var pinned by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          label = { Text("Clip text") },
          minLines = 3,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth().testTag("clip_editor_text_input"),
        )
        if (showFolderAndPin) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            folders.forEach { f ->
              FilterChip(
                selected = selectedFolder == f.id,
                onClick = { selectedFolder = f.id },
                label = { Text(f.name) },
              )
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = pinned, onCheckedChange = { pinned = it })
            Text("Pin to top", fontWeight = FontWeight.Medium)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(text, selectedFolder, pinned) },
        enabled = text.isNotBlank(),
        modifier = Modifier.testTag("save_clip_dialog_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
