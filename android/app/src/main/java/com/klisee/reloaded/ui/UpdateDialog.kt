package com.klisee.reloaded.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.klisee.reloaded.Update

/**
 * Offers the newer build, and leaves a way out for anyone who would rather not trust an in-app
 * updater: that last button opens the release page, so the APK can be fetched by hand.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateDialog(
    release: Update.Release,
    busy: Boolean,
    written: Long,
    total: Long,
    problem: String?,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
    onOpenReleases: () -> Unit,
) {
    val percent = if (total > 0) (written * 100 / total).toInt() else 0
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
        title = { Text(if (busy) "Downloading update" else "A new version is available") },
        text = {
            Column {
                Text("Version ${release.version}")
                if (busy) {
                    LinearWavyProgressIndicator(
                        progress = { if (total > 0) written.toFloat() / total.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${inMegabytes(written)} of ${if (total > 0) inMegabytes(total) else "?"} MB",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (problem == null && !busy) {
                    Text(
                        text = "It downloads and installs itself, without going through the browser. Android will ask for permission the first time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (problem != null) {
                    Text(
                        text = problem,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                TextButton(onClick = onOpenReleases, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "I don't trust the auto-updater",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload, enabled = !busy) {
                Text(if (busy) "Downloading..." else "Download")
            }
        },
        dismissButton = {
            // "Ocultar" while it works, "Cancelar" while it waits: the same thing to the user, but
            // it says what it does at each moment.
            TextButton(onClick = onDismiss, enabled = !busy) { Text(if (busy) "Hide" else "Cancel") }
        },
    )
}

private fun inMegabytes(bytes: Long) = "%.1f".format(bytes / 1024.0 / 1024.0)
