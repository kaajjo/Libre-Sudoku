package com.kaajjo.libresudoku.ui.learn.learnsudoku

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.board.Board
import com.kaajjo.libresudoku.ui.learn.components.TutorialBase
import com.kaajjo.libresudoku.ui.learn.components.TutorialBottomContent
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination(style = AnimatedNavigation::class)
@Composable
fun LearnBasic(
    navigator: DestinationsNavigator
) {
    TutorialBase(
        title = stringResource(R.string.learn_basic_title),
        navigator = navigator
    ) {
        val sudokuParser = SudokuParser()
        var board by remember {
            mutableStateOf(
                sudokuParser.parseBoard(
                    board = "2...7..38.....6.7.3...4.6....8.2.7..1.......6..7.3.4....4.8...9.6.4.....91..6...2",
                    gameType = GameType.Default9x9,
                    emptySeparator = '.'
                ).toList()
            )
        }
        val steps = listOf(
            stringResource(R.string.learn_basic_1),
            stringResource(R.string.learn_basic_2),
            stringResource(R.string.learn_basic_3),
            stringResource(R.string.learn_basic_4),
            stringResource(R.string.learn_basic_5),
            stringResource(R.string.learn_basic_6),
        )
        val stepsCell = listOf(
            listOf(
                Cell(6, 0),
                Cell(6, 1),
                Cell(6, 2),
                Cell(7, 0),
                Cell(7, 1),
                Cell(7, 2),
                Cell(8, 0),
                Cell(8, 1),
                Cell(8, 2),
            ),
            listOf(Cell(3, 2), Cell(7, 2), Cell(8, 2)),
            listOf(Cell(6, 4), Cell(6, 0), Cell(6, 1)),
            listOf(Cell(7, 0)),
            listOf(
                Cell(6, 2),
                Cell(2, 4),
                Cell(5, 6),
                Cell(0, 2),
                Cell(0, 3),
                Cell(0, 5),
                Cell(0, 6),
            ),
            listOf(Cell(0, 1))
        )
        var step by remember { mutableIntStateOf(0) }
        LaunchedEffect(key1 = step) {
            when (step) {
                0 -> board = sudokuParser.parseBoard(
                    "2...7..38.....6.7.3...4.6....8.2.7..1.......6..7.3.4....4.8...9.6.4.....91..6...2",
                    GameType.Default9x9,
                    emptySeparator = '.'
                )

                3 -> board = sudokuParser.parseBoard(
                    "2...7..38.....6.7.3...4.6....8.2.7..1.......6..7.3.4....4.8...986.4.....91..6...2",
                    GameType.Default9x9,
                    emptySeparator = '.'
                )

                5 -> board = sudokuParser.parseBoard(
                    "24..7..38.....6.7.3...4.6....8.2.7..1.......6..7.3.4....4.8...986.4.....91..6...2",
                    GameType.Default9x9,
                    emptySeparator = '.'
                )
            }
        }

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Board(
                    board = board,
                    cellsToHighlight = if (step < stepsCell.size) stepsCell[step] else null,
                    onClick = { },
                    selectedCell = Cell(-1, -1)
                )
                TutorialBottomContent(
                    steps = steps,
                    step = step,
                    onPreviousClick = { if (step > 0) step-- },
                    onNextClick = { if (step < (steps.size - 1)) step++ },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Board(
                    board = board,
                    cellsToHighlight = if (step < stepsCell.size) stepsCell[step] else null,
                    onClick = { },
                    selectedCell = Cell(-1, -1)
                )
                TutorialBottomContent(
                    steps = steps,
                    step = step,
                    onPreviousClick = { if (step > 0) step-- },
                    onNextClick = { if (step < (steps.size - 1)) step++ }
                )
            }
        }
    }
}

