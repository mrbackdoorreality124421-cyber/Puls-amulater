package com.example.engine

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class InternalCrashEntry(
    val id: String,
    val exceptionType: String,
    val message: String,
    val stackTrace: String,
    val subsystem: String,
    val androidVersion: String,
    val sdkInt: Int,
    val deviceModel: String,
    val timestamp: Long,
    val formattedDate: String,
    val isFatal: Boolean
)

enum class SubsystemStatus {
    HEALTHY,
    DEGRADED,
    DISABLED
}

object CrashLogger {
    private const val TAG = "PulsePC_CrashLogger"
    private const val PREFS_NAME = "pulsepc_stability_prefs"
    private const val KEY_CRASH_COUNT = "consecutive_startup_crashes"
    private const val KEY_SAFE_MODE = "is_safe_mode_activated"
    private const val KEY_LAST_CLEAN_START = "last_clean_startup_timestamp"
    private const val CRASH_LOG_FILE = "pulsepc_crash_history.json"
    private const val MAX_LOGGED_CRASHES = 20

    private val subsystemHealth = mutableMapOf<String, SubsystemStatus>(
        "DeviceAnalyzer" to SubsystemStatus.HEALTHY,
        "ArchiveManager" to SubsystemStatus.HEALTHY,
        "GameAnalyzer" to SubsystemStatus.HEALTHY,
        "ControlEngine" to SubsystemStatus.HEALTHY,
        "RuntimeCompatibilityLayer" to SubsystemStatus.HEALTHY,
        "Database" to SubsystemStatus.HEALTHY
    )

    fun initialize(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordCrash(
                    context = context,
                    throwable = throwable,
                    subsystem = "UncaughtGlobalException",
                    isFatal = true
                )
                incrementConsecutiveCrashCount(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record fatal crash", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isSafeModeActive(context: Context): Boolean {
        val count = getPrefs(context).getInt(KEY_CRASH_COUNT, 0)
        val explicitSafeMode = getPrefs(context).getBoolean(KEY_SAFE_MODE, false)
        return explicitSafeMode || count >= 2
    }

    fun setSafeModeActive(context: Context, active: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SAFE_MODE, active).apply()
        if (!active) {
            resetCrashCount(context)
        }
    }

    fun incrementConsecutiveCrashCount(context: Context) {
        val prefs = getPrefs(context)
        val current = prefs.getInt(KEY_CRASH_COUNT, 0)
        prefs.edit().putInt(KEY_CRASH_COUNT, current + 1).apply()
    }

    fun markSuccessfulStartup(context: Context) {
        getPrefs(context).edit()
            .putInt(KEY_CRASH_COUNT, 0)
            .putLong(KEY_LAST_CLEAN_START, System.currentTimeMillis())
            .apply()
    }

    fun resetCrashCount(context: Context) {
        getPrefs(context).edit().putInt(KEY_CRASH_COUNT, 0).apply()
    }

    fun recordCrash(
        context: Context,
        throwable: Throwable,
        subsystem: String,
        isFatal: Boolean = false
    ): InternalCrashEntry {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val timestamp = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))

        val entry = InternalCrashEntry(
            id = "crash_${timestamp}_${(1000..9999).random()}",
            exceptionType = throwable.javaClass.name,
            message = throwable.localizedMessage ?: throwable.message ?: "No error message",
            stackTrace = stackTrace,
            subsystem = subsystem,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            sdkInt = Build.VERSION.SDK_INT,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            timestamp = timestamp,
            formattedDate = dateStr,
            isFatal = isFatal
        )

        Log.e(TAG, "[$subsystem] Crash captured: ${entry.exceptionType} - ${entry.message}")

        if (isFatal) {
            setSubsystemStatus(subsystem, SubsystemStatus.DISABLED)
        } else {
            setSubsystemStatus(subsystem, SubsystemStatus.DEGRADED)
        }

        saveCrashToFile(context, entry)
        return entry
    }

    fun setSubsystemStatus(subsystem: String, status: SubsystemStatus) {
        subsystemHealth[subsystem] = status
    }

    fun getSubsystemStatus(subsystem: String): SubsystemStatus {
        return subsystemHealth[subsystem] ?: SubsystemStatus.HEALTHY
    }

    fun getAllSubsystemStatuses(): Map<String, SubsystemStatus> {
        return subsystemHealth.toMap()
    }

    private fun saveCrashToFile(context: Context, entry: InternalCrashEntry) {
        try {
            val file = File(context.filesDir, CRASH_LOG_FILE)
            val jsonArray = if (file.exists() && file.length() > 0) {
                try {
                    JSONArray(file.readText())
                } catch (e: Exception) {
                    JSONArray()
                }
            } else {
                JSONArray()
            }

            val obj = JSONObject().apply {
                put("id", entry.id)
                put("exceptionType", entry.exceptionType)
                put("message", entry.message)
                put("stackTrace", entry.stackTrace.take(1500))
                put("subsystem", entry.subsystem)
                put("androidVersion", entry.androidVersion)
                put("sdkInt", entry.sdkInt)
                put("deviceModel", entry.deviceModel)
                put("timestamp", entry.timestamp)
                put("formattedDate", entry.formattedDate)
                put("isFatal", entry.isFatal)
            }

            jsonArray.put(obj)

            // Trim old entries if above limit
            val trimmedArray = JSONArray()
            val startIndex = (jsonArray.length() - MAX_LOGGED_CRASHES).coerceAtLeast(0)
            for (i in startIndex until jsonArray.length()) {
                trimmedArray.put(jsonArray.get(i))
            }

            file.writeText(trimmedArray.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist crash entry to file", e)
        }
    }

    fun getCrashHistory(context: Context): List<InternalCrashEntry> {
        return try {
            val file = File(context.filesDir, CRASH_LOG_FILE)
            if (!file.exists()) return emptyList()

            val jsonArray = JSONArray(file.readText())
            val list = mutableListOf<InternalCrashEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    InternalCrashEntry(
                        id = obj.optString("id"),
                        exceptionType = obj.optString("exceptionType"),
                        message = obj.optString("message"),
                        stackTrace = obj.optString("stackTrace"),
                        subsystem = obj.optString("subsystem"),
                        androidVersion = obj.optString("androidVersion"),
                        sdkInt = obj.optInt("sdkInt"),
                        deviceModel = obj.optString("deviceModel"),
                        timestamp = obj.optLong("timestamp"),
                        formattedDate = obj.optString("formattedDate"),
                        isFatal = obj.optBoolean("isFatal")
                    )
                )
            }
            list.reversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearCrashHistory(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, CRASH_LOG_FILE)
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            false
        }
    }
}
