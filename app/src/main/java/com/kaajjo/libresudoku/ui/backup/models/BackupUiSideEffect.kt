package com.kaajjo.libresudoku.ui.backup.models

import com.kaajjo.libresudoku.util.UiText

/**
 * One time effects for the Backup screen
 */
sealed interface BackupUiSideEffect {

    /**
     * Triggers a Snackbar to display a [message].
     *
     * @property message the message to be displayed
     */
    data class ShowSnackbar(val message: UiText) : BackupUiSideEffect

    /**
     * Signals that the backup file has been successfully parsed and is ready to be restored.
     */
    data object ReadyToRestore : BackupUiSideEffect

    /**
     * Signals that the backup operation has completed successfully.
     */
    data object BackupCreated : BackupUiSideEffect

    /**
     * Triggers the system UI to save the backup file
     */
    data object SaveBackup : BackupUiSideEffect
}