package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class ErrorEntry(
    val timestamp: Long,
    val dateString: String = "",
    val message: String,
    val stackTrace: String,
    val appVersion: String = "Unknown",
    val osVersion: String = "Unknown",
    val deviceModel: String = "Unknown",
    val availableMemoryMb: Long = 0L,
    val userAction: String = "Unknown"
)

object ErrorLogger {
    private const val FILE_NAME = "error_log.json"
    private const val SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000
    private val logJson = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true 
    }
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logError(appContext!!, throwable, "Unhandled Exception")
            defaultHandler?.uncaughtException(thread, throwable)
        }
        cleanupOldLogs(appContext!!)
    }

    fun logError(context: Context, throwable: Throwable, userAction: String = "Unknown") {
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }

        val entry = ErrorEntry(
            timestamp = System.currentTimeMillis(),
            dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            message = throwable.message ?: "Unknown error",
            stackTrace = stackTrace,
            appVersion = appVersion,
            osVersion = Build.VERSION.RELEASE,
            deviceModel = Build.MODEL,
            availableMemoryMb = memoryInfo.availMem / (1024 * 1024),
            userAction = userAction
        )
        
        val file = File(context.filesDir, FILE_NAME)
        val existingLogs = if (file.exists()) {
            try {
                logJson.decodeFromString<List<ErrorEntry>>(file.readText())
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        val updatedLogs = existingLogs + entry
        file.writeText(logJson.encodeToString(updatedLogs))
    }

    private fun cleanupOldLogs(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return
        
        try {
            val existingLogs = logJson.decodeFromString<List<ErrorEntry>>(file.readText())
            val currentTime = System.currentTimeMillis()
            val filteredLogs = existingLogs.filter { currentTime - it.timestamp < SEVEN_DAYS_MS }
            if (filteredLogs.size < existingLogs.size) {
                file.writeText(logJson.encodeToString(filteredLogs))
            }
        } catch (e: Exception) {
            // Ignore format issues during cleanup
        }
    }

    fun getLogs(context: Context): List<ErrorEntry> {
        val file = File(context.filesDir, FILE_NAME)
        return if (file.exists()) {
            try {
                logJson.decodeFromString<List<ErrorEntry>>(file.readText()).reversed()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun generateDiagnosticReport(context: Context): String {
        val logs = getLogs(context)
        if (logs.isEmpty()) return "No diagnostic data available."
        
        val latestLog = logs.first()
        return buildString {
            appendLine("=== DIAGNOSTIC REPORT ===")
            appendLine("App Version: ${latestLog.appVersion}")
            appendLine("Device Model: ${latestLog.deviceModel}")
            appendLine("OS Version: ${latestLog.osVersion}")
            appendLine("Available Memory: ${latestLog.availableMemoryMb} MB")
            appendLine("Crash Time: ${latestLog.dateString}")
            appendLine("Last Action: ${latestLog.userAction}")
            appendLine("Error Message: ${latestLog.message}")
            appendLine("\n--- Stack Trace ---")
            appendLine(latestLog.stackTrace)
        }
    }
}
