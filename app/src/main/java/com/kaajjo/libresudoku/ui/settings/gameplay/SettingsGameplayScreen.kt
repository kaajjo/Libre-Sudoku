package com.kaajjo.libresudoku.ui.settings.gameplay

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.SwitchAccessShortcut
import androidx.compose.material.icons.automirrored.outlined.ViewSidebar
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.PreferenceRow
import com.kaajjo.libresudoku.ui.components.PreferenceRowSwitch
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyColumn
import com.kaajjo.libresudoku.ui.settings.SelectionDialog
import com.kaajjo.libresudoku.ui.settings.SettingsScaffoldLazyColumn
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination(style = AnimatedNavigation::class)
@Composable
fun SettingsGameplayScreen(
    viewModel: SettingsGameplayViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
) {
    var inputMethodDialog by rememberSaveable { mutableStateOf(false) }
    var controlPanelPositionDialog by rememberSaveable { mutableStateOf(false) }
    var controlPanelScaleDialog by rememberSaveable { mutableStateOf(false) }

    val controlPanelPositionNames = listOf(
        stringResource(R.string.pref_control_panel_position_auto),
        stringResource(R.string.pref_control_panel_position_bottom),
        stringResource(R.string.pref_control_panel_position_side)
    )

    val inputMethod by viewModel.inputMethod.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_INPUT_METHOD)
    val mistakesLimit by viewModel.mistakesLimit.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_MISTAKES_LIMIT)
    val hintDisabled by viewModel.disableHints.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_HINTS_DISABLED)
    val timerEnabled by viewModel.timer.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_SHOW_TIMER)
    val resetTimer by viewModel.canResetTimer.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_GAME_RESET_TIMER)
    val funKeyboardOverNum by viewModel.funKeyboardOverNum.collectAsStateWithLifecycle(
        initialValue = PreferencesConstants.DEFAULT_FUN_KEYBOARD_OVER_NUM
    )
    val hideGameInfoRow by viewModel.hideGameInfoRow.collectAsStateWithLifecycle(
        initialValue = PreferencesConstants.DEFAULT_HIDE_GAME_INFO_ROW
    )
    val showAppBarToggle by viewModel.showAppBarToggle.collectAsStateWithLifecycle(
        initialValue = PreferencesConstants.DEFAULT_SHOW_APP_BAR_TOGGLE
    )
    val controlPanelScale by viewModel.controlPanelScale.collectAsStateWithLifecycle(
        initialValue = PreferencesConstants.DEFAULT_CONTROL_PANEL_SCALE
    )
    val controlPanelPosition by viewModel.controlPanelPosition.collectAsStateWithLifecycle(
        initialValue = PreferencesConstants.DEFAULT_CONTROL_PANEL_POSITION
    )

    SettingsScaffoldLazyColumn(
        titleText = stringResource(R.string.pref_gameplay),
        navigator = navigator
    ) {  paddingValues ->
        ScrollbarLazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
        ) {
            item {
                PreferenceRow(
                    title = stringResource(R.string.pref_input),
                    subtitle = when (inputMethod) {
                        0 -> stringResource(R.string.pref_input_cell_first)
                        1 -> stringResource(R.string.pref_input_digit_first)
                        else -> ""
                    },
                    onClick = { inputMethodDialog = true },
                    painter = rememberVectorPainter(Icons.Outlined.EditNote)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_mistakes_limit),
                    subtitle = stringResource(R.string.pref_mistakes_limit_summ),
                    checked = mistakesLimit,
                    onClick = { viewModel.updateMistakesLimit(!mistakesLimit) },
                    painter = rememberVectorPainter(Icons.Outlined.Block)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_disable_hints),
                    subtitle = stringResource(R.string.pref_disable_hints_summ),
                    checked = hintDisabled,
                    onClick = { viewModel.updateHintDisabled(!hintDisabled) },
                    painter = rememberVectorPainter(Icons.Outlined.VisibilityOff)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_show_timer),
                    checked = timerEnabled,
                    onClick = { viewModel.updateTimer(!timerEnabled) },
                    painter = rememberVectorPainter(Icons.Outlined.Schedule)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_reset_timer),
                    checked = resetTimer,
                    onClick = { viewModel.updateCanResetTimer(!resetTimer) },
                    painter = rememberVectorPainter(Icons.Outlined.HistoryToggleOff)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_fun_keyboard_over_num),
                    subtitle = stringResource(R.string.pref_fun_keyboard_over_num_subtitle),
                    checked = funKeyboardOverNum,
                    onClick = {
                        viewModel.updateFunKeyboardOverNum(!funKeyboardOverNum)
                    },
                    painter = rememberVectorPainter(Icons.Outlined.SwitchAccessShortcut)
                )
            }

            item {
                PreferenceRow(
                    title = stringResource(R.string.pref_control_panel_position),
                    subtitle = controlPanelPositionNames[
                        controlPanelPosition.coerceIn(controlPanelPositionNames.indices)
                    ],
                    onClick = { controlPanelPositionDialog = true },
                    painter = rememberVectorPainter(Icons.AutoMirrored.Outlined.ViewSidebar)
                )
            }

            item {
                PreferenceRow(
                    title = stringResource(R.string.pref_control_panel_scale),
                    subtitle = stringResource(
                        R.string.pref_control_panel_scale_value,
                        controlPanelScale
                    ),
                    onClick = { controlPanelScaleDialog = true },
                    painter = rememberVectorPainter(Icons.Outlined.FormatSize)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_hide_game_info_row),
                    subtitle = stringResource(R.string.pref_hide_game_info_row_subtitle),
                    checked = hideGameInfoRow,
                    onClick = { viewModel.updateHideGameInfoRow(!hideGameInfoRow) },
                    painter = rememberVectorPainter(Icons.Outlined.Straighten)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_show_app_bar_toggle),
                    subtitle = stringResource(R.string.pref_show_app_bar_toggle_subtitle),
                    checked = showAppBarToggle,
                    onClick = { viewModel.updateShowAppBarToggle(!showAppBarToggle) },
                    painter = rememberVectorPainter(Icons.Outlined.SmartButton)
                )
            }
        }

        if (controlPanelPositionDialog) {
            SelectionDialog(
                title = stringResource(R.string.pref_control_panel_position),
                selections = controlPanelPositionNames,
                selected = controlPanelPosition.coerceIn(controlPanelPositionNames.indices),
                onSelect = { index ->
                    viewModel.updateControlPanelPosition(index)
                },
                onDismiss = { controlPanelPositionDialog = false }
            )
        }

        if (controlPanelScaleDialog) {
            val scaleValues = PreferencesConstants.CONTROL_PANEL_SCALE_VALUES
            SelectionDialog(
                title = stringResource(R.string.pref_control_panel_scale),
                selections = scaleValues.map {
                    stringResource(R.string.pref_control_panel_scale_value, it)
                },
                selected = scaleValues.indexOf(controlPanelScale).coerceAtLeast(0),
                onSelect = { index ->
                    viewModel.updateControlPanelScale(scaleValues[index])
                },
                onDismiss = { controlPanelScaleDialog = false }
            )
        }

        if (inputMethodDialog) {
            SelectionDialog(
                title = stringResource(R.string.pref_input),
                selections = listOf(
                    stringResource(R.string.pref_input_cell_first),
                    stringResource(R.string.pref_input_digit_first)
                ),
                selected = inputMethod,
                onSelect = { index ->
                    viewModel.updateInputMethod(index)
                },
                onDismiss = { inputMethodDialog = false }
            )
        }
    }
}