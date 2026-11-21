package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
  entities = [ClipboardClipEntity::class, ClipboardFolderEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class SmartClipboardDatabase : RoomDatabase() {
  abstract fun clipboardDao(): ClipboardDao

  companion object {
    const val DB_NAME = "smart_clipboard.db"

    @Volatile private var instance: SmartClipboardDatabase? = null

    fun getInstance(context: Context): SmartClipboardDatabase {
      return instance ?: synchronized(this) {
        instance ?: buildDatabase(context.applicationContext).also { instance = it }
      }
    }

    fun defaultSeedFolders(now: Long = System.currentTimeMillis()): List<ClipboardFolderEntity> =
      listOf(
        ClipboardFolderEntity(DefaultFolders.GENERAL, "General", "inbox", 0, now),
        ClipboardFolderEntity(DefaultFolders.WORK, "Work", "work", 1, now),
        ClipboardFolderEntity(DefaultFolders.SNIPPETS, "Snippets", "bolt", 2, now),
        ClipboardFolderEntity(DefaultFolders.TELEGRAM, "Telegram", "cloud", 3, now),
      )

    private fun buildDatabase(appContext: Context): SmartClipboardDatabase {
      return Room.databaseBuilder(
        appContext,
        SmartClipboardDatabase::class.java,
        DB_NAME,
      )
        .addCallback(
          object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
              super.onCreate(db)
              CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                getInstance(appContext).clipboardDao().insertFoldersBatch(defaultSeedFolders())
              }
            }
          }
        )
        .build()
    }
  }
}
