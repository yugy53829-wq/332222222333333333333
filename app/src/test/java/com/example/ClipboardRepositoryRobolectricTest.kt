package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ClipboardRepository
import com.example.data.DefaultFolders
import com.example.data.SmartClipboardDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClipboardRepositoryRobolectricTest {

  private lateinit var db: SmartClipboardDatabase
  private lateinit var repo: ClipboardRepository

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, SmartClipboardDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repo = ClipboardRepository(db.clipboardDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `deduplicates clips, pins, moves folders, edits, and deletes`() = runBlocking {
    val first = repo.saveClip("مرحبا بك في الحافظة الذكية", DefaultFolders.GENERAL, isPinned = false)
    assertNotNull(first)
    assertFalse(first!!.wasDuplicate)

    val duplicate = repo.saveClip("  مرحبا بك في الحافظة الذكية  ", DefaultFolders.GENERAL, isPinned = true)
    assertNotNull(duplicate)
    assertTrue(duplicate!!.wasDuplicate)
    assertEquals(1, db.clipboardDao().getTotalCountOnce())

    val customFolder = repo.createFolder("Personal")
    assertNotNull(customFolder)

    repo.moveClipsToFolder(listOf(first.clipId), customFolder!!.id)
    val moved = db.clipboardDao().findById(first.clipId)!!
    assertEquals(customFolder.id, moved.folderId)
    assertTrue(moved.isPinned)

    assertTrue(repo.editClip(first.clipId, "تم تعديل النص بنجاح"))
    assertEquals("تم تعديل النص بنجاح", db.clipboardDao().findById(first.clipId)!!.content)

    repo.deleteClips(listOf(first.clipId))
    assertEquals(0, db.clipboardDao().getTotalCountOnce())
  }

  @Test
  fun `json export excludes sensitive clips by default and import deduplicates`() = runBlocking {
    repo.saveClip("Public snippet for export", DefaultFolders.SNIPPETS, isPinned = true)
    repo.saveClip("123456", DefaultFolders.GENERAL, isPinned = false) // flagged sensitive OTP

    val exportedJson = repo.exportJson(includeSensitive = false)
    assertTrue(exportedJson.contains("Public snippet for export"))
    assertFalse(exportedJson.contains("123456"))

    val reimportSummary = repo.importJson(exportedJson)
    assertEquals(0, reimportSummary.importedCount)
    assertEquals(1, reimportSummary.skippedDuplicates)

    val freshDb = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      SmartClipboardDatabase::class.java,
    ).allowMainThreadQueries().build()
    val freshRepo = ClipboardRepository(freshDb.clipboardDao())

    val freshSummary = freshRepo.importJson(exportedJson)
    assertEquals(1, freshSummary.importedCount)
    val restored = freshRepo.observeClips("", null, false).first()
    assertEquals("Public snippet for export", restored.single().content)
    freshDb.close()
  }

  @Test
  fun `stress benchmark handles 1000 and 10000 items cleanly`() = runBlocking {
    val report1k = repo.runStressBenchmark(1_000)
    assertEquals(1_000, report1k.itemCount)
    assertEquals(1, report1k.searchHitCount)
    assertEquals(1_000, report1k.totalRowsAfter)

    repo.clearBenchmarkData()
    assertEquals(0, db.clipboardDao().getTotalCountOnce())

    val report10k = repo.runStressBenchmark(10_000)
    assertEquals(10_000, report10k.itemCount)
    assertEquals(1, report10k.searchHitCount)
    assertEquals(10_000, report10k.totalRowsAfter)
    println(
      "BENCHMARK_10K insertMs=${report10k.insertDurationMs} " +
        "searchHitMs=${report10k.searchHitDurationMs} " +
        "searchMissMs=${report10k.searchMissDurationMs} " +
        "top100Ms=${report10k.topQueryDurationMs}"
    )
  }
}
