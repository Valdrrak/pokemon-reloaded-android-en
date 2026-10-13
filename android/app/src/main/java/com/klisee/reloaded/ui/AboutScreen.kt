package com.klisee.reloaded.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            Text(
                text = "Pokemon Reloaded",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Text(
                text = "Unofficial Android port of the fangame, built with OpenGMK. It doesn't emulate Windows: it reads the game's data and runs it directly.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            SettingsGroup("Who made it") {
                InfoRow(Icons.Filled.Person, "The game", "Fer Productions", items = 3, index = 0)
                InfoRow(Icons.Filled.Memory, "The engine", "OpenGMK, a Rust reimplementation", items = 3, index = 1)
                InfoRow(Icons.Filled.Favorite, "This port", "Community, non-profit", items = 3, index = 2)
            }

            SettingsGroup("Licenses and notices") {
                InfoRow(Icons.Filled.Brush, "Typeface", "Google Sans Flex, under the OFL license", items = 2, index = 0)
                InfoRow(
                    icon = Icons.Filled.Memory,
                    title = "Trademarks",
                    value = "Pokemon is owned by Nintendo, Game Freak, and Creatures. This project is not affiliated with or endorsed by them.",
                    items = 2,
                    index = 1,
                )
            }

            SettingsGroup("Version") {
                val context = androidx.compose.ui.platform.LocalContext.current
                val version = remember(context) {
                    runCatching {
                        val info = context.packageManager.getPackageInfo(context.packageName, 0)
                        "${info.versionName} (${info.versionCode})"
                    }.getOrDefault("unknown")
                }
                InfoRow(Icons.Filled.Memory, "Launcher", version, items = 1, index = 0)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
