package com.kaajjo.libresudoku.ui.components.board

import com.kaajjo.libresudoku.core.qqwing.GameType

fun getNoteColumnNumber(number: Int, size: Int): Int {
    if (size == 9 || size == 6) {
        return when (number) {
            1, 2, 3 -> 0
            4, 5, 6 -> 1
            7, 8, 9 -> 2
            else -> 0
        }
    } else if (size == 12) {
        return when (number) {
            1, 2, 3, 4 -> 0
            5, 6, 7, 8 -> 1
            9, 10, 11, 12 -> 2
            else -> 0
        }
    }
    return 0
}

fun getNoteRowNumber(number: Int, size: Int): Int {
    if (size == 9 || size == 6) {
        return when (number) {
            1, 4, 7 -> 0
            2, 5, 8 -> 1
            3, 6, 9 -> 2
            else -> 0
        }
    } else if (size == 12) {
        return when (number) {
            1, 5, 9 -> 0
            2, 6, 10 -> 1
            3, 7, 11 -> 2
            4, 8, 12 -> 3
            else -> 0
        }
    }
    return 0
}

fun getSectionHeightForSize(size: Int): Int {
    return when (size) {
        6 -> GameType.Default6x6.sectionHeight
        9 -> GameType.Default9x9.sectionHeight
        12 -> GameType.Default12x12.sectionHeight
        16 -> GameType.Default16x16.sectionHeight
        else -> GameType.Default9x9.sectionHeight
    }
}

fun getSectionWidthForSize(size: Int): Int {
    return when (size) {
        6 -> GameType.Default6x6.sectionWidth
        9 -> GameType.Default9x9.sectionWidth
        12 -> GameType.Default12x12.sectionWidth
        16 -> GameType.Default16x16.sectionWidth
        else -> GameType.Default9x9.sectionWidth
    }
}