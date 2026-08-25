package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val title: String,
    val archivePath: String,
    val installDirectory: String,
    val executableRelativePath: String,
    val sizeBytes: Long,
    val extractedSizeBytes: Long,
    val iconUri: String? = null,
    val detectedEngine: String = "DirectX / Windows PC",
    val detectedGraphicsApi: String = "DirectX 11 (DXVK)",
    val detectedArchitecture: String = "64-bit (x86_64)",
    val genre: GameGenre = GameGenre.DEFAULT,
    val gameLoadScore: GameLoadScore = GameLoadScore.MODERATE,
    val activeProfile: PerformanceProfile = PerformanceProfile.SMOOTH,
    val recommendedProfile: PerformanceProfile = PerformanceProfile.SMOOTH,
    val recommendationReason: String = "Optimal balance for this device and game profile.",
    val isReady: Boolean = true,
    val isSafeMode: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val dateAddedTimestamp: Long = System.currentTimeMillis(),
    val totalPlayTimeSeconds: Long = 0L,
    val lastFpsAchieved: Float = 60.0f,
    val crashCount: Int = 0,
    val lastCrashReason: String? = null,
    // Serialized JSON or simple config representations
    val configJson: String = "",
    val controlLayoutJson: String = "",
    val backupConfigJson: String = ""
) {
    val displaySize: String
        get() {
            val bytes = if (extractedSizeBytes > 0) extractedSizeBytes else sizeBytes
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1.0) {
                String.format("%.1f GB", gb)
            } else {
                val mb = bytes / (1024.0 * 1024.0)
                String.format("%.0f MB", mb)
            }
        }
}
