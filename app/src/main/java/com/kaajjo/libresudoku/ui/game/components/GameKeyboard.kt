package com.kaajjo.libresudoku.ui.game.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.ui.components.board.BOARD_DIGIT_RADIX
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.util.LightDarkPreview

/** Spacing between the keys of the keyboard */
private val KeyboardSpacing = 2.dp

/** Horizontal padding inside a single key */
private val KeyHorizontalPadding = 4.dp

/** Approximate width of a bold digit relative to its font size */
private const val DigitWidthRatio = 0.62f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyboardItem(
    modifier: Modifier = Modifier,
    number: Int,
    remainingUses: Int? = null,
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit = { },
    selected: Boolean = false,
    fontSize: TextUnit = defaultKeyboardFontSize(remainingUses != null),
    scale: Float = 1f
) {
    val mutableInteractionSource by remember { mutableStateOf(MutableInteractionSource()) }
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent
    )
    val localView = LocalView.current
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color)
            .combinedClickable(
                interactionSource = mutableInteractionSource,
                onClick = {
                    onClick(number)
                },
                onLongClick = {
                    localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onLongClick(number)
                },
                indication = ripple(
                    bounded = true,
                    color = MaterialTheme.colorScheme.primary
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = KeyHorizontalPadding,
                vertical = 7.dp * scale
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = number.toString(BOARD_DIGIT_RADIX).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                maxLines = 1
            )
            if (remainingUses != null) {
                Text(
                    text = remainingUses.toString(),
                    fontSize = 11.sp * scale
                )
            }
        }
    }
}

/**
 * Number of keys in a single keyboard row for a [size]x[size] game.
 * 12x12 doesn't fit into one row, so it uses two rows.
 */
fun defaultKeyboardColumns(size: Int): Int = when (size) {
    GameType.Default12x12.size -> 6
    GameType.Default16x16.size -> 8
    else -> size
}

@Composable
fun DefaultGameKeyboard(
    modifier: Modifier = Modifier,
    itemModifier: Modifier = Modifier,
    remainingUses: List<Int>? = null,
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    size: Int,
    selected: Int = 0,
    columns: Int = defaultKeyboardColumns(size),
    scale: Float = 1f
) {
    val keysInRow = columns.coerceIn(1, size)
    val rows by remember(size, keysInRow) { mutableStateOf((1..size).toList().chunked(keysInRow)) }

    BoxWithConstraints(modifier = modifier) {
        // shrink the digits when the keys are too narrow to fit them, otherwise they
        // get clipped by the round key background on small screens
        val fontSize = fittingKeyboardFontSize(
            keyWidth = if (maxWidth != Dp.Infinity) {
                (maxWidth - KeyboardSpacing * (keysInRow - 1)) / keysInRow
            } else {
                Dp.Infinity
            },
            withRemainingUses = remainingUses != null,
            scale = scale
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(KeyboardSpacing)
        ) {
            rows.forEachIndexed { index, rowNumbers ->
                // on a multi row keyboard a row without any number left to place is
                // hidden entirely. A single row keyboard always stays visible
                val rowVisible = remainingUses == null || rows.size == 1 ||
                        remainingUses.chunked(keysInRow).getOrNull(index)?.any { it > 0 } != false
                AnimatedVisibility(visible = rowVisible) {
                    KeyboardRow {
                        rowNumbers.forEach { number ->
                            val hide =
                                remainingUses != null && (remainingUses.size > number && remainingUses[number - 1] <= 0)
                            KeyboardItem(
                                modifier = itemModifier
                                    .weight(1f)
                                    .alpha(if (hide) 0f else 1f),
                                number = number,
                                onClick = {
                                    if (!hide) {
                                        onClick(number)
                                    }
                                },
                                onLongClick = {
                                    if (!hide) {
                                        onLongClick(number)
                                    }
                                },
                                remainingUses = if (remainingUses != null && remainingUses.size >= number) {
                                    remainingUses[number - 1]
                                } else {
                                    null
                                },
                                selected = number == selected,
                                fontSize = fontSize,
                                scale = scale
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Font size of a key when there is enough room for it */
@Composable
private fun defaultKeyboardFontSize(withRemainingUses: Boolean, scale: Float = 1f): TextUnit =
    (if (withRemainingUses) 25.sp else 36.sp) * scale

/**
 * Font size of a key that is guaranteed to fit into a key of [keyWidth].
 * Returns the default size when the key is wide enough or when [keyWidth] is unbounded.
 */
@Composable
private fun fittingKeyboardFontSize(
    keyWidth: Dp,
    withRemainingUses: Boolean,
    scale: Float = 1f
): TextUnit {
    val default = defaultKeyboardFontSize(withRemainingUses, scale)
    if (keyWidth == Dp.Infinity) return default

    val availableForDigit = (keyWidth - KeyHorizontalPadding * 2).coerceAtLeast(0.dp)
    val fitting = with(LocalDensity.current) { availableForDigit.toSp() } / DigitWidthRatio
    return minOf(default.value, fitting.value).sp
}

@Composable
private fun KeyboardRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier),
        horizontalArrangement = Arrangement.spacedBy(KeyboardSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}


@LightDarkPreview
@Composable
private fun KeyboardItemPreview() {
    LibreSudokuTheme {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                KeyboardItem(
                    number = 1,
                    onClick = { }
                )
                KeyboardItem(
                    number = 1,
                    selected = true,
                    onClick = { }
                )
                KeyboardItem(
                    number = 1,
                    remainingUses = 5,
                    onClick = { }
                )
                KeyboardItem(
                    number = 1,
                    remainingUses = 5,
                    selected = true,
                    onClick = { }
                )
            }
        }
    }
}


@LightDarkPreview
@Composable
private fun KeyboardPreview9x9() {
    LibreSudokuTheme {
        Surface {
            DefaultGameKeyboard(
                onClick = { },
                onLongClick = { },
                size = 9,
                remainingUses = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun KeyboardPreview12x12() {
    LibreSudokuTheme {
        Surface {
            DefaultGameKeyboard(
                onClick = { },
                onLongClick = { },
                size = 12,
                remainingUses = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
            )
        }
    }
}
