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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.util.LightDarkPreview
import kotlin.math.ceil
import kotlin.math.sqrt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeyboardItem(
    modifier: Modifier = Modifier,
    number: Int,
    remainingUses: Int? = null,
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit = { },
    selected: Boolean = false
) {
    val mutableInteractionSource by remember { mutableStateOf(MutableInteractionSource()) }
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent
    )
    val localView = LocalView.current
    val keyboardFontSize = if (remainingUses != null) {
        25.sp
    } else {
        36.sp
    }
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
            modifier = Modifier.padding(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = number.toString(16).uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = keyboardFontSize,
            )
            if (remainingUses != null) {
                Text(
                    text = remainingUses.toString(),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun DefaultGameKeyboard(
    modifier: Modifier = Modifier,
    itemModifier: Modifier = Modifier,
    remainingUses: List<Int>? = null,
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    size: Int,
    selected: Int = 0
) {
    val numbers by remember(size) { mutableStateOf((1..size).toList()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (size == GameType.Default12x12.size) {
            // double-height keyboard only for 12x12
            val chunkedNumbers = numbers.chunked(6)
            if (chunkedNumbers.size == 2) {
                chunkedNumbers.forEachIndexed { index, chunked ->
                    AnimatedVisibility(
                        visible =
                        (remainingUses != null && remainingUses.chunked(6)[index].any { it > 0 }) ||
                                remainingUses == null
                    ) {
                        KeyboardRow {
                            chunked.forEach { number ->
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
                                    selected = number == selected
                                )
                            }
                        }
                    }
                }
            }
        } else {
            KeyboardRow(modifier = modifier) {
                numbers.forEach { number ->
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
                        selected = number == selected
                    )
                }
            }
        }
    }
}

@Composable
fun SquareGameKeyboard(
    modifier: Modifier = Modifier,
    itemModifier: Modifier = Modifier,
    remainingUses: List<Int>? = null,
    onClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    size: Int,
    selected: Int = 0
) {
    val numbers by remember(size) { mutableStateOf((1..size).toList()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val chunkSize = ceil(sqrt(size.toDouble())).toInt()
        val chunkedNumbers = numbers.chunked(chunkSize)
        chunkedNumbers.forEachIndexed { index, chunked ->
            AnimatedVisibility(
                visible =
                    (!remainingUses.isNullOrEmpty() && remainingUses.chunked(chunkSize)[index].any { it > 0 }) ||
                            remainingUses == null
            ) {
                KeyboardRow {
                    chunked.forEach { number ->
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
                            selected = number == selected
                        )
                    }
                }
            }
        }
    }
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
        horizontalArrangement = Arrangement.spacedBy(2.dp),
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
