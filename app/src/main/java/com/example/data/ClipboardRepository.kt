package com.example.data

import com.example.security.SensitiveContentDetector
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ClipboardRepository(
  private val dao: ClipboardDao,
  private val jsonBackup: ClipboardJsonBackup = ClipboardJsonBackup(),
) {

  val folders: Flow<List<ClipboardFolderEntity>> = dao.observeFolders()
  val totalClipCount: Flow<Int> = dao.observeTotalClipCount()
  val folderCounts: Flow<List<FolderCountTuple>> = dao.observeFolderCounts()
  val pendingSyncCount: Flow<Int> = dao.observeCountBySyncStatus(SyncStatus.PENDING)
  val failedSyncCount: Flow<Int> = dao.observeCountBySyncStatus(SyncStatus.FAILED)

  fun observeClips(
    query: String,
    folderId: String?,
    pinnedOnly: Boolean,
    limit: Int = 250,
  ): Flow<List<ClipboardClipEntity>> {
    return dao.observeClips(
      query = query.trim(),
      folderId = folderId,
      pinnedOnly = pinnedOnly,
      limit = limit,
    )
  }

  suspend fun ensureDefaultFolders() = withContext(Dispatchers.IO) {
    dao.insertFoldersBatch(SmartClipboardDatabase.defaultSeedFolders())
  }

  suspend fun saveClip(
    rawText: String,
    folderId: String = DefaultFolders.GENERAL,
    isPinned: Boolean = false,
    queueForTelegramSync: Boolean = false,
    telegramMessageId: Long? = null,
    forceSensitiveFlag: Boolean? = null,
  ): ClipSaveResult? = withContext(Dispatchers.IO) {
    val normalized = rawText.trim()
    if (normalized.isEmpty()) return@withContext null

    ensureDefaultFolders()
    val hash = sha256Hex(normalized)
    val now = System.currentTimeMillis()
    val isSensitive = forceSensitiveFlag ?: SensitiveContentDetector.isSensitiveClipboardText(normalized)

    val existing = dao.findByHash(hash)
    if (existing != null) {
      val updated = existing.copy(
        updatedAt = now,
        isPinned = existing.isPinned || isPinned,
        isSensitive = existing.isSensitive || isSensitive,
        telegramMessageId = existing.telegramMessageId ?: telegramMessageId,
        syncStatus = if (telegramMessageId != null) SyncStatus.SYNCED else existing.syncStatus,
      )
      dao.updateClip(updated)
      return@withContext ClipSaveResult(
        clipId = existing.id,
        wasDuplicate = true,
        flaggedSensitive = updated.isSensitive,
      )
    }

    val syncStatus = when {
      telegramMessageId != null -> SyncStatus.SYNCED
      queueForTelegramSync && !isSensitive -> SyncStatus.PENDING
      else -> SyncStatus.LOCAL_ONLY
    }

    val clip = ClipboardClipEntity(
      content = normalized,
      contentHash = hash,
      previewText = buildPreview(normalized),
      folderId = folderId,
      isPinned = isPinned,
      isSensitive = isSensitive,
      languageTag = detectLanguageTag(normalized),
      charCount = normalized.length,
      createdAt = now,
      updatedAt = now,
      syncStatus = syncStatus,
      telegramMessageId = telegramMessageId,
    )

    val insertedId = dao.insertClip(clip)
    ClipSaveResult(
      clipId = insertedId,
      wasDuplicate = insertedId == -1L,
      flaggedSensitive = isSensitive,
    )
  }

  suspend fun editClip(clipId: Long, newContent: String): Boolean = withContext(Dispatchers.IO) {
    val trimmed = newContent.trim()
    if (trimmed.isEmpty()) return@withContext false
    val existing = dao.findById(clipId) ?: return@withContext false
    val newHash = sha256Hex(trimmed)
    val collision = dao.findByHash(newHash)
    if (collision != null && collision.id != clipId) {
      return@withContext false
    }
    val sensitive = SensitiveContentDetector.isSensitiveClipboardText(trimmed)
    dao.updateClip(
      existing.copy(
        content = trimmed,
        contentHash = newHash,
        previewText = buildPreview(trimmed),
        isSensitive = sensitive,
        languageTag = detectLanguageTag(trimmed),
        charCount = trimmed.length,
        updatedAt = System.currentTimeMillis(),
      )
    )
    true
  }

  suspend fun togglePin(clip: ClipboardClipEntity) = withContext(Dispatchers.IO) {
    dao.setPinned(clip.id, !clip.isPinned, System.currentTimeMillis())
  }

  suspend fun moveClipsToFolder(ids: List<Long>, targetFolderId: String) = withContext(Dispatchers.IO) {
    if (ids.isEmpty()) return@withContext
    dao.moveFolderByIds(ids, targetFolderId, System.currentTimeMillis())
  }

  suspend fun deleteClips(ids: List<Long>) = withContext(Dispatchers.IO) {
    if (ids.isEmpty()) return@withContext
    dao.deleteClipsByIds(ids)
  }

  suspend fun createFolder(name: String): ClipboardFolderEntity? = withContext(Dispatchers.IO) {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return@withContext null
    val id = "folder_${trimmed.lowercase().replace(Regex("[^a-z0-9\\u0600-\\u06FF]+"), "_")}_${System.currentTimeMillis() % 10000}"
    val existingCount = dao.getAllFoldersOnce().size
    val folder = ClipboardFolderEntity(
      id = id,
      name = trimmed.take(28),
      iconKey = "folder",
      sortOrder = existingCount + 1,
      createdAt = System.currentTimeMillis(),
    )
    dao.insertFolder(folder)
    folder
  }

  suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
    dao.deleteFolderSafely(folderId)
  }

  suspend fun getQuickClipsForIme(query: String, folderId: String?, limit: Int = 40): List<ClipboardClipEntity> =
    withContext(Dispatchers.IO) {
      if (query.isBlank() && folderId == null) {
        dao.getTopClips(limit)
      } else if (folderId == null) {
        dao.searchClipsDirect(query.trim(), limit)
      } else {
        dao.searchClipsDirect(query.trim(), limit * 2).filter { it.folderId == folderId }.take(limit)
      }
    }

  suspend fun exportJson(includeSensitive: Boolean = false): String = withContext(Dispatchers.IO) {
    ensureDefaultFolders()
    val folders = dao.getAllFoldersOnce()
    val clips = dao.getAllClipsForExport()
    jsonBackup.encode(folders, clips, includeSensitive)
  }

  suspend fun importJson(rawJson: String): JsonImportSummary = withContext(Dispatchers.IO) {
    ensureDefaultFolders()
    val parsed = jsonBackup.decode(rawJson)
    val existingFolderIds = dao.getAllFoldersOnce().map { it.id }.toMutableSet()
    var foldersCreated = 0

    parsed.folders.forEach { f ->
      if (f.id.isNotBlank() && f.name.isNotBlank() && existingFolderIds.add(f.id)) {
        val inserted = dao.insertFolder(
          ClipboardFolderEntity(
            id = f.id,
            name = f.name.take(32),
            iconKey = f.iconKey,
            sortOrder = f.sortOrder,
            createdAt = if (f.createdAt > 0L) f.createdAt else System.currentTimeMillis(),
          )
        )
        if (inserted != -1L) foldersCreated++
      }
    }

    var imported = 0
    var skipped = 0
    val now = System.currentTimeMillis()
    val candidates = mutableListOf<ClipboardClipEntity>()
    val seenHashes = mutableSetOf<String>()

    parsed.clips.forEach { c ->
      val text = c.content.trim()
      if (text.isEmpty()) return@forEach
      val hash = sha256Hex(text)
      if (!seenHashes.add(hash) || dao.findByHash(hash) != null) {
        skipped++
        return@forEach
      }
      val folderId = if (existingFolderIds.contains(c.folderId)) c.folderId else DefaultFolders.GENERAL
      val sensitive = c.isSensitive || SensitiveContentDetector.isSensitiveClipboardText(text)
      candidates.add(
        ClipboardClipEntity(
          content = text,
          contentHash = hash,
          previewText = buildPreview(text),
          folderId = folderId,
          isPinned = c.isPinned,
          isSensitive = sensitive,
          languageTag = detectLanguageTag(text),
          charCount = text.length,
          createdAt = if (c.createdAt > 0L) c.createdAt else now,
          updatedAt = if (c.updatedAt > 0L) c.updatedAt else now,
          syncStatus = SyncStatus.LOCAL_ONLY,
          telegramMessageId = null,
        )
      )
    }

    if (candidates.isNotEmpty()) {
      val results = dao.insertClipsBatch(candidates)
      imported = results.count { it != -1L }
      skipped += results.count { it == -1L }
    }

    JsonImportSummary(
      importedCount = imported,
      skippedDuplicates = skipped,
      foldersCreated = foldersCreated,
    )
  }

  suspend fun runStressBenchmark(count: Int): BenchmarkReport = withContext(Dispatchers.IO) {
    ensureDefaultFolders()
    val safeCount = count.coerceIn(1, 10_000)
    val runTag = System.currentTimeMillis()
    val prefix = "[BENCH-$runTag]"
    val items = List(safeCount) { index ->
      val body = "$prefix Note #$index — Arabic اختبار الحافظة الذكية token_ref_${index % 97}"
      ClipboardClipEntity(
        content = body,
        contentHash = sha256Hex(body),
        previewText = buildPreview(body),
        folderId = DefaultFolders.GENERAL,
        isPinned = index == safeCount - 1,
        isSensitive = false,
        languageTag = "mixed",
        charCount = body.length,
        createdAt = runTag + index,
        updatedAt = runTag + index,
        syncStatus = SyncStatus.LOCAL_ONLY,
      )
    }

    val insertStart = System.nanoTime()
    dao.insertClipsBatch(items)
    val insertMs = (System.nanoTime() - insertStart) / 1_000_000L

    val hitQuery = "Note #${safeCount / 2}"
    val hitStart = System.nanoTime()
    val hits = dao.searchClipsDirect(hitQuery, 50)
    val hitMs = (System.nanoTime() - hitStart) / 1_000_000L

    val missStart = System.nanoTime()
    dao.searchClipsDirect("__NON_EXISTENT_NEEDLE_${runTag}__", 50)
    val missMs = (System.nanoTime() - missStart) / 1_000_000L

    val topStart = System.nanoTime()
    dao.getTopClips(100)
    val topMs = (System.nanoTime() - topStart) / 1_000_000L

    val totalAfter = dao.getTotalCountOnce()
    BenchmarkReport(
      itemCount = safeCount,
      insertDurationMs = insertMs,
      searchHitDurationMs = hitMs,
      searchMissDurationMs = missMs,
      searchHitCount = hits.size,
      topQueryDurationMs = topMs,
      totalRowsAfter = totalAfter,
    )
  }

  suspend fun clearBenchmarkData() = withContext(Dispatchers.IO) {
    dao.deleteBenchmarkItems("[BENCH-")
  }

  companion object {
    fun sha256Hex(input: String): String {
      val digest = MessageDigest.getInstance("SHA-256")
      val bytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
      return bytes.joinToString("") { "%02x".format(it) }
    }

    fun buildPreview(text: String): String {
      val singleLine = text.replace(Regex("\\s+"), " ").trim()
      return if (singleLine.length <= 180) singleLine else singleLine.take(177) + "..."
    }

    fun detectLanguageTag(text: String): String {
      var hasArabic = false
      var hasLatin = false
      for (ch in text) {
        if (ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F') hasArabic = true
        if (ch in 'A'..'Z' || ch in 'a'..'z') hasLatin = true
        if (hasArabic && hasLatin) return "mixed"
      }
      return when {
        hasArabic -> "ar"
        hasLatin -> "en"
        else -> "mixed"
      }
    }
  }
}
