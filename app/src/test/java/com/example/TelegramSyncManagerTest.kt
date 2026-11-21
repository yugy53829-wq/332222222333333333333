package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ClipboardRepository
import com.example.data.DefaultFolders
import com.example.data.SmartClipboardDatabase
import com.example.data.SyncStatus
import com.example.security.AesGcmCryptoEngine
import com.example.security.SecretVault
import com.example.telegram.TelegramApiService
import com.example.telegram.TelegramOpResult
import com.example.telegram.TelegramSyncManager
import java.io.IOException
import java.util.ArrayDeque
import javax.crypto.KeyGenerator
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TelegramSyncManagerTest {

  private data class StubHttpReply(
    val code: Int,
    val jsonBody: String,
    val headers: Map<String, String> = emptyMap(),
    val throwIo: Boolean = false,
  )

  private val queuedReplies = ArrayDeque<StubHttpReply>()
  private val recordedPaths = mutableListOf<String>()
  private lateinit var db: SmartClipboardDatabase
  private lateinit var repo: ClipboardRepository
  private lateinit var vault: SecretVault
  private var networkOnline = true

  @Before
  fun setup() {
    queuedReplies.clear()
    recordedPaths.clear()
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, SmartClipboardDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    repo = ClipboardRepository(db.clipboardDao())
    val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    vault = SecretVault(context, AesGcmCryptoEngine(keyProvider = { key }))
    vault.clearTelegramCredentials()
    networkOnline = true
  }

  @After
  fun tearDown() {
    db.close()
  }

  private fun enqueueJson(code: Int, body: String, headers: Map<String, String> = emptyMap()) {
    queuedReplies.addLast(StubHttpReply(code = code, jsonBody = body, headers = headers))
  }

  private fun manager(): TelegramSyncManager {
    val okHttp = OkHttpClient.Builder()
      .addInterceptor(
        Interceptor { chain ->
          val req = chain.request()
          recordedPaths.add(req.url.encodedPath)
          val next = queuedReplies.pollFirst()
            ?: throw IOException("No stub reply queued for ${req.url.encodedPath}")
          if (next.throwIo) throw IOException("Simulated socket timeout")
          val builder = Response.Builder()
            .request(req)
            .protocol(Protocol.HTTP_1_1)
            .code(next.code)
            .message(if (next.code in 200..299) "OK" else "ERR")
            .body(next.jsonBody.toResponseBody("application/json".toMediaType()))
          next.headers.forEach { (k, v) -> builder.header(k, v) }
          builder.build()
        }
      )
      .build()

    val api = TelegramApiService.create(
      baseUrl = "https://api.telegram.org/",
      okHttpClient = okHttp,
    )
    return TelegramSyncManager(
      api = api,
      dao = db.clipboardDao(),
      repository = repo,
      vault = vault,
      isNetworkAvailable = { networkOnline },
      delayFn = {},
    )
  }

  @Test
  fun `sync is disabled by default until user opts in`() = runBlocking {
    assertFalse(vault.settingsFlow.value.telegramSyncEnabled)
    val res = manager().syncPendingClips()
    assertTrue(res is TelegramOpResult.DisabledOrUnconfigured)
  }

  @Test
  fun `verifyBot and detectChats parse valid Telegram API responses`() = runBlocking {
    vault.setBotToken("111222333:AAFakeTestTokenForMockServerOnly00001")
    assertFalse(vault.hasPlaintextBotTokenInPrefs())

    enqueueJson(
      200,
      """{"ok":true,"result":{"id":999,"is_bot":true,"first_name":"SmartKeyBot","username":"smartkey_test_bot"}}""",
    )
    val verifyRes = manager().verifyBot()
    assertTrue("Expected BotVerified, got $verifyRes", verifyRes is TelegramOpResult.BotVerified)
    assertEquals("smartkey_test_bot", (verifyRes as TelegramOpResult.BotVerified).username)

    enqueueJson(
      200,
      """{"ok":true,"result":[{"update_id":10,"message":{"message_id":55,"date":1700000000,"chat":{"id":777888,"type":"private","first_name":"Tariq"},"text":"/start"}}]}""",
    )
    val chatsRes = manager().detectChatsFromUpdates()
    assertTrue(chatsRes is TelegramOpResult.ChatsDetected)
    val chats = (chatsRes as TelegramOpResult.ChatsDetected).chats
    assertEquals(1, chats.size)
    assertEquals("777888", chats.first().chatId)
  }

  @Test
  fun `sync splits long messages, skips sensitive clips, and restores without duplicates`() = runBlocking {
    vault.setBotToken("111222333:AAFakeTestTokenForMockServerOnly00001")
    vault.setChatId("777888")
    vault.setTelegramSyncEnabled(true)

    val longBody = "Arabic سلام ".repeat(500) // > 5000 chars -> requires chunking
    val normal = repo.saveClip(longBody, DefaultFolders.GENERAL, queueForTelegramSync = true)!!
    repo.saveClip("654321", DefaultFolders.GENERAL, queueForTelegramSync = true) // sensitive OTP

    enqueueJson(
      200,
      """{"ok":true,"result":{"message_id":101,"date":1700000001,"text":"chunk1"}}""",
    )
    enqueueJson(
      200,
      """{"ok":true,"result":{"message_id":102,"date":1700000002,"text":"chunk2"}}""",
    )

    val rawSyncRes = manager().syncPendingClips()
    assertTrue("Expected SyncCompleted, got $rawSyncRes", rawSyncRes is TelegramOpResult.SyncCompleted)
    val syncRes = rawSyncRes as TelegramOpResult.SyncCompleted
    assertEquals(1, syncRes.uploadedCount)
    assertEquals(0, syncRes.failedCount)
    assertEquals(SyncStatus.SYNCED, db.clipboardDao().findById(normal.clipId)!!.syncStatus)
    assertEquals(2, recordedPaths.count { it.endsWith("/sendMessage") })

    // Restore from updates: one new message, one duplicate message_id
    enqueueJson(
      200,
      """{"ok":true,"result":[
        {"update_id":20,"message":{"message_id":102,"date":1700000002,"chat":{"id":777888,"type":"private"},"text":"duplicate"}},
        {"update_id":21,"message":{"message_id":205,"date":1700000010,"chat":{"id":777888,"type":"private"},"text":"Restored snippet from desktop"}}
      ]}""",
    )

    val restoreRes = manager().restoreFromUpdates() as TelegramOpResult.RestoreCompleted
    assertEquals(1, restoreRes.importedCount)
    assertEquals(1, restoreRes.skippedDuplicates)
    assertEquals(22L, vault.settingsFlow.value.lastUpdateOffset)
  }

  @Test
  fun `handles http 429 rate limit, http 401 invalid token, and offline network`() = runBlocking {
    vault.setBotToken("111222333:AAFakeTestTokenForMockServerOnly00001")
    vault.setChatId("777888")
    vault.setTelegramSyncEnabled(true)

    val saved = repo.saveClip("Needs sync retry", DefaultFolders.GENERAL, queueForTelegramSync = true)!!

    // Offline test
    networkOnline = false
    val offlineRes = manager().syncPendingClips()
    assertTrue(offlineRes is TelegramOpResult.NetworkFailure)
    assertEquals(SyncStatus.PENDING, db.clipboardDao().findById(saved.clipId)!!.syncStatus)

    // HTTP 429 rate limit with Retry-After > 3s returns RateLimited cleanly
    networkOnline = true
    enqueueJson(
      429,
      """{"ok":false,"error_code":429,"description":"Too Many Requests: retry after 12","parameters":{"retry_after":12}}""",
    )
    val rateLimitRes = manager().syncPendingClips()
    assertTrue("Expected RateLimited, got $rateLimitRes", rateLimitRes is TelegramOpResult.RateLimited)
    assertEquals(12, (rateLimitRes as TelegramOpResult.RateLimited).retryAfterSeconds)

    // HTTP 401 invalid token
    enqueueJson(
      401,
      """{"ok":false,"error_code":401,"description":"Unauthorized"}""",
    )
    val authRes = manager().verifyBot()
    assertTrue("Expected InvalidCredentials, got $authRes", authRes is TelegramOpResult.InvalidCredentials)
  }
}
