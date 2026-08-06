package com.kaajjo.libresudoku.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.Cage
import com.kaajjo.libresudoku.core.qqwing.CageGenerator
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.QQWingController
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.domain.repository.BoardRepository
import com.kaajjo.libresudoku.domain.repository.SavedGameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val appSettingsManager: AppSettingsManager,
    private val boardRepository: BoardRepository,
    private val savedGameRepository: SavedGameRepository
) : ViewModel() {

    val lastSavedGame = savedGameRepository.getLast()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val lastGamesLimit = 5
    val lastGames = savedGameRepository.getLastPlayable(limit = lastGamesLimit)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    var insertedBoardUid = -1L

    private val difficulties = listOf(
        GameDifficulty.Easy,
        GameDifficulty.Moderate,
        GameDifficulty.Hard,
        GameDifficulty.Challenge,
    )

    // QQWing's solving logic is not strong enough to crack a 16x16 board without guessing,
    // so every generated 16x16 board rates as Challenge. Offering any other difficulty
    // would make the generator spin forever looking for a board it can never produce.
    private val challengeOnly = listOf(GameDifficulty.Challenge)

    private fun difficultiesFor(type: GameType) =
        if (type == GameType.Default16x16) challengeOnly else difficulties

    // plain boards by size first, then the killer variants in the same order
    val types = listOf(
        GameType.Default6x6,
        GameType.Default9x9,
        GameType.Default12x12,
        GameType.Default16x16,
        GameType.Killer6x6,
        GameType.Killer9x9,
        GameType.Killer12x12
    )

    val lastSelectedGameDifficultyType = appSettingsManager.lastSelectedGameDifficultyType
    val saveSelectedGameDifficultyType = appSettingsManager.saveSelectedGameDifficultyType

    var selectedType by mutableStateOf(GameType.Default9x9)
        private set
    var selectedDifficulty by mutableStateOf(GameDifficulty.Easy)
        private set

    /** Difficulties that can be generated for the currently selected type */
    val availableDifficulties: List<GameDifficulty>
        get() = difficultiesFor(selectedType)

    fun selectType(type: GameType) {
        selectedType = type
        // the new type may not support the currently selected difficulty
        val available = difficultiesFor(type)
        if (selectedDifficulty !in available) {
            selectedDifficulty = available.first()
        }
    }

    fun selectDifficulty(difficulty: GameDifficulty) {
        if (difficulty in difficultiesFor(selectedType)) {
            selectedDifficulty = difficulty
        }
    }

    /** Restores a previously saved selection, ignoring combinations that cannot be generated */
    fun restoreSelection(difficulty: GameDifficulty, type: GameType) {
        selectType(if (type in types) type else GameType.Default9x9)
        selectDifficulty(difficulty)
    }

    var isGenerating by mutableStateOf(false)
    var isSolving by mutableStateOf(false)
    var readyToPlay by mutableStateOf(false)

    private var puzzle =
        List(selectedType.size) { row -> List(selectedType.size) { col -> Cell(row, col, 0) } }
    private var solvedPuzzle =
        List(selectedType.size) { row -> List(selectedType.size) { col -> Cell(row, col, 0) } }


    fun startGame() {
        isSolving = false
        isGenerating = false

        val gameTypeToGenerate = selectedType
        val gameDifficultyToGenerate = selectedDifficulty
        val size = gameTypeToGenerate.size

        puzzle = List(size) { row -> List(size) { col -> Cell(row, col, 0) } }
        solvedPuzzle = List(size) { row -> List(size) { col -> Cell(row, col, 0) } }

        viewModelScope.launch(Dispatchers.Default) {
            val saveSelectedGameDifficultyAndType = runBlocking { appSettingsManager.saveSelectedGameDifficultyType.first() }
            if (saveSelectedGameDifficultyAndType) {
                appSettingsManager.setLastSelectedGameDifficultyType(
                    difficulty = selectedDifficulty,
                    type = selectedType
                )
            }

            val qqWingController = QQWingController()

            // generating
            isGenerating = true
            val generated = qqWingController.generate(gameTypeToGenerate, gameDifficultyToGenerate)
            isGenerating = false

            isSolving = true
            val solved = qqWingController.solve(generated, gameTypeToGenerate)
            isSolving = false


            if (!qqWingController.isImpossible && qqWingController.solutionCount == 1) {
                for (i in 0 until size) {
                    for (j in 0 until size) {
                        puzzle[i][j].value = generated[i * size + j]
                        solvedPuzzle[i][j].value = solved[i * size + j]
                    }
                }

                var cages: List<Cage>? = null
                if (gameTypeToGenerate in setOf(
                        GameType.Killer9x9,
                        GameType.Killer12x12,
                        GameType.Killer6x6
                    )
                ) {
                    val generator = CageGenerator(solvedPuzzle, gameTypeToGenerate)
                    cages = generator.generate(2, 5)
                }
                withContext(Dispatchers.IO) {
                    val sudokuParser = SudokuParser()
                    insertedBoardUid = boardRepository.insert(
                        SudokuBoard(
                            uid = 0,
                            initialBoard = sudokuParser.boardToString(puzzle),
                            solvedBoard = sudokuParser.boardToString(solvedPuzzle),
                            difficulty = selectedDifficulty,
                            type = selectedType,
                            killerCages = if (cages != null) sudokuParser.killerSudokuCagesToString(
                                cages
                            ) else null
                        )
                    )
                }

                readyToPlay = true
            }
        }
    }

    fun giveUpLastGame() {
        viewModelScope.launch(Dispatchers.IO) {
            lastSavedGame.value?.let {
                if (!it.completed) {
                    savedGameRepository.update(
                        it.copy(
                            completed = true,
                            canContinue = true
                        )
                    )
                }
            }
        }
    }
}