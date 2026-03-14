package com.kaajjo.libresudoku.ui.backup.models

import java.io.OutputStream

/**
 * UI events for the Backup screen.
 */
sealed interface BackupUiEvent {

    /**
     * Updates the directory path where backup files will be stored.
     * @property path The new directory path.
     */
    data class SetBackupDirectory(val path: String) : BackupUiEvent

    /**
     * Exports the backup data to the provided [outputStream]
     *
     * @property outputStream the stream to save
     */
    data class SaveBackup(
        val outputStream: OutputStream
    ) : BackupUiEvent

    /**
     * Validates and prepares the backup data from a string before restoration
     *
     * @property backupString The raw backup data to be parsed
     */
    data class PrepareBackupToRestore(val backupString: String) : BackupUiEvent

    /**
     * Triggers the creation of a new backup file
     *
     * @property backupSettings Whether to include app preferences in the backup
     */
    data class CreateBackup(val backupSettings: Boolean) : BackupUiEvent

    /**
     * Restore backup from a previously prepared backup
     *
     * @property restoreSettings Whether to overwrite current app settings with those from the backup
     */
    data class RestoreBackup(val restoreSettings: Boolean) : BackupUiEvent

    /**
     * Configures the time interval between automatic backup cycles
     *
     * @property hours The interval in hours
     */
    data class SetAutoBackupInterval(val hours: Long) : BackupUiEvent

    /**
     * Sets the maximum number of automatic backup files to retain before overwriting old ones.
     *
     * @property max The maximum number of backup files
     */
    data class SetAutoBackupMaxNumber(val max: Int) : BackupUiEvent

    /**
     * Clears the current restoration error from the UI state
     */
    data object DismissRestoreError : BackupUiEvent
}