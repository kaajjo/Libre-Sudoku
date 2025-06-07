package com.kaajjo.libresudoku.ui.gameshistory.savedgame

import android.content.res.Configuration
import android.os.Build.VERSION.SDK_INT
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.core.utils.toFormattedString
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.destinations.ExploreFolderScreenDestination
import com.kaajjo.libresudoku.destinations.GameScreenDestination
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.EmptyScreen
import com.kaajjo.libresudoku.ui.components.board.Board
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.time.toKotlinDuration

@Destination(
    style = AnimatedNavigation::class,
    navArgsDelegate = SavedGameScreenNavArgs::class
)
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun SavedGameScreen(
    viewModel: SavedGameViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle(
        initialValue = ""
    )
    val dateTimeFormatter by remember(dateFormat) {
        mutableStateOf(
            AppSettingsManager.dateFormat(dateFormat)
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.game_id, viewModel.boardUid)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_arrow_back_24),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.export_string_title))
                                    },
                                    onClick = {
                                        viewModel.exportDialog = true
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        LaunchedEffect(Unit) { viewModel.updateGameDetails() }

        if (viewModel.savedGame != null && viewModel.boardEntity != null &&
            viewModel.parsedCurrentBoard.isNotEmpty() && viewModel.parsedInitialBoard.isNotEmpty()
        ) {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            val crossHighlight by viewModel.crossHighlight.collectAsStateWithLifecycle(
                initialValue = PreferencesConstants.DEFAULT_BOARD_CROSS_HIGHLIGHT
            )
            val fontSizeFactor by viewModel.fontSize.collectAsState(initial = PreferencesConstants.DEFAULT_FONT_SIZE_FACTOR)
            val fontSizeValue by remember(fontSizeFactor) {
                mutableStateOf(
                    viewModel.getFontSize(factor = fontSizeFactor)
                )
            }

            val pagerState = rememberPagerState(pageCount = { 2 })
            val pages = listOf(
                stringResource(R.string.saved_game_current),
                stringResource(R.string.saved_game_initial)
            )

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                    ) {
                        SecondaryTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            divider = { },
                        ) {
                            pages.forEachIndexed { index, title ->
                                val coroutineScope = rememberCoroutineScope()
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index, 0f)
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = title,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                        val boardScale = remember { Animatable(0.3f) }
                        LaunchedEffect(Unit) {
                            boardScale.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = LinearOutSlowInEasing
                                )
                            )
                        }
                        val boardModifier = Modifier
                            .padding(10.dp)
                            .scale(boardScale.value)
                        Column {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .wrapContentHeight()
                                    .padding(top = 8.dp),
                            ) { page ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    when (page) {
                                        0 -> Board(
                                            board = viewModel.parsedCurrentBoard,
                                            notes = viewModel.notes,
                                            modifier = boardModifier,
                                            mainTextSize = fontSizeValue,
                                            autoFontSize = fontSizeFactor == 0,
                                            selectedCell = Cell(-1, -1),
                                            onClick = { },
                                            crossHighlight = crossHighlight,
                                            cages = viewModel.killerCages
                                        )

                                        1 -> Board(
                                            board = viewModel.parsedInitialBoard,
                                            modifier = boardModifier,
                                            mainTextSize = fontSizeValue,
                                            autoFontSize = fontSizeFactor == 0,
                                            selectedCell = Cell(-1, -1),
                                            onClick = { },
                                            crossHighlight = crossHighlight,
                                            cages = viewModel.killerCages
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth()
                    ) {
                        val gameFolder by viewModel.gameFolder.collectAsStateWithLifecycle()
                        gameFolder?.let {
                            AssistChip(
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Folder,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    navigator.navigate(
                                        ExploreFolderScreenDestination(
                                            folderUid = it.uid
                                        )
                                    )
                                },
                                label = { Text(it.name) }
                            )
                        }

                        val textStyle = MaterialTheme.typography.bodyLarge

                        val progressPercentage by viewModel.gameProgressPercentage.collectAsStateWithLifecycle()
                        LaunchedEffect(viewModel.parsedCurrentBoard) { viewModel.countProgressFilled() }

                        Text(
                            text = stringResource(
                                R.string.saved_game_progress_percentage,
                                progressPercentage
                            ),
                            style = textStyle
                        )


                        viewModel.savedGame?.let { savedGame ->
                            if (savedGame.startedAt != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val startedAtDate by remember(savedGame) {
                                        mutableStateOf(
                                            savedGame.startedAt.format(dateTimeFormatter)
                                        )
                                    }
                                    val startedAtTime by remember(savedGame) {
                                        mutableStateOf(
                                            savedGame.startedAt.format(DateTimeFormatter.ofPattern("HH:mm"))
                                        )
                                    }
                                    Text(startedAtDate)
                                    Text(startedAtTime)
                                }
                            }
                        }

                        Text(
                            text = viewModel.savedGame?.let {
                                when {
                                    it.mistakes >= PreferencesConstants.MISTAKES_LIMIT -> stringResource(
                                        R.string.saved_game_mistakes_limit
                                    )

                                    it.giveUp -> stringResource(R.string.saved_game_give_up)
                                    it.completed && !it.canContinue -> stringResource(R.string.saved_game_completed)
                                    else -> stringResource(R.string.saved_game_in_progress)
                                }
                            } ?: ""
                        )

                        Text(
                            text = stringResource(
                                R.string.saved_game_difficulty,
                                stringResource(viewModel.boardEntity!!.difficulty.resName)
                            ),
                            style = textStyle
                        )
                        Text(
                            text = stringResource(
                                R.string.saved_game_type,
                                stringResource(viewModel.boardEntity!!.type.resName)
                            ),
                            style = textStyle
                        )
                        Text(
                            text = stringResource(
                                R.string.saved_game_time,
                                viewModel.savedGame!!.timer
                                    .toKotlinDuration()
                                    .toFormattedString()
                            )
                        )

                        if (viewModel.savedGame!!.canContinue) {
                            FilledTonalButton(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                onClick = {
                                    navigator.navigate(
                                        GameScreenDestination(
                                            gameUid = viewModel.savedGame!!.uid, playedBefore = true
                                        )
                                    )
                                }
                            ) {
                                Text(stringResource(R.string.action_continue))
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxWidth()
                ) {
                    SecondaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        divider = { },
                    ) {
                        pages.forEachIndexed { index, title ->
                            val coroutineScope = rememberCoroutineScope()
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index, 0f)
                                    }
                                },
                                text = {
                                    Text(
                                        text = title,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                    val boardScale = remember { Animatable(0.3f) }
                    LaunchedEffect(Unit) {
                        boardScale.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = LinearOutSlowInEasing
                            )
                        )
                    }
                    val boardModifier = Modifier
                        .padding(10.dp)
                        .scale(boardScale.value)
                    Column {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(top = 8.dp)
                        ) { page ->
                            when (page) {
                                0 -> Board(
                                    board = viewModel.parsedCurrentBoard,
                                    notes = viewModel.notes,
                                    modifier = boardModifier,
                                    mainTextSize = fontSizeValue,
                                    autoFontSize = fontSizeFactor == 0,
                                    selectedCell = Cell(-1, -1),
                                    onClick = { },
                                    crossHighlight = crossHighlight,
                                    cages = viewModel.killerCages
                                )

                                1 -> Board(
                                    board = viewModel.parsedInitialBoard,
                                    modifier = boardModifier,
                                    mainTextSize = fontSizeValue,
                                    autoFontSize = fontSizeFactor == 0,
                                    selectedCell = Cell(-1, -1),
                                    onClick = { },
                                    crossHighlight = crossHighlight,
                                    cages = viewModel.killerCages
                                )
                            }
                        }
                    }


                    Column(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .fillMaxWidth()
                    ) {
                        val gameFolder by viewModel.gameFolder.collectAsStateWithLifecycle()
                        gameFolder?.let {
                            AssistChip(
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Folder,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    navigator.navigate(
                                        ExploreFolderScreenDestination(
                                            folderUid = it.uid
                                        )
                                    )
                                },
                                label = { Text(it.name) }
                            )
                        }

                        val textStyle = MaterialTheme.typography.bodyLarge

                        val progressPercentage by viewModel.gameProgressPercentage.collectAsStateWithLifecycle()
                        LaunchedEffect(viewModel.parsedCurrentBoard) { viewModel.countProgressFilled() }

                        Text(
                            text = stringResource(
                                R.string.saved_game_progress_percentage,
                                progressPercentage
                            ),
                            style = textStyle
                        )


                        viewModel.savedGame?.let { savedGame ->
                            if (savedGame.startedAt != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val startedAtDate by remember(savedGame) {
                                        mutableStateOf(
                                            savedGame.startedAt.format(dateTimeFormatter)
                                        )
                                    }
                                    val startedAtTime by remember(savedGame) {
                                        mutableStateOf(
                                            savedGame.startedAt.format(DateTimeFormatter.ofPattern("HH:mm"))
                                        )
                                    }
                                    Text(startedAtDate)
                                    Text(startedAtTime)
                                }
                            }
                        }

                        Text(
                            text = viewModel.savedGame?.let {
                                when {
                                    it.mistakes >= PreferencesConstants.MISTAKES_LIMIT -> stringResource(
                                        R.string.saved_game_mistakes_limit
                                    )

                                    it.giveUp -> stringResource(R.string.saved_game_give_up)
                                    it.completed && !it.canContinue -> stringResource(R.string.saved_game_completed)
                                    else -> stringResource(R.string.saved_game_in_progress)
                                }
                            } ?: ""
                        )

                        Text(
                            text = stringResource(
                                R.string.saved_game_difficulty,
                                stringResource(viewModel.boardEntity!!.difficulty.resName)
                            ),
                            style = textStyle
                        )
                        Text(
                            text = stringResource(
                                R.string.saved_game_type,
                                stringResource(viewModel.boardEntity!!.type.resName)
                            ),
                            style = textStyle
                        )
                        Text(
                            text = stringResource(
                                R.string.saved_game_time,
                                viewModel.savedGame!!.timer
                                    .toKotlinDuration()
                                    .toFormattedString()
                            )
                        )

                        if (viewModel.savedGame!!.canContinue) {
                            FilledTonalButton(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                onClick = {
                                    navigator.navigate(
                                        GameScreenDestination(
                                            gameUid = viewModel.savedGame!!.uid, playedBefore = true
                                        )
                                    )
                                }
                            ) {
                                Text(stringResource(R.string.action_continue))
                            }
                        }
                    }
                }
            }
        } else {
            EmptyScreen(stringResource(R.string.empty_screen_something_went_wrong))
        }

        if (viewModel.exportDialog) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.exportDialog = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                viewModel.boardEntity?.let {
                    var copyInitial by rememberSaveable { mutableStateOf(true) }
                    var selectedEmptyCell by rememberSaveable { mutableStateOf(SudokuParser.EMPTY_SEPARATORS.first()) }
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.export_string_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row {
                                FilterChip(
                                    selected = copyInitial,
                                    label = {
                                        Text(stringResource(R.string.saved_game_initial))
                                    },
                                    onClick = {
                                        copyInitial = true
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                FilterChip(
                                    selected = !copyInitial,
                                    label = {
                                        Text(stringResource(R.string.saved_game_current))
                                    },
                                    onClick = {
                                        copyInitial = false
                                    }
                                )
                            }
                            viewModel.savedGame?.let { savedGame ->
                                AnimatedContent(
                                    (if (copyInitial) it.initialBoard else savedGame.currentBoard)
                                        .replace('0', selectedEmptyCell)
                                        .uppercase()
                                ) { board ->
                                    OutlinedTextField(
                                        modifier = Modifier
                                            .padding(top = 8.dp),
                                        value = board,
                                        onValueChange = { },
                                        readOnly = true
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.export_empty_cell),
                                style = MaterialTheme.typography.titleMedium
                            )
                            FlowRow(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SudokuParser.EMPTY_SEPARATORS
                                    .forEach {
                                        FilterChip(
                                            onClick = {
                                                selectedEmptyCell = it
                                            },
                                            label = {
                                                Text(it.toString())
                                            },
                                            selected = selectedEmptyCell == it
                                        )
                                    }
                            }
                        }
                        Button(
                            onClick = {
                                val exported =
                                    (if (copyInitial) it.initialBoard else viewModel.savedGame?.currentBoard
                                        ?: "")
                                        .replace('0', selectedEmptyCell)
                                        .uppercase()

                                clipboardManager.setText(
                                    AnnotatedString(exported)
                                )
                                // Android 13 and higher have its own notification when copying
                                if (SDK_INT < 33) {
                                    Toast.makeText(
                                        context,
                                        R.string.export_string_state_copied,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .align(Alignment.End)
                        ) {
                            Text(stringResource(R.string.export_string_copy))
                        }
                    }
                }
            }
        }
    }
}