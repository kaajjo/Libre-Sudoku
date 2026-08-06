package com.kaajjo.libresudoku.ui.statistics

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.destinations.GamesHistoryScreenDestination
import com.kaajjo.libresudoku.destinations.SavedGameScreenDestination
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.EmptyScreen
import com.kaajjo.libresudoku.ui.components.HelpCard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Destination(style = AnimatedNavigation::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle(initialValue = "")
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics)) },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { navigator.navigate(GamesHistoryScreenDestination()) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_history_24),
                            contentDescription = null
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { scaffoldPadding ->
        val recordListState = viewModel.recordList.collectAsState(initial = emptyList())
        val savedGameList = viewModel.savedGamesList.collectAsState(initial = emptyList())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ChipRowDifficulty(
                items = listOf(
                    GameDifficulty.Unspecified,
                    GameDifficulty.Easy,
                    GameDifficulty.Moderate,
                    GameDifficulty.Hard,
                    GameDifficulty.Challenge,
                    GameDifficulty.Custom
                ),
                selected = viewModel.selectedDifficulty,
                onSelected = { viewModel.setDifficulty(it) }
            )
            ChipRowType(
                types = listOf(
                    Pair(GameType.Default9x9, stringResource(R.string.type_default_9x9)),
                    Pair(GameType.Default6x6, stringResource(R.string.type_default_6x6)),
                    Pair(GameType.Default12x12, stringResource(R.string.type_default_12x12)),
                    Pair(GameType.Killer9x9, stringResource(R.string.type_killer_9x9)),
                    Pair(GameType.Killer6x6, stringResource(R.string.type_killer_6x6)),
                    Pair(GameType.Killer12x12, stringResource(R.string.type_killer_12x12)),
                    Pair(GameType.Default16x16, stringResource(R.string.type_default_16x16))
                ),
                selected = viewModel.selectedType,
                onSelected = { viewModel.setType(it) }
            )

            if (recordListState.value.isNotEmpty()) {
                var averageTime by remember {
                    mutableStateOf(
                        DateUtils.formatElapsedTime(recordListState.value.sumOf { it.time.seconds } / recordListState.value.count())
                    )
                }
                var bestTime by remember {
                    mutableStateOf(
                        DateUtils.formatElapsedTime(recordListState.value.first().time.seconds)
                    )
                }
                LaunchedEffect(recordListState.value) {
                    averageTime = DateUtils.formatElapsedTime(
                        recordListState.value
                            .sumOf { it.time.seconds } / recordListState.value.count()
                    )
                    bestTime = DateUtils.formatElapsedTime(
                        recordListState.value.first().time.seconds
                    )

                }
                StatisticsSection(
                    title = stringResource(R.string.time),
                    painter = painterResource(R.drawable.ic_round_hourglass_empty_24),
                    statRows = listOf(
                        listOf(stringResource(R.string.best_time), bestTime),
                        listOf(stringResource(R.string.average_time), averageTime),
                    )
                )
                if (viewModel.selectedDifficulty == GameDifficulty.Unspecified) {
                    val context = LocalContext.current
                    val gamesStarted by remember {
                        mutableStateOf(savedGameList.value.count().toString())
                    }
                    val gamesCompleted by remember {
                        mutableStateOf(
                            savedGameList.value
                                .count { it.completed && !it.giveUp && !it.canContinue }
                                .toString()
                        )
                    }
                    val winRate by remember {
                        mutableStateOf(
                            if (savedGameList.value.isNotEmpty()) {
                                context.getString(
                                    R.string.win_rate_percentage,
                                    viewModel.getWinRate(savedGameList.value).roundToInt()
                                )
                            } else {
                                context.getString(R.string.no_value_default)
                            }
                        )
                    }
                    OverallStatistics(
                        statsRow = listOf(
                            listOf(stringResource(R.string.games_started), gamesStarted),
                            listOf(stringResource(R.string.games_completed), gamesCompleted),
                            listOf(stringResource(R.string.win_rate), winRate)
                        )
                    )

                    val currentStreak by remember {
                        mutableStateOf(
                            viewModel.getCurrentStreak(savedGameList.value).toString()
                        )
                    }
                    val maxStreak by remember {
                        mutableStateOf(
                            viewModel.getMaxStreak(savedGameList.value).toString()
                        )
                    }

                    StatisticsSection(
                        title = stringResource(R.string.win_streak),
                        painter = painterResource(R.drawable.ic_outline_verified_24),
                        statRows = listOf(
                            listOf(stringResource(R.string.current_streak), currentStreak),
                            listOf(stringResource(R.string.best_streak), maxStreak)
                        )
                    )
                    val streakCard = viewModel.streakTipCard.collectAsState(initial = false)
                    AnimatedVisibility(visible = streakCard.value) {
                        HelpCard(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            title = stringResource(R.string.win_streak),
                            details = stringResource(R.string.tip_card_win_streak_summ),
                            painter = painterResource(R.drawable.ic_outline_verified_24),
                            onCloseClicked = { viewModel.setStreakTipCard(false) }
                        )
                    }
                }
                StatsSectionName(
                    modifier = Modifier.padding(start = 12.dp, top = 12.dp),
                    title = stringResource(R.string.number_best_games, 5) +
                            if (viewModel.selectedType != GameType.Unspecified && viewModel.selectedDifficulty != GameDifficulty.Unspecified
                            ) {
                                " ${stringResource(viewModel.selectedType.resName).lowercase()} " +
                                        stringResource(viewModel.selectedDifficulty.resName).lowercase()
                            } else {
                                ""
                            },
                    painter = painterResource(R.drawable.ic_outline_star_24)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        var selectedIndex by remember { mutableIntStateOf(0) }
                        recordListState.value.take(5).forEachIndexed { index, record ->
                            RecordItem(
                                time = record.time,
                                difficulty = stringResource(record.difficulty.resName),
                                date = record.date.toLocalDateTime(),
                                type = stringResource(record.type.resName),
                                dateFormat = dateFormat,
                                onClick = {
                                    navigator.navigate(SavedGameScreenDestination(gameUid = record.board_uid))
                                },
                                onLongClick = {
                                    selectedIndex = index
                                    viewModel.showDeleteDialog = true
                                }
                            )
                        }
                        if (viewModel.showDeleteDialog) {
                            ShowDeleteDialog(
                                onDismissRequest = { viewModel.showDeleteDialog = false },
                                onConfirm = {
                                    viewModel.deleteRecord(
                                        recordListState.value[selectedIndex]
                                    )
                                },
                                index = selectedIndex
                            )
                        }
                    }
                }
                val recordCard = viewModel.recordTipCard.collectAsState(initial = false)
                AnimatedVisibility(visible = recordCard.value) {
                    HelpCard(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        title = stringResource(R.string.tip_card_records_list_title),
                        details = stringResource(R.string.tip_card_records_list_summ),
                        painter = painterResource(R.drawable.ic_outline_help_outline_24),
                        onCloseClicked = { viewModel.setRecordTipCard(false) }
                    )
                }
            } else {
                EmptyScreen(stringResource(R.string.statistics_no_records))
            }
        }
    }
}


@Composable
fun ShowDeleteDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    index: Int
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismissRequest()
            }) {
                Text(stringResource(R.string.dialog_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.dialog_no))
            }
        },
        title = {
            Text(stringResource(R.string.delete_question))
        },
        text = {
            Text(
                text = stringResource(R.string.delete_record_dialog, index + 1)
            )
        }
    )
}

@Composable
fun OverallStatistics(
    modifier: Modifier = Modifier,
    statsRow: List<List<String>>
) {
    StatisticsSection(
        modifier = modifier,
        title = stringResource(R.string.games),
        painter = painterResource(R.drawable.ic_rounded_stadia_controller_24),
        statRows = statsRow
    )
}

@Composable
fun StatisticsSection(
    modifier: Modifier = Modifier,
    title: String,
    painter: Painter,
    statRows: List<List<String>>
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        StatsSectionName(
            title = title,
            painter = painter
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {

                statRows.forEachIndexed { index, arr ->
                    StatRow(
                        startText = arr[0],
                        endText = arr[1],
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    if (index + 1 != statRows.size) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun StatsSectionName(
    modifier: Modifier = Modifier,
    title: String,
    painter: Painter
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun StatRow(
    startText: String,
    endText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = startText,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
        Text(
            text = endText,
            fontWeight = FontWeight(700)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipRowType(
    modifier: Modifier = Modifier,
    types: List<Pair<GameType, String>>,
    selected: GameType,
    onSelected: (GameType) -> Unit
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(types) { type ->
            val selectedColor by animateColorAsState(
                targetValue = if (type.first == selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                label = "Chip color animation"
            )
            ElevatedFilterChip(
                modifier = Modifier.padding(horizontal = 2.dp),
                selected = type.first == selected,
                onClick = { onSelected(type.first) },
                label = { Text(type.second) },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = selectedColor,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = FilterChipDefaults.elevatedFilterChipElevation(4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipRowDifficulty(
    items: List<GameDifficulty>,
    selected: GameDifficulty,
    onSelected: (GameDifficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(items) { item ->
            val selectedColor by animateColorAsState(
                targetValue = if (selected == item) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
            )
            ElevatedFilterChip(
                selected = selected == item,
                onClick = { onSelected(item) },
                label = {
                    Text(
                        if (item != GameDifficulty.Unspecified) {
                            stringResource(item.resName)
                        } else {
                            stringResource(R.string.statistics_difficulty_filter_all)
                        }
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = selectedColor,
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = FilterChipDefaults.elevatedFilterChipElevation(4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordItem(
    modifier: Modifier = Modifier,
    time: Duration,
    date: LocalDateTime,
    difficulty: String,
    type: String,
    dateFormat: String,
    onClick: () -> Unit = { },
    onLongClick: () -> Unit = { }
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$difficulty $type"
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.time) + ": ${DateUtils.formatElapsedTime(time.seconds)}"
                )
            }
            Row {
                Text(
                    text = date.format(AppSettingsManager.dateFormat(dateFormat))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = date.format(DateTimeFormatter.ofPattern("HH:mm"))
                )
            }
        }
    }
}