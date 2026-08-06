package com.kaajjo.libresudoku.data.database.converters

import androidx.room.TypeConverter
import com.kaajjo.libresudoku.core.qqwing.GameType

/**
 * Converts GameType
 */
class GameTypeConverter {
    @TypeConverter
    fun fromType(gameType: GameType): Int {
        return when (gameType) {
            GameType.Unspecified -> 0
            GameType.Default9x9 -> 1
            GameType.Default12x12 -> 2
            GameType.Default6x6 -> 3
            GameType.Killer9x9 -> 4
            GameType.Killer12x12 -> 5
            GameType.Killer6x6 -> 6
            GameType.Default16x16 -> 7
        }
    }

    @TypeConverter
    fun toType(value: Int): GameType {
        return when (value) {
            0 -> GameType.Unspecified
            1 -> GameType.Default9x9
            2 -> GameType.Default12x12
            3 -> GameType.Default6x6
            4 -> GameType.Killer9x9
            5 -> GameType.Killer12x12
            6 -> GameType.Killer6x6
            7 -> GameType.Default16x16
            else -> GameType.Unspecified
        }
    }
}