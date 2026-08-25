package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.GameDao
import com.example.data.model.GameEntity
import com.example.data.model.PerformanceProfile
import kotlinx.coroutines.flow.Flow
import java.io.File

class GameRepository(
    private val gameDao: GameDao,
    private val context: Context
) {
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()
    val recentlyPlayed: Flow<List<GameEntity>> = gameDao.getRecentlyPlayedGames()

    suspend fun getGameById(id: String): GameEntity? = gameDao.getGameById(id)

    fun getGameByIdFlow(id: String): Flow<GameEntity?> = gameDao.getGameByIdFlow(id)

    suspend fun saveGame(game: GameEntity) {
        gameDao.insertGame(game)
    }

    suspend fun updateGame(game: GameEntity) {
        gameDao.updateGame(game)
    }

    suspend fun updateProfile(gameId: String, profile: PerformanceProfile) {
        gameDao.updateProfile(gameId, profile.name)
    }

    suspend fun recordGameSession(gameId: String, durationSeconds: Long) {
        gameDao.updateLastPlayed(gameId, System.currentTimeMillis(), durationSeconds)
    }

    suspend fun recordCrash(gameId: String, reason: String) {
        gameDao.recordCrash(gameId, reason)
    }

    suspend fun setSafeMode(gameId: String, safeMode: Boolean) {
        gameDao.setSafeMode(gameId, safeMode)
    }

    suspend fun updateControlLayout(gameId: String, layoutJson: String) {
        gameDao.updateControlLayout(gameId, layoutJson)
    }

    suspend fun updateConfig(gameId: String, configJson: String) {
        gameDao.updateConfig(gameId, configJson)
    }

    suspend fun deleteGame(game: GameEntity) {
        // Remove game files and isolated profile
        try {
            if (game.installDirectory.isNotEmpty()) {
                val dir = File(game.installDirectory)
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            }
            val profileDir = File(context.filesDir, "game_profiles/${game.id}")
            if (profileDir.exists()) {
                profileDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        gameDao.deleteGame(game)
    }

    suspend fun getGameCount(): Int = gameDao.getGameCount()

    companion object {
        @Volatile
        private var INSTANCE: GameRepository? = null

        fun getInstance(context: Context): GameRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val instance = GameRepository(db.gameDao(), context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
