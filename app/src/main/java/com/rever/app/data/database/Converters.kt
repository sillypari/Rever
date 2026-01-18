package com.rever.app.data.database

import androidx.room.TypeConverter
import com.rever.app.data.model.Difficulty

class Converters {
    
    @TypeConverter
    fun fromDifficulty(difficulty: Difficulty): String {
        return difficulty.name
    }
    
    @TypeConverter
    fun toDifficulty(value: String): Difficulty {
        return try {
            Difficulty.valueOf(value)
        } catch (e: IllegalArgumentException) {
            Difficulty.MEDIUM
        }
    }
}
