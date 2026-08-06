package com.kaajjo.libresudoku.core.utils

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.GameType

class SudokuUtils {

    // returns range of row indexes in region of give cell
    fun getBoxRowRange(cell: Cell, sectionHeight: Int): IntRange {
        return cell.row - cell.row % sectionHeight until (cell.row - cell.row % sectionHeight) + sectionHeight
    }

    // returns range of col indexes in region of give cell
    fun getBoxColRange(cell: Cell, sectionWidth: Int): IntRange {
        return cell.col - cell.col % sectionWidth until (cell.col - cell.col % sectionWidth) + sectionWidth
    }


    // returns candidates for given cell
    fun getCandidates(
        board: List<List<Cell>>,
        cell: Cell,
        type: GameType
    ): List<Int> {
        var candidates = List(type.size) { index -> index + 1 }

        for (i in getBoxRowRange(cell, type.sectionHeight)) {
            for (j in getBoxColRange(cell, type.sectionWidth)) {
                if (board[i][j].value != 0 && (i != cell.row || j != cell.col)) {
                    candidates = candidates.minus(board[i][j].value)
                }
            }
        }

        for (i in 0 until type.size) {
            if (board[i][cell.col].value != 0 && i != cell.row) {
                candidates = candidates.minus(board[i][cell.col].value)
            }
            if (board[cell.row][i].value != 0 && i != cell.col) {
                candidates = candidates.minus(board[cell.row][i].value)
            }
        }

        return candidates
    }

    // returns if given cell not violating sudoku rules
    fun isValidCellDynamic(
        board: List<List<Cell>>,
        cell: Cell,
        type: GameType
    ): Boolean {
        val sudokuUtils = SudokuUtils()
        for (i in sudokuUtils.getBoxRowRange(cell, type.sectionHeight)) {
            for (j in sudokuUtils.getBoxColRange(cell, type.sectionWidth)) {
                if (board[i][j].value != 0 && board[i][j].value == cell.value &&
                    (i != cell.row || j != cell.col)
                ) {
                    return false
                }
            }
        }

        for (i in 0 until type.size) {
            if ((board[i][cell.col].value == cell.value && i != cell.row) ||
                (board[cell.row][i].value == cell.value && i != cell.col)
            ) {
                return false
            }
        }
        return true
    }

    // returns count of given number on board
    fun countNumberInBoard(board: List<List<Cell>>, number: Int): Int {
        var count = 0
        board.forEach { cells ->
            cells.forEach {
                if (it.value == number) {
                    count++
                }
            }
        }
        return count
    }

    // compute all candidates for empty cells and returns them as notes
    fun computeNotes(board: List<List<Cell>>, type: GameType): List<Note> {
        var notes = emptyList<Note>()
        board.forEach { cells ->
            cells.forEach { cell ->
                if (cell.value == 0) {
                    getCandidates(board, cell, type).forEach {
                        notes = notes.plus(Note(cell.row, cell.col, it))
                    }
                }
            }
        }
        return notes
    }

    fun autoEraseNotes(
        board: List<List<Cell>>,
        notes: List<Note>,
        cell: Cell,
        type: GameType
    ): List<Note> {
        var newNotes = notes

        for (i in getBoxRowRange(cell, type.sectionHeight)) {
            for (j in getBoxColRange(cell, type.sectionWidth)) {
                if (board[i][j].value == 0 && newNotes.contains(Note(i, j, cell.value))) {
                    newNotes = newNotes.minus(Note(i, j, cell.value))
                }
            }
        }
        for (i in 0 until type.size) {
            if (board[i][cell.col].value == 0 && newNotes.contains(Note(i, cell.col, cell.value))) {
                newNotes = newNotes.minus(Note(i, cell.col, cell.value))
            }
            if (board[cell.row][i].value == 0 && newNotes.contains(Note(cell.row, i, cell.value))) {
                newNotes = newNotes.minus(Note(cell.row, i, cell.value))
            }
        }
        return newNotes
    }

    // factor: 1 - small, 2 - medium, 3 - big
    fun getFontSize(type: GameType, factor: Int): TextUnit {
        return when (type) {
            GameType.Unspecified -> {
                when (factor) {
                    2 -> 26.sp
                    3 -> 34.sp
                    else -> 22.sp
                }
            }

            GameType.Default9x9, GameType.Killer9x9 -> {
                when (factor) {
                    2 -> 28.sp
                    3 -> 36.sp
                    else -> 22.sp
                }
            }

            GameType.Default12x12, GameType.Killer12x12 -> {
                when (factor) {
                    2 -> 24.sp
                    3 -> 32.sp
                    else -> 18.sp
                }
            }

            GameType.Default6x6, GameType.Killer6x6 -> {
                when (factor) {
                    2 -> 34.sp
                    3 -> 40.sp
                    else -> 24.sp
                }
            }

            GameType.Default16x16 -> {
                when (factor) {
                    2 -> 20.sp
                    3 -> 26.sp
                    else -> 14.sp
                }
            }
        }
    }
}