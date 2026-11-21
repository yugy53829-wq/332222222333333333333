package com.example.telegram

import com.example.data.ClipboardDao
import com.example.data.ClipboardRepository
import com.example.data.DefaultFolders
import com.example.data.SyncStatus
import com.example.security.LogRedactor
import com.example.security.SecretVault
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.Response

sealed interface TelegramOpResult {
  data class BotVerified(val botId: Long, val username: String, val firstName: String) : TelegramOpResult
  data class ChatsDetected(val chats: List<DetectedChat>) : TelegramOpResult
  data class SyncCompleted(val uploadedCount: Int, val skippedSensitive: Int, val failedCount: Int) : TelegramOpResult
  data class RestoreCompleted(val importedCount: Int, val skippedDuplicates: Int, val nextOffset: Long) : TelegramOpResult
  data class RateLimited(val retryAfterSeconds: Int, val message: String) : TelegramOpResult
  data class InvalidCredentials(val message: String) : TelegramOpResult
  data class NetworkFailure(val message: String) : TelegramOpResult
  data class DisabledOrUnconfigured(val message: String) : TelegramOpResult
}

data class DetectedChat(
  val chatId: String,
  val displayName: String,
  val type: String,
)

class TelegramSyncManager(
  private val api: TelegramApiService,
  private val dao: ClipboardDao,
  private val repository: ClipboardRepository,
  private val vault: SecretVault,
  private val isNetworkAvailable: () -> Boolean = { true },
  private val delayFn: suspend (Long) -> Unit = { ms -> delay(ms) },
) {

  private val errorMoshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val errorAdapter = errorMoshi.adapter(TelegramErrorBody::class.java)

  suspend fun verifyBot(): TelegramOpResult = withContext(Dispatchers.IO) {
    val token = vault.getBotToken()
    if (token.isBlank()) {
      return@withContext TelegramOpResult.DisabledOrUnconfigured("Bot token is not configured.")
    }
    if (!isNetworkAvailable()) {
      return@withContext TelegramOpResult.NetworkFailure("No active internet connection.")
    }
    return@withContext try {
      val response = api.getMe(token)
      if (response.isSuccessful && response.body()?.ok == true && response.body()?.result != null) {
        val user = response.body()!!.result!!
        val uname = user.username ?: user.firstName
        vault.setBotUsername(uname)
        TelegramOpResult.BotVerified(
          botId = user.id,
          username = uname,
          firstName = user.firstName,
        )
      } else {
        mapErrorResponse(response)
      }
    } catch (io: IOException) {
      TelegramOpResult.NetworkFailure("Network error: ${LogRedactor.redact(io.message)}")
    } catch (e: Exception) {
      TelegramOpResult.NetworkFailure("Unexpected error: ${LogRedactor.redact(e.message)}")
    }
  }

  suspend fun detectChatsFromUpdates(): TelegramOpResult = withContext(Dispatchers.IO) {
    val token = vault.getBotToken()
    if (token.isBlank()) {
      return@withContext TelegramOpResult.DisabledOrUnconfigured("Enter a Bot Token first.")
    }
    if (!isNetworkAvailable()) {
      return@withContext TelegramOpResult.NetworkFailure("No active internet connection.")
    }
    return@withContext try {
      val response = api.getUpdates(token = token, offset = null, limit = 50, timeout = 0)
      if (response.isSuccessful && response.body()?.ok == true) {
        val updates = response.body()?.result.orEmpty()
        val seen = LinkedHashMap<String, DetectedChat>()
        updates.forEach { upd ->
          val msg = upd.message ?: upd.editedMessage ?: upd.channelPost ?: return@forEach
          val chat = msg.chat ?: return@forEach
          val idStr = chat.id.toString()
          if (!seen.containsKey(idStr)) {
            val label = chat.title ?: chat.username?.let { "@$it" } ?: chat.firstName ?: "Chat $idStr"
            seen[idStr] = DetectedChat(chatId = idStr, displayName = label, type = chat.type)
          }
        }
        TelegramOpResult.ChatsDetected(seen.values.toList())
      } else {
        mapErrorResponse(response)
      }
    } catch (io: IOException) {
      TelegramOpResult.NetworkFailure("Network error: ${LogRedactor.redact(io.message)}")
    } catch (e: Exception) {
      TelegramOpResult.NetworkFailure("Unexpected error: ${LogRedactor.redact(e.message)}")
    }
  }

  suspend fun syncPendingClips(maxBatch: Int = 20): TelegramOpResult = withContext(Dispatchers.IO) {
    val snapshot = vault.settingsFlow.value
    if (!snapshot.telegramSyncEnabled) {
      return@withContext TelegramOpResult.DisabledOrUnconfigured("Telegram sync is disabled by default.")
    }
    val token = vault.getBotToken()
    val chatId = vault.getChatId()
    if (token.isBlank() || chatId.isBlank()) {
      return@withContext TelegramOpResult.DisabledOrUnconfigured("Bot Token and Chat ID are required.")
    }
    if (!isNetworkAvailable()) {
      return@withContext TelegramOpResult.NetworkFailure("Offline: clips remain queued locally.")
    }

    val pending = dao.getPendingSyncClips(limit = maxBatch)
    if (pending.isEmpty()) {
      return@withContext TelegramOpResult.SyncCompleted(0, 0, 0)
    }

    var uploaded = 0
    var skippedSensitive = 0
    var failed = 0

    for (clip in pending) {
      if (clip.isSensitive) {
        skippedSensitive++
        dao.updateSyncState(clip.id, SyncStatus.LOCAL_ONLY, clip.telegramMessageId)
        continue
      }

      val chunks = splitIntoTelegramChunks(clip.content)
      var lastMsgId: Long? = null
      var chunkFailure: TelegramOpResult? = null

      for (chunk in chunks) {
        val sendOutcome = sendWithRetry(token, chatId, chunk)
        when (sendOutcome) {
          is SendChunkResult.Success -> lastMsgId = sendOutcome.messageId
          is SendChunkResult.Failure -> {
            chunkFailure = sendOutcome.error
            break
          }
        }
      }

      if (chunkFailure != null) {
        dao.updateSyncState(clip.id, SyncStatus.FAILED, clip.telegramMessageId)
        failed++
        if (chunkFailure is TelegramOpResult.InvalidCredentials ||
          chunkFailure is TelegramOpResult.RateLimited ||
          chunkFailure is TelegramOpResult.NetworkFailure
        ) {
          return@withContext chunkFailure
        }
      } else {
        dao.updateSyncState(clip.id, SyncStatus.SYNCED, lastMsgId)
        uploaded++
      }
    }

    vault.recordSyncTimestamp()
    TelegramOpResult.SyncCompleted(uploaded, skippedSensitive, failed)
  }

  suspend fun restoreFromUpdates(): TelegramOpResult = withContext(Dispatchers.IO) {
    val snapshot = vault.settingsFlow.value
    if (!snapshot.telegramSyncEnabled) {
      return@withContext TelegramOpResult.DisabledOrUnconfigured("Enable Telegram sync before restoring.")
    }
    val token = vault.getBotToken()
    val targetChatId = vault.getChatId()
    if (token.isBlank() || targetChatId.isBlank()) {
      return@withContext TelegramOpResult.DisabledOrUnconfigured("Bot Token and Chat ID are required.")
    }
    if (!isNetworkAvailable()) {
      return@withContext TelegramOpResult.NetworkFailure("No active internet connection.")
    }

    return@withContext try {
      val offset = snapshot.lastUpdateOffset.takeIf { it > 0L }
      val response = api.getUpdates(token = token, offset = offset, limit = 100, timeout = 0)
      if (!response.isSuccessful || response.body()?.ok != true) {
        return@withContext mapErrorResponse(response)
      }

      val updates = response.body()?.result.orEmpty()
      var imported = 0
      var skipped = 0
      var maxUpdateId = snapshot.lastUpdateOffset - 1L

      for (upd in updates) {
        if (upd.updateId > maxUpdateId) maxUpdateId = upd.updateId
        val msg = upd.message ?: upd.editedMessage ?: upd.channelPost ?: continue
        val chatIdStr = msg.chat?.id?.toString()
        if (chatIdStr != targetChatId) continue

        val rawText = (msg.text ?: msg.caption)?.trim().orEmpty()
        if (rawText.isEmpty()) continue
        val cleaned = stripChunkHeader(rawText)

        val existingByMsgId = dao.findByTelegramMessageId(msg.messageId)
        if (existingByMsgId != null) {
          skipped++
          continue
        }

        val saveRes = repository.saveClip(
          rawText = cleaned,
          folderId = DefaultFolders.TELEGRAM,
          isPinned = false,
          queueForTelegramSync = false,
          telegramMessageId = msg.messageId,
        )
        if (saveRes != null && !saveRes.wasDuplicate) {
          imported++
        } else {
          skipped++
        }
      }

      val nextOffset = if (updates.isNotEmpty()) maxUpdateId + 1L else snapshot.lastUpdateOffset
      if (nextOffset > snapshot.lastUpdateOffset) {
        vault.setLastUpdateOffset(nextOffset)
      }
      vault.recordSyncTimestamp()

      TelegramOpResult.RestoreCompleted(
        importedCount = imported,
        skippedDuplicates = skipped,
        nextOffset = nextOffset,
      )
    } catch (io: IOException) {
      TelegramOpResult.NetworkFailure("Network error: ${LogRedactor.redact(io.message)}")
    } catch (e: Exception) {
      TelegramOpResult.NetworkFailure("Unexpected error: ${LogRedactor.redact(e.message)}")
    }
  }

  private sealed interface SendChunkResult {
    data class Success(val messageId: Long) : SendChunkResult
    data class Failure(val error: TelegramOpResult) : SendChunkResult
  }

  private suspend fun sendWithRetry(
    token: String,
    chatId: String,
    text: String,
    maxAttempts: Int = 2,
  ): SendChunkResult {
    var attempt = 0
    while (attempt < maxAttempts) {
      attempt++
      try {
        val resp = api.sendMessage(
          token = token,
          body = SendMessageRequest(chatId = chatId, text = text),
        )
        if (resp.isSuccessful && resp.body()?.ok == true && resp.body()?.result != null) {
          return SendChunkResult.Success(resp.body()!!.result!!.messageId)
        }
        val mapped = mapErrorResponse(resp)
        if (mapped is TelegramOpResult.RateLimited && attempt < maxAttempts && mapped.retryAfterSeconds <= 3) {
          delayFn(mapped.retryAfterSeconds * 1000L)
          continue
        }
        return SendChunkResult.Failure(mapped)
      } catch (io: IOException) {
        if (attempt < maxAttempts) {
          delayFn(400L * attempt)
          continue
        }
        return SendChunkResult.Failure(
          TelegramOpResult.NetworkFailure("Network error: ${LogRedactor.redact(io.message)}")
        )
      } catch (e: Exception) {
        return SendChunkResult.Failure(
          TelegramOpResult.NetworkFailure("Sync failure: ${LogRedactor.redact(e.message)}")
        )
      }
    }
    return SendChunkResult.Failure(TelegramOpResult.NetworkFailure("Sync retries exhausted."))
  }

  private fun <T> mapErrorResponse(response: Response<TelegramResponse<T>>): TelegramOpResult {
    val code = response.code()
    val rawErr = response.errorBody()?.string().orEmpty()
    val parsed = runCatching { errorAdapter.fromJson(rawErr) }.getOrNull()
    val description = LogRedactor.redact(parsed?.description ?: response.body()?.description ?: response.message())

    if (code == 429 || parsed?.errorCode == 429) {
      val headerRetry = response.headers()["Retry-After"]?.toIntOrNull()
      val retryAfter = parsed?.parameters?.retryAfter ?: headerRetry ?: 10
      return TelegramOpResult.RateLimited(
        retryAfterSeconds = retryAfter,
        message = "Telegram rate limit (HTTP 429). Retry after ${retryAfter}s.",
      )
    }

    if (code == 401 || code == 404 || parsed?.errorCode == 401 || parsed?.errorCode == 404) {
      return TelegramOpResult.InvalidCredentials(
        "Invalid Bot Token or Chat ID (HTTP $code): $description"
      )
    }

    return TelegramOpResult.NetworkFailure("Telegram API error (HTTP $code): $description")
  }

  companion object {
    const val TELEGRAM_MAX_SAFE_CHARS = 3900

    fun splitIntoTelegramChunks(text: String, maxChunkChars: Int = TELEGRAM_MAX_SAFE_CHARS): List<String> {
      if (text.length <= maxChunkChars) return listOf(text)
      val bodyLimit = (maxChunkChars - 24).coerceAtLeast(200)
      val rawChunks = mutableListOf<String>()
      var idx = 0
      while (idx < text.length) {
        var end = (idx + bodyLimit).coerceAtMost(text.length)
        if (end < text.length) {
          val lastSpace = text.lastIndexOf(' ', startIndex = end)
          if (lastSpace > idx + (bodyLimit / 2)) {
            end = lastSpace
          }
        }
        rawChunks.add(text.substring(idx, end).trimEnd())
        idx = end
        while (idx < text.length && text[idx] == ' ') idx++
      }
      val total = rawChunks.size
      return rawChunks.mapIndexed { i, chunk ->
        "[SmartKey ${i + 1}/$total]\n$chunk"
      }
    }

    fun stripChunkHeader(text: String): String {
      val regex = Regex("""^\[SmartKey \d+/\d+]\n""")
      return text.replaceFirst(regex, "")
    }
  }
}
