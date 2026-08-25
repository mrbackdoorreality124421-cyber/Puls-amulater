package com.example.engine

import android.content.Context
import com.example.data.model.GameEntity
import java.io.File

data class CacheDetails(
    val shaderCacheSizeBytes: Long,
    val temporaryFilesSizeBytes: Long,
    val logsSizeBytes: Long,
    val formattedTotalSize: String
)

object StorageCacheManager {

    fun getProfileDirectory(context: Context, gameId: String): File {
        val dir = File(context.filesDir, "game_profiles/$gameId")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getShaderCacheDirectory(context: Context, gameId: String): File {
        val dir = File(getProfileDirectory(context, gameId), "shader_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun backupConfiguration(context: Context, game: GameEntity) {
        val profileDir = getProfileDirectory(context, game.id)
        val backupFile = File(profileDir, "config_backup.json")
        if (game.configJson.isNotEmpty()) {
            backupFile.writeText(game.configJson)
        }
    }

    fun restoreBackupConfiguration(context: Context, gameId: String): String? {
        val profileDir = getProfileDirectory(context, gameId)
        val backupFile = File(profileDir, "config_backup.json")
        return if (backupFile.exists()) {
            backupFile.readText()
        } else {
            null
        }
    }

    fun getCacheDetails(context: Context, gameId: String): CacheDetails {
        val profileDir = getProfileDirectory(context, gameId)
        val shaderDir = File(profileDir, "shader_cache")
        val logsDir = File(profileDir, "logs")
        val tmpDir = File(profileDir, "tmp")

        val shaderSize = getDirectorySize(shaderDir)
        val logsSize = getDirectorySize(logsDir)
        val tmpSize = getDirectorySize(tmpDir)
        val total = shaderSize + logsSize + tmpSize

        val formatted = if (total >= 1024 * 1024) {
            String.format("%.1f MB", total / (1024.0 * 1024.0))
        } else {
            String.format("%.0f KB", total / 1024.0)
        }

        return CacheDetails(
            shaderCacheSizeBytes = shaderSize,
            temporaryFilesSizeBytes = tmpSize,
            logsSizeBytes = logsSize,
            formattedTotalSize = formatted
        )
    }

    fun clearGameCache(context: Context, gameId: String): Boolean {
        return try {
            val profileDir = getProfileDirectory(context, gameId)
            val shaderDir = File(profileDir, "shader_cache")
            val logsDir = File(profileDir, "logs")
            val tmpDir = File(profileDir, "tmp")

            shaderDir.deleteRecursively()
            logsDir.deleteRecursively()
            tmpDir.deleteRecursively()

            shaderDir.mkdirs()
            logsDir.mkdirs()
            tmpDir.mkdirs()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) getDirectorySize(f) else f.length()
        }
        return size
    }
}
