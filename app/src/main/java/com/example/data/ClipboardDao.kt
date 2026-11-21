package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {

  @Query(
    """
    SELECT * FROM clipboard_clips
    WHERE (:folderId IS NULL OR folderId = :folderId)
      AND (:pinnedOnly = 0 OR isPinned = 1)
      AND (:query = '' OR content LIKE '%' || :query || '%')
    ORDER BY isPinned DESC, updatedAt DESC
    LIMIT :limit
    """
  )
  fun observeClips(
    query: String,
    folderId: String?,
    pinnedOnly: Boolean,
    limit: Int = 250,
  ): Flow<List<ClipboardClipEntity>>

  @Query("SELECT * FROM clipboard_folders ORDER BY sortOrder ASC, createdAt ASC")
  fun observeFolders(): Flow<List<ClipboardFolderEntity>>

  @Query("SELECT COUNT(*) FROM clipboard_clips")
  fun observeTotalClipCount(): Flow<Int>

  @Query("SELECT folderId, COUNT(*) as clipCount FROM clipboard_clips GROUP BY folderId")
  fun observeFolderCounts(): Flow<List<FolderCountTuple>>

  @Query("SELECT COUNT(*) FROM clipboard_clips WHERE syncStatus = :status")
  fun observeCountBySyncStatus(status: String): Flow<Int>

  @Query("SELECT * FROM clipboard_clips WHERE contentHash = :hash LIMIT 1")
  suspend fun findByHash(hash: String): ClipboardClipEntity?

  @Query("SELECT * FROM clipboard_clips WHERE telegramMessageId = :messageId LIMIT 1")
  suspend fun findByTelegramMessageId(messageId: Long): ClipboardClipEntity?

  @Query("SELECT * FROM clipboard_clips WHERE id = :id LIMIT 1")
  suspend fun findById(id: Long): ClipboardClipEntity?

  @Query("SELECT * FROM clipboard_clips ORDER BY isPinned DESC, updatedAt DESC LIMIT :limit")
  suspend fun getTopClips(limit: Int = 100): List<ClipboardClipEntity>

  @Query(
    """
    SELECT * FROM clipboard_clips
    WHERE content LIKE '%' || :query || '%'
    ORDER BY isPinned DESC, updatedAt DESC
    LIMIT :limit
    """
  )
  suspend fun searchClipsDirect(query: String, limit: Int = 100): List<ClipboardClipEntity>

  @Query(
    """
    SELECT * FROM clipboard_clips
    WHERE isSensitive = 0 AND syncStatus IN ('PENDING', 'FAILED')
    ORDER BY updatedAt ASC
    LIMIT :limit
    """
  )
  suspend fun getPendingSyncClips(limit: Int = 25): List<ClipboardClipEntity>

  @Query("SELECT * FROM clipboard_clips ORDER BY createdAt ASC")
  suspend fun getAllClipsForExport(): List<ClipboardClipEntity>

  @Query("SELECT * FROM clipboard_folders ORDER BY sortOrder ASC, createdAt ASC")
  suspend fun getAllFoldersOnce(): List<ClipboardFolderEntity>

  @Query("SELECT COUNT(*) FROM clipboard_clips")
  suspend fun getTotalCountOnce(): Int

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertClip(clip: ClipboardClipEntity): Long

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertClipsBatch(clips: List<ClipboardClipEntity>): List<Long>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertFolder(folder: ClipboardFolderEntity): Long

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertFoldersBatch(folders: List<ClipboardFolderEntity>)

  @Update
  suspend fun updateClip(clip: ClipboardClipEntity)

  @Query("UPDATE clipboard_clips SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
  suspend fun setPinned(id: Long, pinned: Boolean, updatedAt: Long)

  @Query("UPDATE clipboard_clips SET folderId = :folderId, updatedAt = :updatedAt WHERE id IN (:ids)")
  suspend fun moveFolderByIds(ids: List<Long>, folderId: String, updatedAt: Long)

  @Query("UPDATE clipboard_clips SET syncStatus = :status, telegramMessageId = :messageId WHERE id = :id")
  suspend fun updateSyncState(id: Long, status: String, messageId: Long?)

  @Query("DELETE FROM clipboard_clips WHERE id IN (:ids)")
  suspend fun deleteClipsByIds(ids: List<Long>)

  @Query("UPDATE clipboard_clips SET folderId = :fallbackFolderId WHERE folderId = :deletedFolderId")
  suspend fun reassignFolder(deletedFolderId: String, fallbackFolderId: String = DefaultFolders.GENERAL)

  @Query("DELETE FROM clipboard_folders WHERE id = :folderId")
  suspend fun deleteFolderById(folderId: String)

  @Query("DELETE FROM clipboard_clips WHERE content LIKE :prefix || '%'")
  suspend fun deleteBenchmarkItems(prefix: String)

  @Transaction
  suspend fun deleteFolderSafely(folderId: String) {
    if (folderId == DefaultFolders.GENERAL) return
    reassignFolder(folderId, DefaultFolders.GENERAL)
    deleteFolderById(folderId)
  }
}
