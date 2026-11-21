package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object DefaultFolders {
  const val GENERAL = "general"
  const val WORK = "work"
  const val SNIPPETS = "snippets"
  const val TELEGRAM = "telegram"
}

object SyncStatus {
  const val LOCAL_ONLY = "LOCAL_ONLY"
  const val PENDING = "PENDING"
  const val SYNCED = "SYNCED"
  const val FAILED = "FAILED"
}

@Entity(tableName = "clipboard_folders")
data class ClipboardFolderEntity(
  @PrimaryKey val id: String,
  val name: String,
  val iconKey: String = "folder",
  val sortOrder: Int = 0,
  val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "clipboard_clips",
  indices = [
    Index(value = ["contentHash"], unique = true),
    Index(value = ["folderId"]),
    Index(value = ["isPinned", "updatedAt"]),
    Index(value = ["syncStatus"]),
    Index(value = ["telegramMessageId"]),
  ],
)
data class ClipboardClipEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val content: String,
  val contentHash: String,
  val previewText: String,
  val folderId: String = DefaultFolders.GENERAL,
  val isPinned: Boolean = false,
  val isSensitive: Boolean = false,
  val languageTag: String = "mixed",
  val charCount: Int = content.length,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val syncStatus: String = SyncStatus.LOCAL_ONLY,
  val telegramMessageId: Long? = null,
)

data class FolderCountTuple(
  @ColumnInfo(name = "folderId") val folderId: String,
  @ColumnInfo(name = "clipCount") val clipCount: Int,
)

data class ClipSaveResult(
  val clipId: Long,
  val wasDuplicate: Boolean,
  val flaggedSensitive: Boolean,
)

data class BenchmarkReport(
  val itemCount: Int,
  val insertDurationMs: Long,
  val searchHitDurationMs: Long,
  val searchMissDurationMs: Long,
  val searchHitCount: Int,
  val topQueryDurationMs: Long,
  val totalRowsAfter: Int,
)
