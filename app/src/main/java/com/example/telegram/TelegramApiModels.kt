package com.example.telegram

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TelegramResponse<T>(
  val ok: Boolean,
  val result: T? = null,
  @Json(name = "error_code") val errorCode: Int? = null,
  val description: String? = null,
  val parameters: TelegramResponseParameters? = null,
)

@JsonClass(generateAdapter = true)
data class TelegramErrorBody(
  val ok: Boolean = false,
  @Json(name = "error_code") val errorCode: Int? = null,
  val description: String? = null,
  val parameters: TelegramResponseParameters? = null,
)

@JsonClass(generateAdapter = true)
data class TelegramResponseParameters(
  @Json(name = "retry_after") val retryAfter: Int? = null,
)

@JsonClass(generateAdapter = true)
data class TelegramUser(
  val id: Long,
  @Json(name = "is_bot") val isBot: Boolean = true,
  @Json(name = "first_name") val firstName: String = "",
  val username: String? = null,
)

@JsonClass(generateAdapter = true)
data class TelegramChat(
  val id: Long,
  val type: String = "private",
  val title: String? = null,
  val username: String? = null,
  @Json(name = "first_name") val firstName: String? = null,
)

@JsonClass(generateAdapter = true)
data class TelegramMessage(
  @Json(name = "message_id") val messageId: Long,
  val date: Long = 0L,
  val chat: TelegramChat? = null,
  val text: String? = null,
  val caption: String? = null,
)

@JsonClass(generateAdapter = true)
data class TelegramUpdate(
  @Json(name = "update_id") val updateId: Long,
  val message: TelegramMessage? = null,
  @Json(name = "edited_message") val editedMessage: TelegramMessage? = null,
  @Json(name = "channel_post") val channelPost: TelegramMessage? = null,
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
  @Json(name = "chat_id") val chatId: String,
  val text: String,
  @Json(name = "disable_web_page_preview") val disableWebPagePreview: Boolean = true,
)
