package com.kaajjo.libresudoku.ui.components.board

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalContext
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
            .aspectRatio(1f)
            .padding(4.dp)
    ) {
        val boxWithConstraintsScope = this

        var maxWidth by remember { mutableFloatStateOf(boxWithConstraintsScope.constraints.maxWidth.toFloat()) }

        // single cell size
        var cellSize by remember(size) { mutableFloatStateOf(maxWidth / size.toFloat()) }
        // div for notes in one row in cell
        var cellSizeDivWidth by remember(size) { mutableFloatStateOf(cellSize / ceil(sqrt(size.toFloat()))) }

        SideEffect {
            maxWidth = boxWithConstraintsScope.constraints.maxWidth.toFloat()
            cellSize = maxWidth / size.toFloat()
            cellSizeDivWidth = cellSize / ceil(sqrt(size.toFloat()))
        }

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

        val context = LocalContext.current
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
                            x = selectedCell.col * cellSize,
                            y = selectedCell.row * cellSize
                        ),
                        size = Size(cellSize, cellSize)
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
                                        x = board[i][j].col * cellSize,
                                        y = board[i][j].row * cellSize
                                    ),
                                    size = Size(cellSize, cellSize)
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
                            x = it.col * cellSize,
                            y = it.row * cellSize
                        ),
                        size = Size(cellSize, cellSize)
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

            drawNumbers(
                size = size,
                board = board,
                highlightErrors = errorsHighlight,
                errorTextPaint = errorTextPaint,
                lockedTextPaint = lockedTextPaint,
                textPaint = textPaint,
                questions = questions,
                cellSize = cellSize
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