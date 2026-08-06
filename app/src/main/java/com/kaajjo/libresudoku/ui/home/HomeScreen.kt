package com.kaajjo.libresudoku.ui.home

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.toFormattedString
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.destinations.GameScreenDestination
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyColumn
import com.kaajjo.libresudoku.ui.components.board.BoardPreview
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.time.ZonedDateTime
import kotlin.math.sqrt
import kotlin.time.toKotlinDuration
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Destination(style = AnimatedNavigation::class)
@RootNavGraph(start = true)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel(), navigator: DestinationsNavigator) {
    var continueGameDialog by rememberSaveable { mutableStateOf(false) }
    var lastGamesBottomSheet by rememberSaveable { mutableStateOf(false) }

    val lastGame by viewModel.lastSavedGame.collectAsStateWithLifecycle()
    val lastGames by viewModel.lastGames.collectAsStateWithLifecycle(initialValue = emptyMap())
    val saveSelectedGameDifficultyType by
            viewModel.saveSelectedGameDifficultyType.collectAsStateWithLifecycle(false)
    val lastSelectedGameDifficultyType by
            viewModel.lastSelectedGameDifficultyType.collectAsStateWithLifecycle(
                    Pair(GameDifficulty.Easy, GameType.Default9x9)
            )

    LaunchedEffect(saveSelectedGameDifficultyType) {
        if (saveSelectedGameDifficultyType) {
            val (difficulty, type) = lastSelectedGameDifficultyType
            viewModel.restoreSelection(difficulty, type)
        }
    }

    Scaffold { paddingValues ->
        Column(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
        ) {
            if (viewModel.readyToPlay) {
                viewModel.readyToPlay = false

                runBlocking {
                    // viewModel.saveToDatabase()
                    val saved = lastGame?.completed ?: false
                    navigator.navigate(
                            GameScreenDestination(
                                    gameUid = viewModel.insertedBoardUid,
                                    playedBefore = saved
                            )
                    )
                }
            }

            Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GameOptionDropdown(
                        label = stringResource(R.string.label_game_type),
                        options = viewModel.types,
                        selected = viewModel.selectedType,
                        optionName = { stringResource(it.resName) },
                        onSelect = { viewModel.selectType(it) }
                )
                Spacer(Modifier.height(8.dp))
                GameOptionDropdown(
                        label = stringResource(R.string.label_difficulty),
                        options = viewModel.availableDifficulties,
                        selected = viewModel.selectedDifficulty,
                        optionName = { stringResource(it.resName) },
                        onSelect = { viewModel.selectDifficulty(it) }
                )

                Spacer(Modifier.height(12.dp))

                // driven by the games that can actually be continued, not by the most
                // recently created one. Finishing a new game must not hide the button
                // for an older game that was never completed
                if (lastGames.isNotEmpty()) {
                    Button(
                            onClick = {
                                val onlyGame = lastGames.keys.singleOrNull()
                                if (onlyGame != null) {
                                    navigator.navigate(
                                            GameScreenDestination(
                                                    gameUid = onlyGame.uid,
                                                    playedBefore = true
                                            )
                                    )
                                } else {
                                    lastGamesBottomSheet = true
                                }
                            }
                    ) { Text(stringResource(R.string.action_continue)) }
                    FilledTonalButton(onClick = { continueGameDialog = true }) {
                        Text(stringResource(R.string.action_play))
                    }
                } else {
                    Button(
                            onClick = {
                                viewModel.giveUpLastGame()
                                viewModel.startGame()
                            }
                    ) { Text(stringResource(R.string.action_play)) }
                }
            }
        }

        if (viewModel.isGenerating || viewModel.isSolving) {
            GeneratingDialog(
                    onDismiss = {},
                    text =
                            when {
                                viewModel.isGenerating -> stringResource(R.string.dialog_generating)
                                viewModel.isSolving -> stringResource(R.string.dialog_solving)
                                else -> ""
                            }
            )
        }

        if (continueGameDialog) {
            AlertDialog(
                    title = { Text(stringResource(R.string.dialog_new_game_title)) },
                    text = { Text(stringResource(R.string.dialog_new_game_text)) },
                    confirmButton = {
                        TextButton(
                                onClick = {
                                    continueGameDialog = false
                                    viewModel.giveUpLastGame()
                                    viewModel.startGame()
                                }
                        ) { Text(stringResource(R.string.dialog_new_game_positive)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { continueGameDialog = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    },
                    onDismissRequest = { continueGameDialog = false }
            )
        }

        if (lastGamesBottomSheet) {
            ModalBottomSheet(onDismissRequest = { lastGamesBottomSheet = false }) {
                Text(
                        text =
                                pluralStringResource(
                                        id = R.plurals.last_x_games,
                                        count = lastGames.size,
                                        lastGames.size
                                ),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                ScrollbarLazyColumn(
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clip(MaterialTheme.shapes.large)
                ) {
                    items(lastGames.toList()) { item ->
                        SavedSudokuPreview(
                                board = item.first.currentBoard,
                                difficulty = stringResource(item.second.difficulty.resName),
                                type = stringResource(item.second.type.resName),
                                savedGame = item.first,
                                onClick = {
                                    navigator.navigate(
                                            GameScreenDestination(
                                                    gameUid = item.first.uid,
                                                    playedBefore = true
                                            )
                                    )
                                    lastGamesBottomSheet = false
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratingDialog(onDismiss: () -> Unit, text: String) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Column { Text(text = text, style = MaterialTheme.typography.bodyLarge) }
                    Column(modifier = Modifier.padding(top = 16.dp)) { CircularProgressIndicator() }
                }
            }
        }
    }
}

@Composable
private fun <T> GameOptionDropdown(
        label: String,
        options: List<T>,
        selected: T,
        optionName: @Composable (T) -> String,
        onSelect: (T) -> Unit,
        modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
            targetValue = if (expanded) 180f else 0f,
            label = "Dropdown arrow"
    )

    Column(modifier = modifier.widthIn(max = 320.dp).fillMaxWidth().padding(horizontal = 24.dp)) {
        Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        Box {
            OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                        text = optionName(selected),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start
                )
                Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(arrowRotation)
                )
            }
            MaterialTheme(
                    shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)
            ) {
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { option ->
                        DropdownMenuItem(
                                text = { Text(optionName(option)) },
                                onClick = {
                                    onSelect(option)
                                    expanded = false
                                },
                                trailingIcon = {
                                    if (option == selected) {
                                        Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null
                                        )
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedSudokuPreview(
        board: String,
        difficulty: String,
        type: String,
        savedGame: SavedGame,
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {}
) {
    Box(
            modifier =
                    modifier.clip(MaterialTheme.shapes.medium)
                            .clickable(onClick = onClick)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                        modifier =
                                Modifier.clip(RoundedCornerShape(4.dp))
                                        .size(130.dp)
                                        .align(Alignment.CenterVertically)
                ) { BoardPreview(size = sqrt(board.length.toFloat()).toInt(), boardString = board) }
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text("$difficulty $type")
                    Text(
                            text =
                                    stringResource(
                                            R.string.history_item_time,
                                            savedGame.timer.toKotlinDuration().toFormattedString()
                                    )
                    )
                    if (savedGame.lastPlayed != null) {
                        val lastPlayedRelative by
                                remember(savedGame) {
                                    mutableStateOf(
                                            DateUtils.getRelativeTimeSpanString(
                                                            savedGame.lastPlayed.toEpochSecond() *
                                                                    1000L,
                                                            ZonedDateTime.now().toEpochSecond() *
                                                                    1000L,
                                                            DateUtils.MINUTE_IN_MILLIS
                                                    )
                                                    .toString()
                                    )
                                }
                        Text(lastPlayedRelative)
                    }
                }
            }
            IconButton(onClick = onClick) {
                Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null
                )
            }
        }
    }
}
