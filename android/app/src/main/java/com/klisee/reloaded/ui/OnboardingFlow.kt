package com.klisee.reloaded.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.klisee.reloaded.Blog
import com.klisee.reloaded.GameRepository
import com.klisee.reloaded.Preferences
import kotlinx.coroutines.launch

/**
 * First run only. The reference apps open with a short, purposeful setup, and this one has a real
 * job to do: get the game onto the device before the user reaches a disabled "Jugar".
 */
@Composable
fun Onboarding(
    repo: GameRepository,
    prefs: Preferences,
    onFinish: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var step by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf(repo.status()) }
    var post by remember { mutableStateOf<Blog.Post?>(null) }
    var keepScreenOn by remember { mutableStateOf(prefs.keepScreenOn) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { post = Blog.latest().getOrNull() }
    LaunchedEffect(step) { status = repo.status() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            repo.import(uri) { progress = it }
            busy = false
            progress = null
            status = repo.status()
        }
    }

    val installed = status as? GameRepository.Status.Ready
    val downloadUrl = post?.postUrl ?: post?.links?.firstOrNull()?.url
    val postVersion = post?.version

    fun finish() {
        prefs.onboardingDone = true
        onFinish()
    }

    when (step) {
        0 -> OnboardingScaffold(
            step = 0,
            stepCount = 3,
            title = "Pokemon Reloaded for Android",
            subtitle = buildAnnotatedString {
                append(
                    "A community-made port by @kliseex, built for the community. It runs on " +
                        "OpenGMK, a native reimplementation of the Game Maker 8 engine, so the game " +
                        "runs directly without emulated Windows and performs much like a native game.\n\n" +
                        "This is still a beta, and it may contain bugs or crash. If you run into any " +
                        "issue, please save and share a log.\n\n" +
                        "Report bugs, crashes, or freezes at ",
                )
                withLink(LinkAnnotation.Url("https://github.com/Klisee/pokemon-reloaded-android")) {
                    append("GitHub")
                }
                append(".")
            },
            continueLabel = "Get started",
            continueEnabled = true,
            onContinue = { step = 1 },
            hero = { OnboardingHero("WELCOME") },
        )

        1 -> OnboardingScaffold(
            step = 1,
            stepCount = 3,
            title = "Before you start",
            subtitle = buildAnnotatedString {
                append(
                    "To play this Android port, you need the original game files from ",
                )
                withLink(LinkAnnotation.Url("https://pokemon-reloaded.blogspot.com/")) {
                    append("F3R")
                }
                append(
                    ". This APK does not include game assets, which keeps the project open source.\n\n" +
                        "To get started, open the blog's download page, get the game as a .zip, then " +
                        "choose Import ZIP.",
                )
            },
            continueLabel = "Continue",
            continueEnabled = installed != null,
            onContinue = { step = 2 },
            onBack = { step = 0 },
            hint = if (installed == null) "Install the game to continue" else "Ready: ${installed.exe}",
        ) {
            BigActionRow(
                icon = Icons.Filled.Download,
                title = "Download from the blog",
                subtitle = if (postVersion != null) "Version $postVersion" else "Open the download page",
                items = 2,
                index = 0,
                enabled = downloadUrl != null && !busy,
                onClick = { downloadUrl?.let { openInBrowser(context, it) } },
            )
            BigActionRow(
                icon = Icons.Filled.FolderOpen,
                title = "Import a ZIP",
                subtitle = "Choose an existing file",
                items = 2,
                index = 1,
                enabled = !busy,
                onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
            )
            if (busy) {
                LinearWavyProgressIndicator(progress = { (progress ?: 0) / 100f }, modifier = Modifier.fillMaxWidth())
            }
        }

        else -> OnboardingScaffold(
            step = 2,
            stepCount = 3,
            title = "How would you like to play?",
            subtitle = AnnotatedString("You can change this anytime in Settings."),
            continueLabel = "Done",
            continueEnabled = true,
            onContinue = { finish() },
            onBack = { step = 1 },
        ) {
            SwitchRow(
                icon = Icons.Filled.LightMode,
                title = "Keep screen on",
                subtitle = "Prevent your phone from locking during a long battle",
                checked = keepScreenOn,
                items = 1,
                index = 0,
                onCheckedChange = {
                    keepScreenOn = it
                    prefs.keepScreenOn = it
                },
            )
            Text(
                text = "Touch controls appear automatically: a joystick and buttons in the overworld and in battle, and none in menus, which you can tap directly.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BigActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    items: Int,
    index: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    GroupListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodyMedium) },
        leadingContent = { Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp)) },
        items = items,
        index = index,
        onClick = if (enabled) onClick else null,
    )
}
