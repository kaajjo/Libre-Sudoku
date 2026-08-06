package com.kaajjo.libresudoku.core.qqwing

import com.kaajjo.libresudoku.R

enum class GameType(
    val size: Int,
    val sectionHeight: Int,
    val sectionWidth: Int,
    val resName: Int
) {
    Unspecified(1, 1, 1, R.string.type_unspecified),
    Default9x9(9, 3, 3, R.string.type_default_9x9),
    Default12x12(12, 3, 4, R.string.type_default_12x12),
    Default6x6(6, 2, 3, R.string.type_default_6x6),
    Default16x16(16, 4, 4, R.string.type_default_16x16),
    Killer9x9(9, 3, 3, R.string.type_killer_9x9),
    Killer12x12(12, 3, 4, R.string.type_killer_12x12),
    Killer6x6(6, 2, 3, R.string.type_killer_6x6),
}