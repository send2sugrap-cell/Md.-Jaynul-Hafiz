package com.example.util

import android.os.Build
import com.example.data.AppDao
import com.example.data.SystemActivityLog
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

class GlobalExceptionHandler(
    private val context: android.content.Context,
    private val appDao: AppDao?,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Log to file first (more reliable)
        try {
            ErrorLogger.logError(context, throwable)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val deviceInfo = "Model: ${Build.MODEL}, Brand: ${Build.BRAND}, SDK: ${Build.VERSION.SDK_INT}"
        val logDetails = "Error: ${throwable.message}\n\nStack Trace:\n$stackTrace\n\nDevice Info: $deviceInfo"

        // Save to DB in a separate thread if available
        appDao?.let { dao ->
            Thread {
                try {
                    val log = SystemActivityLog(
                        actionType = "CRASH",
                        actor = "SYSTEM",
                        details = logDetails,
                        timestamp = Date().time
                    )
                    dao.insertSystemActivityLogSync(log)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        // Wait a bit to let the log be saved, but don't block too long
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }
}
