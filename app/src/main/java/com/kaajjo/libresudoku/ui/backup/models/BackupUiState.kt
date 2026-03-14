package com.kaajjo.libresudoku.ui.backup.models

import com.kaajjo.libresudoku.data.backup.BackupData
import java.time.ZonedDateTime

/**
 * UI Model for Backup screen
 *
 * @property backupData contains all data for the backup
 * @property backupDirectory the directory where backups are saved
 * @property maxAutomaticBackups the maximum number of backups to keep
 * @property autoBackupIntervalHours the interval in hours between automatic backups
 * @property lastBackupDate the timestamp of the last successful backup
 * @property dateFormat the pattern used for formatting backup dates
 * @property restoreErrorText the restoration error message
 */
data class BackupUiState(
    val backupData: BackupData? = null,
    val backupDirectory: String = "",
    val maxAutomaticBackups: Int = 0,
    val autoBackupIntervalHours: Long = 0,
    val lastBackupDate: ZonedDateTime? = null,
    val dateFormat: String = "",
    val restoreErrorText: String? = null
)
