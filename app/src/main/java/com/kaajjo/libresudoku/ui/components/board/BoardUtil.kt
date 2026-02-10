package com.kaajjo.libresudoku.ui.components.board

import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.GameType

data class BoardCellCoordinate(
    val row: Int,
    val col: Int
)

data class CompletedBoardGroup(
    val key: String,
    val cells: List<BoardCellCoordinate>
)

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
        else -> GameType.Default9x9.sectionHeight
    }
}

fun getSectionWidthForSize(size: Int): Int {
    return when (size) {
        6 -> GameType.Default6x6.sectionWidth
        9 -> GameType.Default9x9.sectionWidth
        12 -> GameType.Default12x12.sectionWidth
        else -> GameType.Default9x9.sectionWidth
    }
}

fun getCompletedBoardGroups(
    board: List<List<Cell>>,
    size: Int
): List<CompletedBoardGroup> {
    val completedGroups = mutableListOf<CompletedBoardGroup>()

    for (row in 0 until size) {
        if ((0 until size).all { col -> board[row][col].value in 1..size }) {
            completedGroups.add(
                CompletedBoardGroup(
                    key = "row-$row",
                    cells = (0 until size).map { col -> BoardCellCoordinate(row = row, col = col) }
                )
            )
        }
    }

    for (col in 0 until size) {
        if ((0 until size).all { row -> board[row][col].value in 1..size }) {
            completedGroups.add(
                CompletedBoardGroup(
                    key = "col-$col",
                    cells = (0 until size).map { row -> BoardCellCoordinate(row = row, col = col) }
                )
            )
        }
    }

    val sectionHeight = getSectionHeightForSize(size)
    val sectionWidth = getSectionWidthForSize(size)

    for (sectionRow in 0 until size step sectionHeight) {
        for (sectionCol in 0 until size step sectionWidth) {
            val cells = mutableListOf<BoardCellCoordinate>()
            var completed = true

            for (row in sectionRow until sectionRow + sectionHeight) {
                for (col in sectionCol until sectionCol + sectionWidth) {
                    cells.add(BoardCellCoordinate(row = row, col = col))
                    if (board[row][col].value !in 1..size) {
                        completed = false
                    }
                }
            }

            if (completed) {
                completedGroups.add(
                    CompletedBoardGroup(
                        key = "block-$sectionRow-$sectionCol",
                        cells = cells
                    )
                )
            }
        }
    }

    return completedGroups
}
