package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY lastPlayedTimestamp DESC, dateAddedTimestamp DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: String): GameEntity?

    @Query("SELECT * FROM games WHERE id = :id")
    fun getGameByIdFlow(id: String): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 5")
    fun getRecentlyPlayedGames(): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGameById(id: String)

    @Query("UPDATE games SET lastPlayedTimestamp = :timestamp, totalPlayTimeSeconds = totalPlayTimeSeconds + :durationSeconds WHERE id = :id")
    suspend fun updateLastPlayed(id: String, timestamp: Long, durationSeconds: Long)

    @Query("UPDATE games SET activeProfile = :profile WHERE id = :id")
    suspend fun updateProfile(id: String, profile: String)

    @Query("UPDATE games SET crashCount = crashCount + 1, lastCrashReason = :reason, isSafeMode = 1 WHERE id = :id")
    suspend fun recordCrash(id: String, reason: String)

    @Query("UPDATE games SET isSafeMode = :safeMode WHERE id = :id")
    suspend fun setSafeMode(id: String, safeMode: Boolean)

    @Query("UPDATE games SET controlLayoutJson = :layoutJson WHERE id = :id")
    suspend fun updateControlLayout(id: String, layoutJson: String)

    @Query("UPDATE games SET configJson = :configJson WHERE id = :id")
    suspend fun updateConfig(id: String, configJson: String)

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getGameCount(): Int
}
