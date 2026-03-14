package com.kaajjo.libresudoku.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.BuildConfig
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.data.backup.BackupData
import com.kaajjo.libresudoku.data.backup.SettingsBackup
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import com.kaajjo.libresudoku.domain.repository.BoardRepository
import com.kaajjo.libresudoku.domain.repository.DatabaseRepository
import com.kaajjo.libresudoku.domain.repository.FolderRepository
import com.kaajjo.libresudoku.domain.repository.RecordRepository
import com.kaajjo.libresudoku.domain.repository.SavedGameRepository
import com.kaajjo.libresudoku.ui.backup.models.BackupUiEvent
import com.kaajjo.libresudoku.ui.backup.models.BackupUiSideEffect
import com.kaajjo.libresudoku.ui.backup.models.BackupUiState
import com.kaajjo.libresudoku.util.FlavorUtil
import com.kaajjo.libresudoku.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.time.ZonedDateTime
import javax.inject.Inject


@HiltViewModel
class BackupScreenViewModel @Inject constructor(
    private val appSettingsManager: AppSettingsManager,
    private val themeSettingsManager: ThemeSettingsManager,
    private val boardRepository: BoardRepository,
    private val folderRepository: FolderRepository,
    private val recordRepository: RecordRepository,
    private val savedGameRepository: SavedGameRepository,
    private val databaseRepository: DatabaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState = _uiState.asStateFlow()

    private val _effectChannel = Channel<BackupUiSideEffect>()
    val effect = _effectChannel.receiveAsFlow()

    private var backupJson: String? = null

    init {
        viewModelScope.launch {
            combine(
                appSettingsManager.backupUri,
                appSettingsManager.autoBackupsNumber,
                appSettingsManager.autoBackupInterval,
                appSettingsManager.lastBackupDate,
                appSettingsManager.dateFormat
            ) { uri, max, interval, date, format ->
                { state: BackupUiState ->
                    state.copy(
                        backupDirectory = uri,
                        maxAutomaticBackups = max,
                        autoBackupIntervalHours = interval,
                        lastBackupDate = date,
                        dateFormat = format
                    )
                }
            }.collect { mutation ->
                _uiState.update(mutation)
            }
        }
    }

    fun sendEvent(event: BackupUiEvent) {
        when (event) {
            is BackupUiEvent.CreateBackup -> createBackup(event.backupSettings)
            is BackupUiEvent.PrepareBackupToRestore -> prepareBackupToRestore(event.backupString)
            is BackupUiEvent.SaveBackup -> saveBackupTo(event.outputStream)
            is BackupUiEvent.SetBackupDirectory -> setBackupDirectory(event.path)
            is BackupUiEvent.RestoreBackup -> restoreBackup()
            is BackupUiEvent.SetAutoBackupInterval -> setAutoBackupInterval(event.hours)
            is BackupUiEvent.SetAutoBackupMaxNumber -> setAutoBackupsNumber(event.max)
            BackupUiEvent.DismissRestoreError -> dismissRestoreError()
        }
    }

    private fun createBackup(backupSettings: Boolean) {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                val boards = boardRepository.getAll().first()
                val folders = folderRepository.getAll().first()
                val records = recordRepository.getAll().first()
                val savedGames = savedGameRepository.getAll().first()

                val backupData = BackupData(
                    appVersionName = BuildConfig.VERSION_NAME + if (FlavorUtil.isFoss()) "-FOSS" else "",
                    appVersionCode = BuildConfig.VERSION_CODE,
                    createdAt = ZonedDateTime.now(),
                    boards = boards,
                    folders = folders,
                    records = records,
                    savedGames = savedGames,
                    settings = if (backupSettings) SettingsBackup.getSettings(
                        appSettingsManager,
                        themeSettingsManager
                    ) else null
                )
                _uiState.update { it.copy(backupData = backupData) }

                val json = Json {
                    encodeDefaults = true
                    ignoreUnknownKeys = true
                }
                backupJson = json.encodeToString(backupData)

                produceSideEffect(effect = BackupUiSideEffect.SaveBackup)
            }
        } catch (e: Exception) {
            produceSideEffect(
                effect = BackupUiSideEffect.ShowSnackbar(
                    message = UiText.StringResource(
                        resId = R.string.creating_backup_error
                    )
                )
            )
        }
    }

    private fun setBackupDirectory(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsManager.setBackupUri(uri)
        }
    }

    private fun prepareBackupToRestore(backupString: String) {
        try {
            val json = Json { ignoreUnknownKeys = true }
            val backupData = json.decodeFromString<BackupData?>(backupString)
            _uiState.update { it.copy(backupData = backupData) }
            produceSideEffect(BackupUiSideEffect.ReadyToRestore)
        } catch (e: Exception) {
            produceSideEffect(
                effect = BackupUiSideEffect.ShowSnackbar(
                    message = UiText.PlainString(
                        text = e.message.toString()
                    )
                )
            )
        }
    }

    private fun saveBackupTo(outputStream: OutputStream?) {
        viewModelScope.launch(Dispatchers.IO) {
            backupJson?.let { backup ->
                try {
                    outputStream?.use {
                        it.write(backup.toByteArray())
                        it.close()
                    }
                    produceSideEffect(
                        BackupUiSideEffect.ShowSnackbar(
                            UiText.StringResource(
                                resId = R.string.save_backup_success
                            )
                        )
                    )
                    appSettingsManager.setLastBackupDate(ZonedDateTime.now())
                } catch (e: Exception) {
                    e.printStackTrace()
                    produceSideEffect(
                        BackupUiSideEffect.ShowSnackbar(
                            UiText.StringResource(
                                resId = R.string.save_backup_error
                            )
                        )
                    )
                }
            }
        }
    }

    private fun restoreBackup() {
        _uiState.value.backupData?.let { backup ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // deleting all data from the database
                    // maybe dangerous actually
                    runBlocking { databaseRepository.resetDb() }

                    if (backup.boards.isNotEmpty()) {
                        folderRepository.insert(backup.folders)
                        boardRepository.insert(backup.boards)
                        savedGameRepository.insert(backup.savedGames)
                        recordRepository.insert(backup.records)
                    }

                    backup.settings?.setSettings(appSettingsManager, themeSettingsManager)
                    produceSideEffect(
                        effect = BackupUiSideEffect.ShowSnackbar(
                            message = UiText.StringResource(
                                resId = R.string.restore_backup_success
                            )
                        )
                    )
                } catch (e: Exception) {
                    _uiState.update { it.copy(restoreErrorText = e.message.toString()) }
                }
            }
        }
    }

    private fun setAutoBackupsNumber(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsManager.setAutoBackupsNumber(value)
        }
    }

    private fun setAutoBackupInterval(hours: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsManager.setAutoBackupInterval(hours)
        }
    }

    private fun produceSideEffect(effect: BackupUiSideEffect) {
        viewModelScope.launch {
            _effectChannel.send(effect)
        }
    }

    private fun dismissRestoreError() {
        _uiState.update { it.copy(restoreErrorText = null) }
    }
}