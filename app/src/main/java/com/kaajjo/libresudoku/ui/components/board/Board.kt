package com.kaajjo.libresudoku.ui.components.board

import android.annotation.SuppressLint
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.Cage
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.ui.theme.BoardColors
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.theme.SudokuBoardColors
import com.kaajjo.libresudoku.ui.theme.SudokuBoardColorsImpl
import com.kaajjo.libresudoku.ui.util.LightDarkPreview
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Sudoku board
 *
 * @param board sudoku game
 * @param size sudoku game size (always []
 * @param notes a list of [Note] for sudoku game
 * @param mainTextSize main numbers text size (used when [autoFontSize] is set to false
 * @param autoFontSize adjust numbers font size according to cell size. Enabling this will override [mainTextSize]
 * @param noteTextSize note number text size
 * @param selectedCell currently selected [Cell]
 * @param onClick returns [Cell] that was clicked
 * @param onLongClick returns [Cell] that was long clicked
 * @param identicalNumbersHighlight highlight cells with the same value as selected cell
 * @param errorsHighlight whether to highlight mistakes
 * @param positionLines whether to highlight row and column of currently selected [Cell]
 * @param enabled
 * @param questions  if enabled, "?" will be shown instead of number in cells (used instead of blur modifier on android < 12)
 * @param renderNotes whether to show notes at all
 * @param cellsToHighlight list of [Cell] to highlight
 * @param notesToHighlight list of [Note] to highlight
 * @param zoomable whether to allow zoom and pan the board
 * @param boardColors colors of the board (see [BoardColors])
 * @param crossHighlight highlight some boxes on the board
 * @param cages a list of [Cage] for killer sudoku
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun Board(
    modifier: Modifier = Modifier,
    board: List<List<Cell>>,
    size: Int = board.size,
    notes: List<Note>? = null,
    mainTextSize: TextUnit = when (size) {
        6 -> 32.sp
        9 -> 26.sp
        12 -> 24.sp
        else -> 14.sp
    },
    autoFontSize: Boolean = false,
    noteTextSize: TextUnit = when (size) {
        6 -> 18.sp
        9 -> 12.sp
        12 -> 7.sp
        else -> 14.sp
    },
    selectedCell: Cell,
    onClick: (Cell) -> Unit,
    onLongClick: (Cell) -> Unit = { },
    identicalNumbersHighlight: Boolean = true,
    errorsHighlight: Boolean = true,
    positionLines: Boolean = true,
    enabled: Boolean = true,
    questions: Boolean = false,
    renderNotes: Boolean = true,
    cellsToHighlight: List<Cell>? = null,
    notesToHighlight: List<Note> = emptyList(),
    zoomable: Boolean = false,
    boardColors: SudokuBoardColors = LocalBoardColors.current,
    crossHighlight: Boolean = false,
    cages: List<Cage> = emptyList()
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(4.dp)
    ) {
        val maxWidth = constraints.maxWidth.toFloat()

        // single cell size
        val cellSize by remember(size) { mutableFloatStateOf(maxWidth / size.toFloat()) }
        // div for notes in one row in cell
        val cellSizeDivWidth by remember(size) { mutableFloatStateOf(cellSize / ceil(sqrt(size.toFloat()))) }
        val highlightedCellInset = cellSize * 0.035f

        val errorColor = boardColors.errorColor
        val foregroundColor = boardColors.foregroundColor
        val thickLineColor = boardColors.thickLineColor
        val thinLineColor = boardColors.thinLineColor
        // locked numbers
        val altForegroundColor = boardColors.altForegroundColor
        val notesColor = boardColors.notesColor

        // highlight (cells)
        val highlightColor = boardColors.highlightColor

        val vertThick by remember(size) { mutableIntStateOf(floor(sqrt(size.toFloat())).toInt()) }
        val horThick by remember(size) { mutableIntStateOf(ceil(sqrt(size.toFloat())).toInt()) }

        var completionAnimationInitialized by remember(size) { mutableStateOf(false) }
        var animatedCompletedGroupKeys by remember(size) { mutableStateOf(setOf<String>()) }
        var previousBoardValuesSnapshot by remember(size) { mutableStateOf<List<Int>?>(null) }
        val animatedCellsScale = remember(size) { mutableStateMapOf<BoardCellCoordinate, Float>() }
        val animationScope = rememberCoroutineScope()
        val boardValuesKey = buildBoardValuesKey(board = board, size = size)

        var fontSizePx by remember { mutableFloatStateOf(1f) }
        with(LocalDensity.current) {
            LaunchedEffect(autoFontSize, size, mainTextSize) {
                fontSizePx = if (autoFontSize) {
                    (cellSize * 0.9f).toSp().toPx()
                } else {
                    mainTextSize.toPx()
                }
            }
        }
        val noteSizePx = with(LocalDensity.current) { (cellSizeDivWidth * 0.8f).toSp().toPx() }
        val killerSumSizePx = with(LocalDensity.current) { noteSizePx * 1.1f }

        val thinLineWidth = with(LocalDensity.current) { 1.3.dp.toPx() }
        val thickLineWidth = with(LocalDensity.current) { 1.3.dp.toPx() }

        val killerSumBounds by remember { mutableStateOf(android.graphics.Rect()) }

        // paints
        // numbers
        var textPaint by remember(fontSizePx) {
            mutableStateOf(
                Paint().apply {
                    color = foregroundColor.toArgb()
                    isAntiAlias = true
                    textSize = fontSizePx
                }
            )
        }
        // errors
        var errorTextPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = errorColor.toArgb()
                    isAntiAlias = true
                    textSize = fontSizePx
                }
            )
        }
        // locked numbers
        var lockedTextPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = altForegroundColor.toArgb()
                    isAntiAlias = true
                    textSize = fontSizePx
                }
            )
        }

        // notes
        var notePaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = notesColor.toArgb()
                    isAntiAlias = true
                    textSize = noteSizePx
                }
            )
        }

        val noteHighlightPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = highlightColor.copy(alpha = 0.3f).toArgb()
                    isAntiAlias = true
                }
            )
        }

        var killerSumPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = notesColor.toArgb()
                    isAntiAlias = true
                    textSize = killerSumSizePx
                }
            )
        }

        LaunchedEffect(mainTextSize, noteTextSize, boardColors) {
            textPaint = Paint().apply {
                color = foregroundColor.toArgb()
                isAntiAlias = true
                textSize = fontSizePx
            }
            notePaint = Paint().apply {
                color = notesColor.toArgb()
                isAntiAlias = true
                textSize = noteSizePx
            }
            errorTextPaint = Paint().apply {
                color = Color(230, 67, 83).toArgb()
                isAntiAlias = true
                textSize = fontSizePx
            }
            lockedTextPaint = Paint().apply {
                color = altForegroundColor.toArgb()
                isAntiAlias = true
                textSize = fontSizePx
            }
            killerSumPaint = Paint().apply {
                color = notesColor.toArgb()
                isAntiAlias = true
                textSize = killerSumSizePx
            }
        }

        LaunchedEffect(boardValuesKey, size) {
            val completedGroups = getCompletedBoardGroups(board = board, size = size)
            val completedGroupKeys = completedGroups.map { it.key }.toSet()
            val currentBoardValues = buildBoardValuesSnapshot(board = board, size = size)

            if (!completionAnimationInitialized) {
                completionAnimationInitialized = true
                animatedCompletedGroupKeys = completedGroupKeys
                previousBoardValuesSnapshot = currentBoardValues
                return@LaunchedEffect
            }

            val newlyCompletedGroups = completedGroups.filter { group ->
                !animatedCompletedGroupKeys.contains(group.key)
            }

            val changedValuesCount = countChangedBoardValues(
                previousValues = previousBoardValuesSnapshot,
                currentValues = currentBoardValues
            )
            val shouldAnimate = changedValuesCount == 1 && newlyCompletedGroups.isNotEmpty()

            animatedCompletedGroupKeys = completedGroupKeys
            previousBoardValuesSnapshot = currentBoardValues

            if (shouldAnimate) {
                animationScope.launch {
                    animateCompletedGroups(
                        groups = newlyCompletedGroups,
                        animatedCellsScale = animatedCellsScale
                    )
                }
            }
        }

        var zoom by remember(enabled) { mutableFloatStateOf(1f) }
        var offset by remember(enabled) { mutableStateOf(Offset.Zero) }

        val boardModifier = Modifier
            .fillMaxSize()
            .pointerInput(key1 = enabled, key2 = board) {
                detectTapGestures(
                    onTap = {
                        val totalOffset = it / zoom + offset
                        val row =
                            floor((totalOffset.y) / cellSize)
                                .toInt()
                                .coerceIn(board.indices)
                        val column =
                            floor((totalOffset.x) / cellSize)
                                .toInt()
                                .coerceIn(board.indices)
                        onClick(board[row][column])
                    },
                    onLongPress = {
                        if (enabled) {
                            val totalOffset = it / zoom + offset
                            val row = floor((totalOffset.y) / cellSize).toInt()
                            val column = floor((totalOffset.x) / cellSize).toInt()
                            onLongClick(board[row][column])
                        }
                    }
                )
            }

        val zoomModifier = Modifier
            .pointerInput(enabled) {
                detectTransformGestures(
                    onGesture = { gestureCentroid, gesturePan, gestureZoom, _ ->
                        if (enabled) {
                            val oldScale = zoom
                            val newScale = (zoom * gestureZoom).coerceIn(1f..3f)

                            offset = (offset + gestureCentroid / oldScale) -
                                    (gestureCentroid / newScale + gesturePan / oldScale)

                            zoom = newScale
                            if (offset.x < 0) {
                                offset = Offset(0f, offset.y)
                            } else if (maxWidth - offset.x < maxWidth / zoom) {
                                offset = offset.copy(x = maxWidth - maxWidth / zoom)
                            }
                            if (offset.y < 0) {
                                offset = Offset(offset.x, 0f)
                            } else if (maxWidth - offset.y < maxWidth / zoom) {
                                offset = offset.copy(y = maxWidth - maxWidth / zoom)
                            }
                        }
                    }
                )
            }
            .graphicsLayer {
                translationX = -offset.x * zoom
                translationY = -offset.y * zoom
                scaleX = zoom
                scaleY = zoom
                TransformOrigin(0f, 0f).also { transformOrigin = it }
            }
        Canvas(
            modifier = if (zoomable) boardModifier.then(zoomModifier) else boardModifier
        ) {
            val cornerRadius = CornerRadius(15f, 15f)

            if (selectedCell.row >= 0 && selectedCell.col >= 0) {
                // current cell
                drawRoundCell(
                    row = selectedCell.row,
                    col = selectedCell.col,
                    gameSize = size,
                    rect = Rect(
                        offset = Offset(
                            x = selectedCell.col * cellSize + highlightedCellInset,
                            y = selectedCell.row * cellSize + highlightedCellInset
                        ),
                        size = Size(
                            cellSize - highlightedCellInset * 2f,
                            cellSize - highlightedCellInset * 2f
                        )
                    ),
                    color = highlightColor.copy(alpha = 0.2f),
                    cornerRadius = cornerRadius
                )
                if (positionLines) {
                    drawPositionLines(
                        row = selectedCell.row,
                        col = selectedCell.col,
                        gameSize = size,
                        color = highlightColor.copy(alpha = 0.1f),
                        cellSize = cellSize,
                        lineLength = maxWidth,
                        cornerRadius = cornerRadius
                    )
                }
            }
            if (identicalNumbersHighlight) {
                for (i in 0 until size) {
                    for (j in 0 until size) {
                        if (board[i][j].value == selectedCell.value && board[i][j].value != 0) {
                            drawRoundCell(
                                row = board[i][j].row,
                                col = board[i][j].col,
                                gameSize = size,
                                rect = Rect(
                                    offset = Offset(
                                        x = board[i][j].col * cellSize + highlightedCellInset,
                                        y = board[i][j].row * cellSize + highlightedCellInset
                                    ),
                                    size = Size(
                                        cellSize - highlightedCellInset * 2f,
                                        cellSize - highlightedCellInset * 2f
                                    )
                                ),
                                color = highlightColor.copy(alpha = 0.2f),
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                }
            }
            cellsToHighlight?.forEach {
                drawRoundCell(
                    row = it.row,
                    col = it.col,
                    gameSize = size,
                    color = highlightColor.copy(alpha = 0.3f),
                    rect = Rect(
                        Offset(
                            x = it.col * cellSize + highlightedCellInset,
                            y = it.row * cellSize + highlightedCellInset
                        ),
                        size = Size(
                            cellSize - highlightedCellInset * 2f,
                            cellSize - highlightedCellInset * 2f
                        )
                    ),
                    cornerRadius = cornerRadius
                )
            }

            drawBoardFrame(
                thickLineColor = thickLineColor,
                thickLineWidth = thickLineWidth,
                maxWidth = maxWidth,
                cornerRadius = CornerRadius(15f, 15f)
            )

            // horizontal line
            for (i in 1 until size) {
                val isThickLine = i % horThick == 0
                drawLine(
                    color = if (isThickLine) thickLineColor else thinLineColor,
                    start = Offset(cellSize * i.toFloat(), 0f),
                    end = Offset(cellSize * i.toFloat(), maxWidth),
                    strokeWidth = if (isThickLine) thickLineWidth else thinLineWidth
                )
            }
            // vertical line
            for (i in 1 until size) {
                val isThickLine = i % vertThick == 0
                if (maxWidth >= cellSize * i) {
                    drawLine(
                        color = if (isThickLine) thickLineColor else thinLineColor,
                        start = Offset(0f, cellSize * i.toFloat()),
                        end = Offset(maxWidth, cellSize * i.toFloat()),
                        strokeWidth = if (isThickLine) thickLineWidth else thinLineWidth
                    )
                }
            }

            drawCompletedCellsAnimation(
                animatedCellsScale = animatedCellsScale,
                cellSize = cellSize,
                gameSize = size,
                color = highlightColor.copy(alpha = 0.35f),
                cornerRadius = cornerRadius,
                cellInset = highlightedCellInset
            )

            drawNumbers(
                size = size,
                board = board,
                highlightErrors = errorsHighlight,
                errorTextPaint = errorTextPaint,
                lockedTextPaint = lockedTextPaint,
                textPaint = textPaint,
                questions = questions,
                cellSize = cellSize,
                cellScaleProvider = { row, col ->
                    animatedCellsScale[BoardCellCoordinate(row = row, col = col)] ?: 1f
                }
            )

            if (!notes.isNullOrEmpty() && !questions && renderNotes) {
                drawNotes(
                    size = size,
                    paint = notePaint,
                    highlightPaint = noteHighlightPaint,
                    notes = notes,
                    notesToHighlight = notesToHighlight,
                    cellSize = cellSize,
                    cellSizeDivWidth = cellSizeDivWidth,
                    killerSumBounds = killerSumBounds
                )
            }

            // doesn't look good on 6x6
            if (crossHighlight && size != 6) {
                drawCrossSelection(
                    gameSize = size,
                    sectionWidth = getSectionWidthForSize(size),
                    sectionHeight = getSectionHeightForSize(size),
                    color = highlightColor.copy(alpha = 0.1f),
                    cellSize = cellSize
                )
            }

            if (cages.isNotEmpty()) {
                cages.forEach { cage ->
                    val textToDraw = if (questions) "?" else cage.sum.toString()
                    killerSumPaint.getTextBounds(textToDraw, 0, textToDraw.length, killerSumBounds)
                    val cellWithSum = cage.cells.first()
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            textToDraw,
                            cellWithSum.col * cellSize + (cellSize * 0.05f),
                            cellWithSum.row * cellSize + killerSumBounds.height() + (cellSize * 0.05f),
                            killerSumPaint
                        )
                    }

                    drawKillerCage(
                        cage = cage,
                        cellWithSum = cellWithSum,
                        cellSize = cellSize,
                        strokeWidth = thickLineWidth,
                        color = thinLineColor,
                        cornerTextPadding = Offset(
                            killerSumBounds.width().toFloat(),
                            killerSumBounds.height().toFloat()
                        )
                    )
                }
            }
        }
    }
}

private const val CompletedCellScale = 1.3f
private const val CompletedCellScaleUpDurationMs = 120
private const val CompletedCellScaleDownDurationMs = 135
private const val CompletedCellStepDelayMs = 20L
private const val CompletedGroupDelayMs = 40L

private fun buildBoardValuesKey(
    board: List<List<Cell>>,
    size: Int
): String {
    return buildString(capacity = size * size * 2) {
        for (row in 0 until size) {
            for (col in 0 until size) {
                append(board[row][col].value)
                append(',')
            }
        }
    }
}

private fun buildBoardValuesSnapshot(
    board: List<List<Cell>>,
    size: Int
): List<Int> {
    val values = ArrayList<Int>(size * size)

    for (row in 0 until size) {
        for (col in 0 until size) {
            values.add(board[row][col].value)
        }
    }

    return values
}

private fun countChangedBoardValues(
    previousValues: List<Int>?,
    currentValues: List<Int>
): Int {
    if (previousValues == null || previousValues.size != currentValues.size) {
        return Int.MAX_VALUE
    }

    var changedCount = 0
    for (index in previousValues.indices) {
        if (previousValues[index] != currentValues[index]) {
            changedCount++
        }
    }

    return changedCount
}

private suspend fun animateCompletedGroups(
    groups: List<CompletedBoardGroup>,
    animatedCellsScale: SnapshotStateMap<BoardCellCoordinate, Float>
) {
    groups.forEach { group ->
        coroutineScope {
            group.cells.forEachIndexed { index, cell ->
                launch {
                    delay(index * CompletedCellStepDelayMs)
                    animateCompletedCell(
                        cell = cell,
                        animatedCellsScale = animatedCellsScale
                    )
                }
            }
        }
        delay(CompletedGroupDelayMs)
    }
}

private suspend fun animateCompletedCell(
    cell: BoardCellCoordinate,
    animatedCellsScale: SnapshotStateMap<BoardCellCoordinate, Float>
) {
    val scaleAnimatable = Animatable(initialValue = 1f)
    try {
        scaleAnimatable.animateTo(
            targetValue = CompletedCellScale,
            animationSpec = tween(
                durationMillis = CompletedCellScaleUpDurationMs,
                easing = FastOutSlowInEasing
            )
        ) {
            animatedCellsScale[cell] = value
        }

        scaleAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = CompletedCellScaleDownDurationMs,
                easing = FastOutSlowInEasing
            )
        ) {
            animatedCellsScale[cell] = value
        }
    } finally {
        animatedCellsScale.remove(cell)
    }
}


@LightDarkPreview
@Composable
private fun BoardPreviewLight() {
    LibreSudokuTheme {
        Surface {
            val sudokuParser = SudokuParser()
            val board by remember {
                mutableStateOf(
                    sudokuParser.parseBoard(
                        board = "....1........4.............7...........9........68...............5...............",
                        gameType = GameType.Default9x9,
                        emptySeparator = '.'
                    ).toList()
                )
            }
            val notes = listOf(Note(2, 3, 1), Note(2, 3, 5))
            Board(
                board = board,
                notes = notes,
                selectedCell = Cell(-1, -1),
                onClick = { },
                boardColors = SudokuBoardColorsImpl(
                    foregroundColor = BoardColors.foregroundColor,
                    notesColor = BoardColors.notesColor,
                    altForegroundColor = BoardColors.altForegroundColor,
                    errorColor = BoardColors.errorColor,
                    highlightColor = BoardColors.highlightColor,
                    thickLineColor = BoardColors.thickLineColor,
                    thinLineColor = BoardColors.thinLineColor
                )
            )
        }
    }
}
