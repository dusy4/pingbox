package com.dusy4.pingbox.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dusy4.pingbox.data.preferences.AppPreferences
import com.dusy4.pingbox.ui.theme.*
import com.dusy4.pingbox.ui.viewmodel.HistoryViewModel
import com.dusy4.pingbox.util.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.java.KoinJavaComponent
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scale = rememberScreenScale()

    val preferences: AppPreferences = KoinJavaComponent.get(AppPreferences::class.java)
    var wasLoggedIn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        preferences.accessToken.collect { token ->
            val isLoggedIn = !token.isNullOrEmpty()
            if (isLoggedIn && !wasLoggedIn) {
                Timber.i("User logged in, syncing history...")
                viewModel.syncHistoryFromServer()
            }
            wasLoggedIn = isLoggedIn
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = CardBackground,
                    contentColor = Foreground,
                    actionColor = Primary,
                    actionContentColor = Primary
                )
            }
        },
        containerColor = Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
        ) {
            // === HEADER ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = scale.headerTopPadding, bottom = scale.headerBottomPadding, start = scale.spacingXL, end = scale.spacingXL),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(scale.spacingXS)) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(scale.iconSizeSmall), tint = MutedForeground)
                        Text(text = "ACTIVITY", style = MaterialTheme.typography.labelMedium, color = MutedForeground)
                    }
                    Spacer(Modifier.height(scale.spacingXS))
                    Text(text = "History", style = MaterialTheme.typography.headlineMedium, color = Foreground)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(scale.spacingS)) {
                    OutlinedButton(
                        onClick = { viewModel.syncHistoryFromServer() },
                        modifier = Modifier.height(scale.buttonHeight),
                        shape = RoundedCornerShape(scale.dp(50f)),
                        border = BorderStroke(scale.dp(1f), Border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Foreground)
                    ) {
                        Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(scale.iconSizeMedium))
                        Spacer(Modifier.width(scale.spacingXS))
                        Text("Sync", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = {
                            ClipboardUtil.copyToClipboard(context, "History", buildString {
                                uiState.notifications.forEach { n ->
                                    appendLine("${n.title} - ${n.body}")
                                }
                            })
                        },
                        modifier = Modifier.height(scale.buttonHeight),
                        shape = RoundedCornerShape(scale.dp(50f)),
                        border = BorderStroke(scale.dp(1f), Border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Foreground)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(scale.iconSizeMedium))
                    }
                }
            }

            // === NOTIFICATIONS LIST ===
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = scale.spacingL),
                verticalArrangement = Arrangement.spacedBy(scale.spacingM)
            ) {
                if (uiState.isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = scale.spacingXXL), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary, modifier = Modifier.size(scale.dp(32f)))
                        }
                    }
                } else if (uiState.notifications.isEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(scale.spacingXXL), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.NotificationsOff, contentDescription = null, modifier = Modifier.size(scale.dp(48f)), tint = MutedForeground.copy(alpha = 0.5f))
                            Spacer(Modifier.height(scale.spacingS))
                            Text(text = "No notifications yet", style = MaterialTheme.typography.bodyMedium, color = MutedForeground.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    items(uiState.notifications, key = { it.id }) { notification ->
                        NotificationCard(notification = notification, onDelete = { viewModel.deleteNotification(notification.id) }, scale = scale)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: com.dusy4.pingbox.data.local.NotificationEntity,
    onDelete: () -> Unit,
    scale: ScreenScale
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(scale.cardCornerRadius),
        color = CardBackground,
        tonalElevation = scale.dp(2f)
    ) {
        Column(modifier = Modifier.padding(scale.spacingL)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(scale.spacingM),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier.size(scale.dp(36f)).clip(CircleShape).background(Muted.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Notifications, contentDescription = null, modifier = Modifier.size(scale.iconSizeMedium), tint = Foreground)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = notification.title, style = MaterialTheme.typography.titleMedium, color = Foreground)
                        if (!notification.body.isNullOrEmpty()) {
                            Text(text = notification.body, style = MaterialTheme.typography.bodySmall, color = MutedForeground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Box {
                    IconButton(onClick = { expanded = true }, modifier = Modifier.size(scale.dp(32f))) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Options", modifier = Modifier.size(scale.iconSizeMedium), tint = MutedForeground)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = Destructive) },
                            onClick = { onDelete(); expanded = false },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Destructive) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(scale.spacingS))

            // Metadata footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeStr = kotlin.runCatching {
                    Instant.fromEpochMilliseconds(notification.receivedAt)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .toString().replace("T", " ").take(19)
                }.getOrNull() ?: "Unknown"

                Text(text = timeStr, style = MaterialTheme.typography.bodySmall, color = MutedForeground.copy(alpha = 0.7f))

                Row(horizontalArrangement = Arrangement.spacedBy(scale.spacingS), verticalAlignment = Alignment.CenterVertically) {
                    if (!notification.tag.isNullOrEmpty()) {
                        Surface(shape = RoundedCornerShape(scale.dp(4f)), color = Muted.copy(alpha = 0.3f)) {
                            Text(text = notification.tag, modifier = Modifier.padding(horizontal = scale.spacingS, vertical = scale.spacingXS), style = MaterialTheme.typography.labelSmall, color = MutedForeground)
                        }
                    }
                    if (!notification.targetType.isNullOrEmpty() && notification.targetType != "none") {
                        Surface(shape = RoundedCornerShape(scale.dp(4f)), color = Primary.copy(alpha = 0.1f)) {
                            Text(text = notification.targetType, modifier = Modifier.padding(horizontal = scale.spacingS, vertical = scale.spacingXS), style = MaterialTheme.typography.labelSmall, color = Foreground)
                        }
                    }
                }
            }
        }
    }
}
