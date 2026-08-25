package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.GameGenre
import com.example.data.model.GameLoadScore
import com.example.data.model.PerformanceProfile

class Converters {
    @TypeConverter
    fun fromGenre(genre: GameGenre?): String = genre?.name ?: GameGenre.DEFAULT.name

    @TypeConverter
    fun toGenre(value: String?): GameGenre = GameGenre.fromString(value)

    @TypeConverter
    fun fromLoadScore(score: GameLoadScore?): String = score?.name ?: GameLoadScore.MODERATE.name

    @TypeConverter
    fun toLoadScore(value: String?): GameLoadScore {
        return try {
            if (value != null) GameLoadScore.valueOf(value) else GameLoadScore.MODERATE
        } catch (e: Exception) {
            GameLoadScore.MODERATE
        }
    }

    @TypeConverter
    fun fromProfile(profile: PerformanceProfile?): String = profile?.name ?: PerformanceProfile.BALANCE.name

    @TypeConverter
    fun toProfile(value: String?): PerformanceProfile = PerformanceProfile.fromName(value)
}
