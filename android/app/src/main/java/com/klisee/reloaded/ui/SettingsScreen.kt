package com.klisee.reloaded.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.klisee.reloaded.GameRepository
import com.klisee.reloaded.Preferences
import com.klisee.reloaded.Update
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repo: GameRepository,
    prefs: Preferences,
    revision: Int = 0,
    onLog: () -> Unit,
    onAbout: () -> Unit,
    onControls: () -> Unit,
    onMySaves: () -> Unit,
    onUpdateFound: (Update.Release) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var status by remember(revision) { mutableStateOf(repo.status()) }
    var keepScreenOn by remember { mutableStateOf(prefs.keepScreenOn) }
    var keepAspect by remember { mutableStateOf(prefs.keepAspect) }
    var checking by remember { mutableStateOf(false) }
    val appVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    var confirmingDelete by remember { mutableStateOf(false) }
    val installed = status as? GameRepository.Status.Ready

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete the game?") },
            text = { Text("This removes the extracted copy (${formatBytes(installed?.sizeBytes ?: 0)}). You can download or import it again, but your saved games will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    scope.launch {
                        repo.deleteGame()
                        status = repo.status()
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        bottomBar = bottomBar,
        // Lifted clear of the floating bar, which otherwise sits on top of the message.
        snackbarHost = { SnackbarHost(snackbar, modifier = Modifier.padding(bottom = BottomNavSpace)) },
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            SettingsGroup("Game") {
                if (installed == null) {
                    InfoRow(
                        icon = Icons.Filled.Info,
                        title = "No game installed",
                        value = "Download or import it from the home screen",
                        items = 1,
                        index = 0,
                    )
                } else {
                    InfoRow(Icons.Filled.VideogameAsset, "Version", installed.version ?: "No data", items = 4, index = 0)
                    InfoRow(Icons.Filled.Storage, "Size on disk", formatBytes(installed.sizeBytes), items = 4, index = 1)
                    InfoRow(Icons.Filled.CalendarMonth, "Installed", formatDate(installed.installedAt), items = 4, index = 2)
                    NavRow(
                        icon = Icons.Filled.Delete,
                        title = "Delete game",
                        subtitle = "Free up ${formatBytes(installed.sizeBytes)} of storage",
                        items = 4,
                        index = 3,
                        onClick = { confirmingDelete = true },
                    )
                }
            }

            SettingsGroup("Saves") {
                NavRow(
                    icon = Icons.Filled.Save,
                    title = "My saves",
                    subtitle = if (installed == null) {
                        "The game is not installed"
                    } else {
                        "View the saves stored in the game"
                    },
                    items = 1,
                    index = 0,
                    enabled = installed != null,
                    onClick = onMySaves,
                )
            }

            SettingsGroup("Controller") {
                NavRow(
                    icon = Icons.Filled.SportsEsports,
                    title = "Controls",
                    subtitle = "Choose which controller button maps to each key, and test it",
                    items = 1,
                    index = 0,
                    onClick = onControls,
                )
            }

            SettingsGroup("Display") {
                ChoiceRow(
                    icon = Icons.Filled.Fullscreen,
                    title = "Fill the screen",
                    subtitle = "Fill the entire screen, even if the image is stretched",
                    selected = !keepAspect,
                    items = 2,
                    index = 0,
                    onSelect = {
                        keepAspect = false
                        prefs.keepAspect = false
                        prefs.writeVideo(context.filesDir)
                    },
                )
                ChoiceRow(
                    icon = Icons.Filled.AspectRatio,
                    title = "Keep aspect ratio",
                    subtitle = "Use the classic PC proportions, with dark bars on the sides",
                    selected = keepAspect,
                    items = 2,
                    index = 1,
                    onSelect = {
                        keepAspect = true
                        prefs.keepAspect = true
                        prefs.writeVideo(context.filesDir)
                    },
                )
            }

            SettingsGroup("Screen") {
                SwitchRow(
                    icon = Icons.Filled.LightMode,
                    title = "Keep screen on",
                    subtitle = "Prevent your phone from locking while you play",
                    checked = keepScreenOn,
                    items = 1,
                    index = 0,
                    onCheckedChange = {
                        keepScreenOn = it
                        prefs.keepScreenOn = it
                    },
                )
            }

            SettingsGroup("Updates") {
                NavRow(
                    icon = Icons.Filled.SystemUpdate,
                    title = "App version: $appVersion",
                    subtitle = "Check for available updates",
                    items = 1,
                    index = 0,
                    enabled = !checking,
                    onClick = {
                        checking = true
                        scope.launch {
                            val found = Update.check(appVersion).getOrNull()
                            checking = false
                            if (found != null) {
                                onUpdateFound(found)
                            } else {
                                snackbar.showSnackbar("No updates are available for this build ($appVersion).")
                            }
                        }
                    },
                )
            }

            SettingsGroup("Help") {
                NavRow(
                    icon = Icons.Filled.Article,
                    title = "Logs",
                    subtitle = "Share the logs if something goes wrong; they can help diagnose the problem",
                    items = 2,
                    index = 0,
                    onClick = onLog,
                )
                NavRow(
                    icon = Icons.Filled.Info,
                    title = "About",
                    subtitle = "Version, credits, and notices",
                    items = 2,
                    index = 1,
                    onClick = onAbout,
                )
            }

            Spacer(Modifier.height(BottomNavSpace))
        }
    }
}
