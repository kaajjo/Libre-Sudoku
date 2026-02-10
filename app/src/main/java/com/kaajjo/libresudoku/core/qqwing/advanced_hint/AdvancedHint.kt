package com.kaajjo.libresudoku.core.qqwing.advanced_hint

import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuUtils


/**
 * Implemented:
 * Naked single
 * Hidden single
 * Full House
 * TODO:
 * Hidden subsets (pair, triple, quadruple)
 * Naked subsets (pair, triple, quadruple)
 * Locked candidates (all types)
 * Wings (X, XY, XYZ)
 * Swordfish, Jellyfish
 * Chains and loops
 */

/**
 * **Very experimental** provides a hint
 *
 * @property type type of the game
 * @property board current sudoku board
 * @property solvedBoard solved sudoku board
 * @property notes notes for sudoku board
 * @property settings settings for an advanced hint
 */
class AdvancedHint(
    val type: GameType,
    private val board: List<List<Cell>>,
    private val solvedBoard: List<List<Cell>>,
    private var notes: List<Note> = emptyList(),
    private var settings: AdvancedHintSettings = AdvancedHintSettings()
) {
    init {
        if (notes.isEmpty()) {
            notes = SudokuUtils().computeNotes(board, type)
        }
    }

    private val rows = getRows()
    private val columns = getColumns()
    private val boxes = getBoxes()

    fun getEasiestHint(): AdvancedHintData? {
        val hint: AdvancedHintData? = null
        if (settings.checkWrongValue) checkForWrongValue()?.let { return it }
        if (settings.fullHouse) checkForFullHouse()?.let { return it }
        if (settings.nakedSingle) checkForNakedSingle()?.let { return it }
        if (settings.hiddenSingle) checkForHiddenSingle()?.let { return it }
        return hint
    }

    private fun checkForWrongValue(): AdvancedHintData? {
        for (i in board.indices) {
            for (j in board.indices) {
                if (board[i][j].value != 0 && board[i][j].value != solvedBoard[i][j].value) {
                    return AdvancedHintData(
                        titleRes = R.string.hint_wrong_value_title,
                        textResWithArg = Pair(
                            R.string.hint_wron_value_detail,
                            listOf(
                                board[i][j].value.toString(),
                                cellStringFormat(board[i][j])
                            )
                        ),
                        targetCell = board[i][j],
                        helpCells = emptyList()
                    )
                }
            }
        }
        return null
    }

    private fun checkForNakedSingle(): AdvancedHintData? {
        if (notes.isEmpty()) return null
        val singles = notes.groupBy { Pair(it.row, it.col) }
            .filter { it.value.size == 1 }
            .map { it.value }
            .randomOrNull()

        return if (!singles.isNullOrEmpty()) {
            val nakedSingle = singles.first()
            val cell = solvedBoard[nakedSingle.row][nakedSingle.col]
            return AdvancedHintData(
                titleRes = R.string.hint_naked_single_title,
                textResWithArg = Pair(
                    R.string.hint_naked_single_detail,
                    listOf(
                        cellStringFormat(cell),
                        cell.value.toString()
                    )
                ),
                targetCell = cell,
                helpCells = emptyList()
            )
        } else {
            null
        }
    }
    
    private fun checkForFullHouse(): AdvancedHintData? {
        val entities = listOf(boxes, rows, columns)

        for (entity in entities) {
            val hint = checkEntityForFullHouse(entity)
            if (hint != null) {
                return hint
            }
        }

        return null
    }

    private fun checkEntityForFullHouse(entity: List<List<Cell>>): AdvancedHintData? {
        for (group in entity) {
            if (group.count { it.value != 0 } == type.size - 1) {
                val emptyCell = group.find { it.value == 0 }
                if (emptyCell != null) {
                    val solvedCell = solvedBoard[emptyCell.row][emptyCell.col]
                    return AdvancedHintData(
                        titleRes = R.string.hint_full_house_group_title,
                        textResWithArg = Pair(
                            R.string.hint_full_house_group_detail,
                            listOf(
                                cellStringFormat(emptyCell),
                                solvedCell.value.toString()
                            )
                        ),
                        targetCell = solvedBoard[emptyCell.row][emptyCell.col],
                        helpCells = board.flatten().filter { group.contains(it) }
                    )
                }
            }
        }
        return null
    }

    private fun checkForHiddenSingle(): AdvancedHintData? {
        if (notes.isEmpty()) return null
        var hiddenSingle: Note? = null
        var helpCells: List<Cell>? = null
        val singlesInRow = notes.groupBy { Pair(it.row, it.value) }
            .filter { it.value.size == 1 }
            .map { it.value }
            .randomOrNull()?.first()
        if (singlesInRow != null) {
            hiddenSingle = singlesInRow
            helpCells = rows[singlesInRow.row]
        }
        val singlesInColumn = notes.groupBy { Pair(it.col, it.value) }
            .filter { it.value.size == 1 }
            .map { it.value }
            .randomOrNull()?.first()
        if (singlesInColumn != null) {
            hiddenSingle = singlesInColumn
            helpCells = columns[singlesInColumn.col]
        }
        val singlesInBox = notes.groupBy { Pair(getBoxNumber(it.row, it.col), it.value) }
            .filter { it.value.size == 1 }
            .map { it.value }
            .randomOrNull()?.first()
        if (singlesInBox != null) {
            hiddenSingle = singlesInBox
            helpCells = boxes[getBoxNumber(singlesInBox.row, singlesInBox.col)]
        }
        if (hiddenSingle == null) return null
        val cell = solvedBoard[hiddenSingle.row][hiddenSingle.col]
        return AdvancedHintData(
            titleRes = R.string.hint_hidden_single_title,
            textResWithArg = Pair(
                R.string.hint_hidden_single_detail,
                listOf(
                    cellStringFormat(cell),
                    cell.value.toString()
                )
            ),
            targetCell = cell,
            helpCells = helpCells ?: emptyList()
        )
    }

    private fun getRows(): List<List<Cell>> {
        return board
    }

    private fun getColumns(): List<List<Cell>> {
        val transposedBoard =
            MutableList(type.size) { row -> MutableList(type.size) { col -> Cell(row, col, 0) } }

        for (i in 0 until type.size) {
            for (j in 0 until type.size) {
                transposedBoard[j][i] = board[i][j]
            }
        }
        return transposedBoard.toList()
    }

    private fun getBoxNumber(row: Int, col: Int): Int {
        val sectionRow = row / type.sectionHeight
        val sectionColumn = col / type.sectionWidth
        val sectorsPerRow = type.size / type.sectionWidth
        return sectionRow * sectorsPerRow + sectionColumn
    }

    private fun getBoxes(): List<List<Cell>> {
        val size = type.size
        val sectionWidth = type.sectionWidth
        val sectionHeight = type.sectionHeight

        val boxes = MutableList(sectionWidth * sectionHeight) { mutableListOf<Cell>() }
        for (i in 0 until size) {
            for (j in 0 until size) {
                val boxNumber = getBoxNumber(i, j)
                boxes[boxNumber].add(board[i][j])
            }
        }
        return boxes
    }

    private fun cellStringFormat(cell: Cell) = "r${cell.row + 1}c${cell.col + 1}"
}