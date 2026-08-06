package com.kaajjo.libresudoku.ui.settings.gameplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsGameplayViewModel @Inject constructor(
    private val settings: AppSettingsManager
) : ViewModel() {
    val inputMethod = settings.inputMethod
    fun updateInputMethod(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.setInputMethod(value)
        }
    }

    val mistakesLimit = settings.mistakesLimit
    fun updateMistakesLimit(enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            settings.setMistakesLimit(enabled)
        }

    val timer = settings.timerEnabled
    fun updateTimer(enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            settings.setTimer(enabled)
        }

    val canResetTimer = settings.resetTimerEnabled
    fun updateCanResetTimer(enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            settings.setResetTimer(enabled)
        }

    val disableHints = settings.hintsDisabled
    fun updateHintDisabled(disabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.setHintsDisabled(disabled)
        }
    }

    val funKeyboardOverNum = settings.funKeyboardOverNumbers
    fun updateFunKeyboardOverNum(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.setFunKeyboardOverNum(enabled)
        }
    }

    val hideGameInfoRow = settings.hideGameInfoRow
    fun updateHideGameInfoRow(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.setHideGameInfoRow(enabled)
        }
    }

    val showAppBarToggle = settings.showAppBarToggle
    fun updateShowAppBarToggle(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.setShowAppBarToggle(enabled)
        }
    }

    val controlPanelScale = settings.controlPanelScale
    fun updateControlPanelScale(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.setControlPanelScale(value)
        }
    }

    val controlPanelPosition = settings.controlPanelPosition
    fun updateControlPanelPosition(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.setControlPanelPosition(value)
        }
    }
}