package com.kaajjo.libresudoku.ui.backup

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.data.backup.BackupData
import com.kaajjo.libresudoku.data.backup.BackupWorker
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.ui.backup.models.BackupUiEvent
import com.kaajjo.libresudoku.ui.backup.models.BackupUiSideEffect
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.GrantPermissionCard
import com.kaajjo.libresudoku.ui.components.PreferenceRow
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyColumn
import com.kaajjo.libresudoku.ui.components.SelectionDialog
import com.kaajjo.libresudoku.ui.components.collapsing_topappbar.CollapsingTitle
import com.kaajjo.libresudoku.ui.components.collapsing_topappbar.CollapsingTopAppBar
import com.kaajjo.libresudoku.ui.components.collapsing_topappbar.rememberTopAppBarScrollBehavior
import com.kaajjo.libresudoku.ui.settings.SettingsCategory
import com.kaajjo.libresudoku.ui.theme.ColorUtils.harmonizeWithPrimary
import com.kaajjo.libresudoku.ui.util.isScrolledToEnd
import com.kaajjo.libresudoku.ui.util.isScrolledToStart
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

private val autoBackupIntervalEntries = mapOf(
    0L to R.string.autobackup_never,
    24L to R.string.daily,
    48L to R.string.every_2_days,
    120L to R.string.every_5_days,
    168L to R.string.weekly,
)

@Destination(style = AnimatedNavigation::class)
@Composable
fun BackupScreen(
    navigator: DestinationsNavigator,
    viewModel: BackupScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = rememberTopAppBarScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    var backupOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var autoBackupsNumberDialog by rememberSaveable { mutableStateOf(false) }
    var autoBackupIntervalDialog by rememberSaveable { mutableStateOf(false) }
    var restoreDialog by rememberSaveable { mutableStateOf(false) }

    var autoBackupAvailable by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        autoBackupAvailable = context.contentResolver
            .persistedUriPermissions.any { it.uri == uiState.backupDirectory.toUri() }
    }

    val requestDirectoryAccess = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                viewModel.sendEvent(
                    event = BackupUiEvent.SetBackupDirectory(
                        path = uri.toString()
                    )
                )

                autoBackupAvailable = context.contentResolver.persistedUriPermissions
                    .any { it.uri == uri }
            }
        }
    )

    val saveBackupFile = rememberLauncherForActivityResult(
        contract = CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) {
                val outputStream = context.contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    viewModel.sendEvent(
                        event = BackupUiEvent.SaveBackup(
                            outputStream = outputStream
                        )
                    )
                }
            }
        }
    )

    val selectFileToRestore = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use {
                        viewModel.sendEvent(
                            BackupUiEvent.PrepareBackupToRestore(
                                backupString = it.readText()
                            )
                        )
                    }
                }
            }
        }
    )

    LaunchedEffect(viewModel.effect, lifecycleOwner) {
        viewModel.effect
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { effect ->
                when (effect) {
                    is BackupUiSideEffect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(
                            message = effect.message.asString(context)
                        )
                    }

                    BackupUiSideEffect.BackupCreated -> {
                        saveBackupFile.launch(BackupData.nameManual)
                    }

                    BackupUiSideEffect.ReadyToRestore -> {
                        restoreDialog = true
                    }

                    BackupUiSideEffect.SaveBackup -> {
                        saveBackupFile.launch(BackupData.nameManual)
                    }
                }
            }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CollapsingTopAppBar(
                collapsingTitle = CollapsingTitle.medium(titleText = stringResource(R.string.backup_restore_title)),
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_arrow_back_24),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        ScrollbarLazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            uiState.lastBackupDate?.let { date ->
                item {
                    CardRow(
                        text = stringResource(
                            R.string.last_backup_date,
                            date.format(AppSettingsManager.dateFormat(uiState.dateFormat))
                        ),
                        icon = Icons.Rounded.History,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            item {
                PreferenceRow(
                    title = stringResource(R.string.action_create_backup),
                    subtitle = stringResource(R.string.create_backup_description),
                    painter = rememberVectorPainter(image = Icons.Rounded.FileUpload),
                    onClick = { backupOptionsDialog = true }
                )
            }
            item {
                PreferenceRow(
                    title = stringResource(R.string.action_restore),
                    subtitle = stringResource(R.string.restore_description),
                    painter = rememberVectorPainter(image = Icons.Rounded.FileDownload),
                    onClick = { selectFileToRestore.launch(arrayOf("application/json")) }
                )
            }
            item {
                SettingsCategory(title = stringResource(R.string.auto_backups))
            }
            if (!autoBackupAvailable) {
                item {
                    GrantPermissionCard(
                        title = stringResource(R.string.auto_backup_folder_access),
                        details = stringResource(R.string.auto_backup_folder_access_description),
                        painter = rememberVectorPainter(image = Icons.Outlined.Folder),
                        confirmButton = {
                            Button(
                                onClick = { requestDirectoryAccess.launch(null) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(stringResource(R.string.action_grant))
                            }
                        },
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    )
                }
            }
            item {
                uiState.autoBackupIntervalHours.let { interval ->
                    PreferenceRow(
                        title = stringResource(R.string.auto_backups_frequency),
                        subtitle = if (autoBackupIntervalEntries[interval] != null)
                            stringResource(autoBackupIntervalEntries[interval]!!)
                        else
                            pluralStringResource(
                                R.plurals.every_x_hours,
                                interval.toInt(),
                                interval.toInt()
                            ),
                        enabled = autoBackupAvailable,
                        onClick = { autoBackupIntervalDialog = true }
                    )
                }
            }
            item {
                PreferenceRow(
                    title = stringResource(R.string.auto_backups_directory),
                    subtitle = if (autoBackupAvailable) getReadableURI(uiState.backupDirectory.toUri()) else "",
                    enabled = autoBackupAvailable,
                    onClick = {
                        requestDirectoryAccess.launch(null)
                    }
                )
            }
            item {
                PreferenceRow(
                    title = stringResource(R.string.auto_backups_max),
                    subtitle = uiState.maxAutomaticBackups.toString(),
                    enabled = autoBackupAvailable,
                    onClick = {
                        autoBackupsNumberDialog = true
                    }
                )
            }
        }
    }

    if (backupOptionsDialog) {
        val options = listOf(
            R.string.backup_option_games,
            R.string.backup_option_settings,
        )
        var selectedOptions by rememberSaveable {
            mutableStateOf(listOf(0))
        }

        AlertDialog(
            title = {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.action_create_backup),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            },
            text = {
                Column {
                    options.forEachIndexed { index, text ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable(enabled = index != 0) {
                                    selectedOptions = if (selectedOptions.contains(index)) {
                                        selectedOptions - index
                                    } else {
                                        selectedOptions + index
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedOptions.contains(index),
                                onCheckedChange = {
                                    selectedOptions = if (selectedOptions.contains(index)) {
                                        selectedOptions - index
                                    } else {
                                        selectedOptions + index
                                    }
                                },
                                enabled = index != 0
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(text),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            onDismissRequest = { backupOptionsDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.sendEvent(
                        event = BackupUiEvent.CreateBackup(
                            backupSettings = selectedOptions.contains(1),
                        )
                    )
                    backupOptionsDialog = false
                }) {
                    Text(stringResource(R.string.action_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { backupOptionsDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    } else if (autoBackupIntervalDialog) {
        SelectionDialog(
            selectedValue = uiState.autoBackupIntervalHours,
            title = stringResource(R.string.auto_backups_frequency),
            onDismiss = { autoBackupIntervalDialog = false },
            entries = autoBackupIntervalEntries.mapNotNull { (key, value) ->
                key to stringResource(value)
            }.toMap(),
            onSelect = { value ->
                viewModel.sendEvent(BackupUiEvent.SetAutoBackupInterval(value))
                BackupWorker.setupWorker(context, value)
                autoBackupIntervalDialog = false
            }
        )
    } else if (autoBackupsNumberDialog) {
        SelectionDialog(
            selectedValue = uiState.maxAutomaticBackups,
            title = stringResource(R.string.auto_backups_max),
            entries = listOf(1, 2, 3, 4, 5).associateWith { it.toString() },
            onDismiss = { autoBackupsNumberDialog = false },
            onSelect = { value ->
                viewModel.sendEvent(BackupUiEvent.SetAutoBackupMaxNumber(value))
                autoBackupsNumberDialog = false
            }
        )
    } else if (restoreDialog) {
        var restoreSettings by rememberSaveable {
            mutableStateOf(true)
        }

        AlertDialog(
            title = { Text(stringResource(R.string.restoring_backup)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.restore_existing_data_alert),
                        fontWeight = FontWeight.SemiBold
                    )
                    uiState.backupData?.let { backupData ->
                        if (backupData.settings != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .fillMaxWidth()
                                    .clickable { restoreSettings = !restoreSettings },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = restoreSettings,
                                    onCheckedChange = { restoreSettings = !restoreSettings })
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.backup_restore_settings)
                                )
                            }
                        }
                    }
                }
            },
            onDismissRequest = { restoreDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.sendEvent(BackupUiEvent.RestoreBackup(restoreSettings))
                        restoreDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_restore))
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    } else if (uiState.restoreErrorText != null) {
        AlertDialog(
            title = {
                Text(stringResource(R.string.restore_backup_error))
            },
            text = {
                val clipboardManager = LocalClipboardManager.current
                Column {
                    Box {
                        val lazyListState = rememberLazyListState()

                        if (!lazyListState.isScrolledToStart()) HorizontalDivider(
                            Modifier.align(
                                Alignment.TopCenter
                            )
                        )
                        if (!lazyListState.isScrolledToEnd()) HorizontalDivider(
                            Modifier.align(
                                Alignment.BottomCenter
                            )
                        )

                        ScrollbarLazyColumn(state = lazyListState) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text(
                                        text = uiState.restoreErrorText ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight(500),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { clipboardManager.setText(AnnotatedString(text = uiState.restoreErrorText ?: "")) },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.export_string_copy))
                    }
                }
            },
            onDismissRequest = { viewModel.sendEvent(BackupUiEvent.DismissRestoreError) },
            confirmButton = {
                TextButton(onClick = { viewModel.sendEvent(BackupUiEvent.DismissRestoreError) }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            }
        )
    }
}

@Composable
fun CardRow(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp))
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.harmonizeWithPrimary())
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(6.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

/**
 * Get readable path from URI
 */
private fun getReadableURI(uri: Uri): String {
    val path = uri.path.toString()
    return if (!path.contains("primary:")) {
        // TODO: readable name from other sources
        return path
    } else {
        Environment.getExternalStorageDirectory().absolutePath + "/" + path.split("primary:").last()
    }
}