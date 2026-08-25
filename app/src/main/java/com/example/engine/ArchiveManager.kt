package com.example.engine

import android.content.Context
import android.net.Uri
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ExtractionProgress(
    val percentage: Int = 0,
    val currentFile: String = "",
    val extractedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedMbPerSec: Float = 0f,
    val remainingTimeSeconds: Int = 0,
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

data class StorageCheckResult(
    val hasEnoughSpace: Boolean,
    val requiredBytes: Long,
    val availableBytes: Long,
    val requiredFormatted: String,
    val availableFormatted: String
)

object ArchiveManager {

    fun checkStorageAvailable(context: Context, archiveSizeBytes: Long): StorageCheckResult {
        val targetDir = context.filesDir
        val statFs = StatFs(targetDir.path)
        val availableBytes = statFs.availableBlocksLong * statFs.blockSizeLong

        // Safety margin: 1.15x archive size + 250MB for cache and shader compiler
        val requiredBytes = (archiveSizeBytes * 1.15).toLong() + (250L * 1024 * 1024)

        return StorageCheckResult(
            hasEnoughSpace = availableBytes >= requiredBytes,
            requiredBytes = requiredBytes,
            availableBytes = availableBytes,
            requiredFormatted = formatBytes(requiredBytes),
            availableFormatted = formatBytes(availableBytes)
        )
    }

    suspend fun extractZip(
        context: Context,
        zipUri: Uri,
        destDirectory: File,
        estimatedTotalBytes: Long,
        onProgress: (ExtractionProgress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val canonicalDest = destDirectory.canonicalPath
        if (!destDirectory.exists()) {
            destDirectory.mkdirs()
        }

        var totalRead = 0L
        val startTime = System.currentTimeMillis()
        var lastUpdate = 0L

        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(zipUri)
                ?: return@withContext Result.failure(Exception("Cannot open game archive file"))

            ZipInputStream(inputStream.buffered(64 * 1024)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(32 * 1024)

                while (entry != null) {
                    ensureActive()
                    val entryName = entry.name

                    // Security: Path Traversal Protection
                    val targetFile = File(destDirectory, entryName)
                    val canonicalTarget = targetFile.canonicalPath
                    if (!canonicalTarget.startsWith(canonicalDest + File.separator) && canonicalTarget != canonicalDest) {
                        throw SecurityException("Security warning: Archive entry $entryName attempts path traversal!")
                    }

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                ensureActive()
                                fos.write(buffer, 0, len)
                                totalRead += len

                                val now = System.currentTimeMillis()
                                if (now - lastUpdate > 150) {
                                    lastUpdate = now
                                    val elapsedSec = ((now - startTime) / 1000f).coerceAtLeast(0.1f)
                                    val speed = (totalRead / (1024f * 1024f)) / elapsedSec
                                    val effectiveTotal = if (estimatedTotalBytes > 0) estimatedTotalBytes else (totalRead * 1.5).toLong()
                                    val percent = ((totalRead.toDouble() / effectiveTotal.toDouble()) * 100).toInt().coerceIn(0, 99)
                                    val remainingBytes = (effectiveTotal - totalRead).coerceAtLeast(0)
                                    val remainingSec = if (speed > 0) ((remainingBytes / (1024 * 1024)) / speed).toInt() else 0

                                    onProgress(
                                        ExtractionProgress(
                                            percentage = percent,
                                            currentFile = targetFile.name,
                                            extractedBytes = totalRead,
                                            totalBytes = effectiveTotal,
                                            speedMbPerSec = speed,
                                            remainingTimeSeconds = remainingSec
                                        )
                                    )
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            onProgress(
                ExtractionProgress(
                    percentage = 100,
                    currentFile = "Complete",
                    extractedBytes = totalRead,
                    totalBytes = totalRead,
                    speedMbPerSec = 0f,
                    remainingTimeSeconds = 0,
                    isComplete = true
                )
            )

            Result.success(destDirectory)
        } catch (e: Exception) {
            // Clean up partial incomplete files on failure
            destDirectory.deleteRecursively()
            onProgress(
                ExtractionProgress(
                    percentage = 0,
                    currentFile = "Error",
                    errorMessage = e.localizedMessage ?: "Extraction failed"
                )
            )
            Result.failure(e)
        }
    }

    suspend fun createSampleDemoGame(context: Context, destDir: File): File = withContext(Dispatchers.IO) {
        if (!destDir.exists()) destDir.mkdirs()

        // Create realistic structure for DirectX 9 / Unity indie PC game
        val exeFile = File(destDir, "SpeedRacer2026.exe")
        val dummyBytes = ByteArray(1024)
        dummyBytes[0] = 0x4D.toByte() // 'M'
        dummyBytes[1] = 0x5A.toByte() // 'Z'
        dummyBytes[0x3C] = 0x80.toByte()
        dummyBytes[0x80] = 0x50.toByte() // 'P'
        dummyBytes[0x81] = 0x45.toByte() // 'E'
        dummyBytes[0x84] = 0x64.toByte() // 64-bit machine type
        dummyBytes[0x85] = 0x86.toByte()

        exeFile.writeBytes(dummyBytes)

        File(destDir, "d3d11.dll").writeText("// DXVK D3D11 dynamic link library")
        File(destDir, "dxgi.dll").writeText("// DXVK DXGI dynamic link library")
        File(destDir, "UnityPlayer.dll").writeText("// Unity 2022 Runtime Engine")

        val dataDir = File(destDir, "SpeedRacer2026_Data")
        dataDir.mkdirs()
        File(dataDir, "resources.assets").writeBytes(ByteArray(5 * 1024 * 1024))
        File(dataDir, "sharedassets0.assets").writeBytes(ByteArray(2 * 1024 * 1024))

        val shadersDir = File(dataDir, "Shaders")
        shadersDir.mkdirs()
        File(shadersDir, "standard_pbr.hlsl").writeText("// Standard PBR High Shader")
        File(shadersDir, "post_fx.spv").writeBytes(ByteArray(64 * 1024))

        destDir
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) {
            String.format("%.1f GB", gb)
        } else {
            val mb = bytes / (1024.0 * 1024.0)
            String.format("%.0f MB", mb)
        }
    }
}
