package com.example.telegram

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TelegramApiService {

  @GET("/bot{token}/getMe")
  suspend fun getMe(
    @Path("token") token: String,
  ): Response<TelegramResponse<TelegramUser>>

  @POST("/bot{token}/sendMessage")
  suspend fun sendMessage(
    @Path("token") token: String,
    @Body body: SendMessageRequest,
  ): Response<TelegramResponse<TelegramMessage>>

  @GET("/bot{token}/getUpdates")
  suspend fun getUpdates(
    @Path("token") token: String,
    @Query("offset") offset: Long? = null,
    @Query("limit") limit: Int = 50,
    @Query("timeout") timeout: Int = 0,
  ): Response<TelegramResponse<List<TelegramUpdate>>>

  companion object {
    const val DEFAULT_BASE_URL = "https://api.telegram.org/"

    fun create(
      baseUrl: String = DEFAULT_BASE_URL,
      okHttpClient: OkHttpClient = buildSecureClient(),
    ): TelegramApiService {
      val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
      return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TelegramApiService::class.java)
    }

    /**
     * Intentionally omits HttpLoggingInterceptor because Telegram places the Bot Token inside
     * the URL path (/bot<TOKEN>/...). Logging URLs would leak secrets into Logcat.
     */
    fun buildSecureClient(): OkHttpClient {
      return OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()
    }
  }
}
