package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class ClipboardBackupFolderJson(
  val id: String,
  val name: String,
  val iconKey: String = "folder",
  val sortOrder: Int = 0,
  val createdAt: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ClipboardBackupClipJson(
  val content: String,
  val folderId: String = DefaultFolders.GENERAL,
  val isPinned: Boolean = false,
  val isSensitive: Boolean = false,
  val languageTag: String = "mixed",
  val createdAt: Long = 0L,
  val updatedAt: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class ClipboardBackupEnvelope(
  val schemaVersion: Int = 1,
  val exportedAt: Long,
  val includesSensitiveClips: Boolean = false,
  val folders: List<ClipboardBackupFolderJson>,
  val clips: List<ClipboardBackupClipJson>,
)

data class JsonImportSummary(
  val importedCount: Int,
  val skippedDuplicates: Int,
  val foldersCreated: Int,
)

class ClipboardJsonBackup(
  moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
) {
  private val adapter = moshi.adapter(ClipboardBackupEnvelope::class.java).indent("  ")

  fun encode(
    folders: List<ClipboardFolderEntity>,
    clips: List<ClipboardClipEntity>,
    includeSensitive: Boolean = false,
    exportedAt: Long = System.currentTimeMillis(),
  ): String {
    val filteredClips = if (includeSensitive) clips else clips.filterNot { it.isSensitive }
    val envelope = ClipboardBackupEnvelope(
      schemaVersion = 1,
      exportedAt = exportedAt,
      includesSensitiveClips = includeSensitive,
      folders = folders.map {
        ClipboardBackupFolderJson(
          id = it.id,
          name = it.name,
          iconKey = it.iconKey,
          sortOrder = it.sortOrder,
          createdAt = it.createdAt,
        )
      },
      clips = filteredClips.map {
        ClipboardBackupClipJson(
          content = it.content,
          folderId = it.folderId,
          isPinned = it.isPinned,
          isSensitive = it.isSensitive,
          languageTag = it.languageTag,
          createdAt = it.createdAt,
          updatedAt = it.updatedAt,
        )
      },
    )
    return adapter.toJson(envelope)
  }

  fun decode(rawJson: String): ClipboardBackupEnvelope {
    require(rawJson.isNotBlank()) { "Backup JSON is empty." }
    require(rawJson.length <= MAX_IMPORT_CHARS) { "Backup JSON exceeds 15 MB safety limit." }
    val parsed = adapter.fromJson(rawJson) ?: throw IllegalArgumentException("Malformed JSON backup.")
    require(parsed.schemaVersion == 1) { "Unsupported backup schemaVersion: ${parsed.schemaVersion}" }
    return parsed
  }

  companion object {
    private const val MAX_IMPORT_CHARS = 15_000_000
  }
}
