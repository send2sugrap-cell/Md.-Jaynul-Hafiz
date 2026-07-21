package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.AppRepository

class SucharuApp : Application() {
    val database by lazy {
        try {
            Room.databaseBuilder(this, AppDatabase::class.java, "sucharu_db").fallbackToDestructiveMigration().build()
        } catch (e: Exception) {
            android.util.Log.e("SucharuApp", "Database init failed", e)
            null
        }
    }
    val repository by lazy {
        try {
            database?.let { AppRepository(it.appDao()) }
        } catch (e: Exception) {
            android.util.Log.e("SucharuApp", "Repository init failed", e)
            null
        }
    }
    val settingsManager by lazy {
        try {
            com.example.data.SettingsManager(this)
        } catch (e: Exception) {
            android.util.Log.e("SucharuApp", "SettingsManager init failed", e)
            null
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            com.example.util.ErrorLogger.init(this)
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                com.example.util.GlobalExceptionHandler(
                    context = this,
                    appDao = database?.appDao(),
                    defaultHandler = defaultHandler
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("SucharuApp", "GlobalExceptionHandler init failed", e)
        }
    }
}
