package com.example

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.ClipboardRepository
import com.example.data.SmartClipboardDatabase
import com.example.security.SecretVault
import com.example.telegram.TelegramApiService
import com.example.telegram.TelegramSyncManager

class SmartKeyboardApp : Application() {

  val database: SmartClipboardDatabase by lazy {
    SmartClipboardDatabase.getInstance(this)
  }

  val secretVault: SecretVault by lazy {
    SecretVault(this)
  }

  val clipboardRepository: ClipboardRepository by lazy {
    ClipboardRepository(database.clipboardDao())
  }

  val telegramApiService: TelegramApiService by lazy {
    TelegramApiService.create()
  }

  val telegramSyncManager: TelegramSyncManager by lazy {
    TelegramSyncManager(
      api = telegramApiService,
      dao = database.clipboardDao(),
      repository = clipboardRepository,
      vault = secretVault,
      isNetworkAvailable = ::isNetworkConnected,
    )
  }

  fun isNetworkConnected(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val active = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(active) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}
