package com.kaajjo.libresudoku.qqwing

import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.QQWing
import com.kaajjo.libresudoku.core.utils.SudokuParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Sudoku16x16Test {

    // note: never call anything with a side effect from kotlin's assert(), it is compiled
    // away when the test jvm runs without -ea
    private fun generate16x16(): QQWing {
        val qqwing = QQWing(GameType.Default16x16, GameDifficulty.Unspecified)
        qqwing.setRecordHistory(true)
        val generated = qqwing.generatePuzzle()
        assertTrue("failed to generate a 16x16 puzzle", generated)
        qqwing.solve()
        return qqwing
    }

    @Test
    fun generated16x16_isValidAndUnique() {
        val qqwing = generate16x16()
        assertTrue("generated 16x16 board is not solvable", qqwing.isSolved())

        // hasUniqueSolution() runs the solver again and overwrites solution, so the
        // solution has to be read before that
        val solution = qqwing.solution.copyOf()
        assertEquals(256, solution.size)
        assertTrue(
            "solution contains values outside 1..16: ${solution.distinct().sorted()}",
            solution.all { it in 1..16 }
        )
        // in a complete 16x16 solution every value appears exactly 16 times
        (1..16).forEach { value ->
            assertEquals("value $value", 16, solution.count { it == value })
        }

        assertTrue("generated 16x16 board has no unique solution", qqwing.hasUniqueSolution())
    }

    @Test
    fun boardWithValuesAbove12_survivesStringRoundTrip() {
        val parser = SudokuParser()
        val solution = generate16x16().solution

        val asString = parser.boardToString(solution)
        // exactly one character per cell, otherwise parseBoard would misread the board
        assertEquals(256, asString.length)

        val parsed = parser.parseBoard(asString, GameType.Default16x16)
        val flattened = parsed.flatten().map { it.value }.toIntArray()
        assertTrue(
            "round trip changed the board",
            solution.contentEquals(flattened)
        )
    }

    @Test
    fun notesWithValuesAbove12_survivesStringRoundTrip() {
        val parser = SudokuParser()
        val notes = listOf(
            Note(row = 0, col = 0, value = 1),
            Note(row = 12, col = 9, value = 13),
            Note(row = 15, col = 15, value = 16)
        )
        val parsed = parser.parseNotes(parser.notesToString(notes))
        assertEquals(notes, parsed)
    }

    @Test
    fun boardsSavedBeforeTheRadixChange_stillParse() {
        // 12x12 board string written by an older version, which serialized with radix 13.
        // characters for 0..12 are identical under radix 17, so it has to parse unchanged
        val parser = SudokuParser()
        val old12x12 =
            "09030000010a00501a0067b910700000050000920000000006407000000250000160300b0000205000000017000003088300b000a006003804090b659000ab007004002400000000"
        val parsed = parser.parseBoard(old12x12, GameType.Default12x12)

        assertEquals(12, parsed.size)
        assertEquals(0, parsed[0][0].value)
        assertEquals(9, parsed[0][1].value)
        // 'a' -> 10 under both the old and the new radix
        assertEquals(10, parsed[0][11].value)
        assertTrue(parsed.flatten().all { it.value in 0..12 })
    }
}
