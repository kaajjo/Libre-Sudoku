package com.kaajjo.libresudoku.ui.settings.other

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.PreferenceRow
import com.kaajjo.libresudoku.ui.components.PreferenceRowSwitch
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyColumn
import com.kaajjo.libresudoku.ui.settings.SettingsScaffoldLazyColumn
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Destination(style = AnimatedNavigation::class)
@Composable
fun SettingsOtherScreen(
    viewModel: SettingsOtherViewModel = hiltViewModel(),
    navigator: DestinationsNavigator,
    launchedFromGame: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var resetGameDataDialog by rememberSaveable { mutableStateOf(false) }

    val saveLastSelectedDifficultyType by viewModel.saveLastSelectedDifficultyType
        .collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_SAVE_LAST_SELECTED_DIFF_TYPE)
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_KEEP_SCREEN_ON)
    val fullScreen by viewModel.fullScreen.collectAsStateWithLifecycle(
        initialValue = PreferencesConstants.DEFAULT_FULL_SCREEN
    )

    SettingsScaffoldLazyColumn(
        titleText = stringResource(R.string.pref_other),
        navigator = navigator,
        snackbarHostState = snackbarHostState
    ) { paddingValues ->
        ScrollbarLazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
        ) {
            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_save_last_diff_and_type),
                    subtitle = stringResource(R.string.pref_save_last_diff_and_type_subtitle),
                    checked = saveLastSelectedDifficultyType,
                    onClick = {
                        viewModel.updateSaveLastSelectedDifficultyType(!saveLastSelectedDifficultyType)
                    },
                    painter = rememberVectorPainter(Icons.Outlined.Bookmark)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_keep_screen_on),
                    checked = keepScreenOn,
                    onClick = {
                        viewModel.updateKeepScreenOn(!keepScreenOn)
                    },
                    painter = rememberVectorPainter(Icons.Outlined.Smartphone)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_full_screen),
                    subtitle = stringResource(R.string.pref_full_screen_subtitle),
                    checked = fullScreen,
                    onClick = {
                        viewModel.updateFullScreen(!fullScreen)
                    },
                    painter = rememberVectorPainter(Icons.Outlined.Fullscreen)
                )
            }

            item {
                PreferenceRow(
                    title = stringResource(R.string.pref_reset_tipcards),
                    onClick = {
                        viewModel.resetTipCards()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.resources.getString(R.string.pref_tipcards_reset)
                            )
                        }
                    },
                    painter = rememberVectorPainter(Icons.Outlined.Clear)
                )
            }

            if (!launchedFromGame) {
                item {
                    PreferenceRow(
                        title = stringResource(R.string.pref_delete_stats),
                        onClick = {
                            resetGameDataDialog = true
                        },
                        painter = rememberVectorPainter(Icons.Outlined.Delete)
                    )
                }
            }
        }

        if (resetGameDataDialog) {
            AlertDialog(
                title = { Text(stringResource(R.string.pref_delete_stats)) },
                text = { Text(stringResource(R.string.pref_delete_stats_summ)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteAllTables()
                        resetGameDataDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.resources.getString(R.string.action_deleted)
                            )
                        }
                    }) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    FilledTonalButton(onClick = { resetGameDataDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
                onDismissRequest = { resetGameDataDialog = false }
            )
        }
    }
}