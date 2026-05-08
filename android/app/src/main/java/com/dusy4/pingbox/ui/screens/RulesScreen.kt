package com.dusy4.pingbox.ui.screens

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dusy4.pingbox.data.local.RuleEntity
import com.dusy4.pingbox.data.preferences.AppPreferences
import com.dusy4.pingbox.data.remote.RuleCreateRequest
import com.dusy4.pingbox.ui.theme.*
import com.dusy4.pingbox.ui.viewmodel.RulesViewModel
import com.dusy4.pingbox.ui.viewmodel.SyncStatus
import com.dusy4.pingbox.util.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.java.KoinJavaComponent
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RulesScreen(
    viewModel: RulesViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scale = rememberScreenScale()

    val preferences: AppPreferences = KoinJavaComponent.get(AppPreferences::class.java)
    var wasLoggedIn by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<RuleEntity?>(null) }

    // Sync on first load
    LaunchedEffect(Unit) {
        viewModel.syncRulesFromServer()
    }

    // Re-sync when user becomes logged in
    LaunchedEffect(Unit) {
        preferences.accessToken.collect { token ->
            val isLoggedIn = !token.isNullOrEmpty()
            if (isLoggedIn && !wasLoggedIn) {
                Timber.i("User logged in, syncing rules...")
                viewModel.syncRulesFromServer()
            }
            wasLoggedIn = isLoggedIn
        }
    }

    LaunchedEffect(uiState.syncStatus) {
        when (val status = uiState.syncStatus) {
            is SyncStatus.Success -> {
                snackbarHostState.showSnackbar(status.message)
                viewModel.clearSyncStatus()
            }
            is SyncStatus.Error -> {
                snackbarHostState.showSnackbar(status.message)
                viewModel.clearSyncStatus()
            }
            else -> Unit
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar("Error: $error")
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
                .padding(paddingValues)
                .background(Background)
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
                        Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(scale.iconSizeSmall), tint = MutedForeground)
                        Text(text = "ROUTING", style = MaterialTheme.typography.labelMedium, color = MutedForeground)
                    }
                    Spacer(Modifier.height(scale.spacingXS))
                    Text(text = "Rules", style = MaterialTheme.typography.headlineMedium, color = Foreground)
                }

                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Primary,
                    contentColor = PrimaryForeground,
                    shape = CircleShape,
                    modifier = Modifier.size(scale.quickActionButtonSize + scale.spacingM)
                ) {
                    Icon(Icons.Filled.AddCircle, contentDescription = "Add Rule", modifier = Modifier.size(scale.iconSizeLarge))
                }
            }

            // === RULES LIST ===
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = scale.spacingL),
                verticalArrangement = Arrangement.spacedBy(scale.spacingM)
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = scale.spacingXXL), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(scale.dp(32f)))
                    }
                } else if (uiState.rules.isEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(scale.spacingXXL), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AltRoute, contentDescription = null, modifier = Modifier.size(scale.dp(48f)), tint = MutedForeground.copy(alpha = 0.5f))
                        Spacer(Modifier.height(scale.spacingS))
                        Text(text = "Add rules to route notifications", style = MaterialTheme.typography.bodyMedium, color = MutedForeground.copy(alpha = 0.5f))
                    }
                } else {
                    uiState.rules.forEach { rule ->
                        RuleCard(
                            rule = rule,
                            onDelete = { viewModel.deleteRule(rule) },
                            onEdit = { editingRule = rule; showAddDialog = true },
                            scale = scale
                        )
                    }
                    Spacer(Modifier.height(scale.spacingM))
                }
            }
        }
    }

    // === ADD/EDIT RULE DIALOG ===
    if (showAddDialog) {
        AddEditRuleDialog(
            existingRule = editingRule,
            onDismiss = { showAddDialog = false; editingRule = null },
            onSave = { ruleCreateRequest ->
                val ruleBeingEdited = editingRule
                if (ruleBeingEdited != null) {
                    val updatedRule = ruleBeingEdited.copy(
                        name = ruleCreateRequest.name,
                        matchTag = ruleCreateRequest.matchTag,
                        targetType = ruleCreateRequest.targetType,
                        targetValue = ruleCreateRequest.targetValue,
                        priority = ruleCreateRequest.priority,
                        enabled = ruleCreateRequest.enabled
                    )
                    viewModel.updateRule(updatedRule) { success ->
                        if (!success) {
                            scope.launch { snackbarHostState.showSnackbar("Failed to update rule") }
                        }
                    }
                } else {
                    viewModel.addRule(ruleCreateRequest) { success ->
                        if (!success) {
                            scope.launch { snackbarHostState.showSnackbar("Failed to save rule") }
                        }
                    }
                }
                showAddDialog = false
                editingRule = null
            }
        )
    }
}

@Composable
private fun RuleCard(
    rule: RuleEntity,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    scale: ScreenScale
) {
    val targetIcon = when (rule.targetType) {
        "url" -> Icons.Outlined.Link
        "deeplink" -> Icons.Outlined.OpenInNew
        "package" -> Icons.Outlined.Apps
        "component" -> Icons.Outlined.Widgets
        "intent_uri" -> Icons.Outlined.Code
        else -> Icons.Outlined.Link
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(scale.cardCornerRadius),
        color = CardBackground,
        tonalElevation = scale.dp(4f),
        shadowElevation = scale.dp(4f)
    ) {
        Column(modifier = Modifier.padding(scale.spacingL)) {
            // Top row: icon + name + priority + enabled
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
                        Icon(targetIcon, contentDescription = null, modifier = Modifier.size(scale.iconSizeMedium), tint = Foreground)
                    }
                    Column {
                        Text(text = rule.name, style = MaterialTheme.typography.titleMedium, color = Foreground)
                        if (!rule.matchTag.isNullOrEmpty()) {
                            Text(text = "tag: ${rule.matchTag}", style = MaterialTheme.typography.bodySmall, color = MutedForeground)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(scale.spacingS), verticalAlignment = Alignment.CenterVertically) {
                    if (rule.priority > 0) {
                        Surface(shape = RoundedCornerShape(scale.dp(4f)), color = Warning.copy(alpha = 0.2f)) {
                            Text(text = "HIGH", modifier = Modifier.padding(horizontal = scale.spacingS, vertical = scale.spacingXS), style = MaterialTheme.typography.labelSmall, color = Warning)
                        }
                    }
                    if (!rule.enabled) {
                        Surface(shape = RoundedCornerShape(scale.dp(4f)), color = Muted.copy(alpha = 0.3f)) {
                            Text(text = "OFF", modifier = Modifier.padding(horizontal = scale.spacingS, vertical = scale.spacingXS), style = MaterialTheme.typography.labelSmall, color = MutedForeground)
                        }
                    } else {
                        Box(modifier = Modifier.size(scale.dp(8f)).clip(CircleShape).background(StatusGreen))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(scale.dp(32f))) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(scale.iconSizeSmall), tint = Primary)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(scale.dp(32f))) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete", modifier = Modifier.size(scale.iconSizeSmall), tint = Destructive)
                    }
                }
            }

            Spacer(Modifier.height(scale.spacingM))

            // Target footer
            Surface(shape = RoundedCornerShape(scale.dp(16f)), color = Background.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(scale.spacingM),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(scale.spacingM), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(targetIcon, contentDescription = null, modifier = Modifier.size(scale.iconSizeSmall), tint = MutedForeground)
                        Text(text = rule.targetValue.ifEmpty { "(none)" }, style = MaterialTheme.typography.bodySmall, color = MutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, modifier = Modifier.size(scale.iconSizeSmall), tint = MutedForeground)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditRuleDialog(
    existingRule: RuleEntity?,
    onDismiss: () -> Unit,
    onSave: (RuleCreateRequest) -> Unit
) {
    val context = LocalContext.current
    val scale = rememberScreenScale()
    val isEditing = existingRule != null

    var name by remember { mutableStateOf(existingRule?.name ?: "") }
    var matchTag by remember { mutableStateOf(existingRule?.matchTag ?: "") }
    var targetType by remember { mutableStateOf(existingRule?.targetType ?: "url") }
    var targetValue by remember { mutableStateOf(existingRule?.targetValue ?: "") }
    var highPriority by remember { mutableStateOf(existingRule?.let { it.priority > 0 } ?: false) }
    var enabled by remember { mutableStateOf(existingRule?.enabled ?: true) }

    var showPackagePicker by remember { mutableStateOf(false) }

    val targetTypes = listOf("url", "package", "deeplink", "component", "intent_uri", "none")
    val placeholderText = when (targetType) {
        "url" -> "https://example.com"
        "package" -> "com.google.android.youtube"
        "deeplink" -> "spotify://track/123"
        "component" -> "com.app/.MainActivity"
        "intent_uri" -> "intent://host#Intent;scheme=..."
        "none" -> "(not applicable)"
        else -> ""
    }

    // Use ModalBottomSheet for better native UX
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Background,
        contentColor = Foreground,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = scale.spacingM),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(scale.dp(32f))
                        .height(scale.dp(4f))
                        .clip(RoundedCornerShape(scale.dp(2f)))
                        .background(Muted.copy(alpha = 0.5f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = scale.spacingL, vertical = scale.spacingM)
                .padding(bottom = scale.spacingXXL),
            verticalArrangement = Arrangement.spacedBy(scale.spacingL)
        ) {
            // === Header ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Rule" else "New Rule",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Foreground
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(scale.spacingXS)
                ) {
                    Text(
                        text = if (enabled) "ON" else "OFF",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (enabled) StatusGreen else MutedForeground
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = StatusGreen,
                            checkedThumbColor = Foreground,
                            uncheckedTrackColor = Muted,
                            uncheckedThumbColor = MutedForeground
                        )
                    )
                }
            }

            HorizontalDivider(color = Border)

            // === Rule Name ===
            Column(verticalArrangement = Arrangement.spacedBy(scale.spacingXS)) {
                Text(text = "Rule Name", style = MaterialTheme.typography.labelLarge, color = MutedForeground)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. GitHub alerts", color = MutedForeground.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Foreground),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Border,
                        focusedTextColor = Foreground,
                        unfocusedTextColor = Foreground,
                        cursorColor = Primary,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = MutedForeground
                    ),
                    shape = RoundedCornerShape(scale.dp(12f))
                )
            }

            // === Match Tag ===
            Column(verticalArrangement = Arrangement.spacedBy(scale.spacingXS)) {
                Text(text = "Match Tag", style = MaterialTheme.typography.labelLarge, color = MutedForeground)
                OutlinedTextField(
                    value = matchTag,
                    onValueChange = { matchTag = it },
                    placeholder = { Text("github-* (optional)", color = MutedForeground.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Tag, contentDescription = null, tint = MutedForeground, modifier = Modifier.size(scale.iconSizeMedium))
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Foreground),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Border,
                        focusedTextColor = Foreground,
                        unfocusedTextColor = Foreground,
                        cursorColor = Primary,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = MutedForeground
                    ),
                    shape = RoundedCornerShape(scale.dp(12f))
                )
            }

            // === Target Type ===
            Column(verticalArrangement = Arrangement.spacedBy(scale.spacingS)) {
                Text(text = "Target Type", style = MaterialTheme.typography.labelLarge, color = MutedForeground)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(scale.spacingS)
                ) {
                    targetTypes.forEach { type ->
                        val isSelected = targetType == type
                        val icon = when (type) {
                            "url" -> Icons.Outlined.Link
                            "package" -> Icons.Outlined.Apps
                            "deeplink" -> Icons.Outlined.OpenInNew
                            "component" -> Icons.Outlined.Widgets
                            "intent_uri" -> Icons.Outlined.Code
                            "none" -> Icons.Outlined.Block
                            else -> Icons.Outlined.Link
                        }
                        // Custom filter chip since FilterChip colors API varies by version
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { targetType = type },
                            shape = RoundedCornerShape(scale.dp(20f)),
                            color = if (isSelected) Primary else Muted.copy(alpha = 0.3f),
                            border = if (isSelected) null else BorderStroke(scale.dp(1f), Border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = scale.spacingS, vertical = scale.spacingS),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(scale.iconSizeSmall),
                                    tint = if (isSelected) PrimaryForeground else MutedForeground
                                )
                                Spacer(Modifier.width(scale.spacingXS))
                                Text(
                                    text = type.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) PrimaryForeground else MutedForeground
                                )
                            }
                        }
                    }
                }
            }

            // === Target Value ===
            if (targetType != "none") {
                Column(verticalArrangement = Arrangement.spacedBy(scale.spacingXS)) {
                    Text(text = "Target Value", style = MaterialTheme.typography.labelLarge, color = MutedForeground)
                    if (targetType == "package" || targetType == "component") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(scale.spacingS),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = targetValue,
                                onValueChange = { targetValue = it },
                                placeholder = { Text(placeholderText, color = MutedForeground.copy(alpha = 0.5f)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Foreground),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = Border,
                                    focusedTextColor = Foreground,
                                    unfocusedTextColor = Foreground,
                                    cursorColor = Primary,
                                    focusedLabelColor = Primary,
                                    unfocusedLabelColor = MutedForeground
                                ),
                                shape = RoundedCornerShape(scale.dp(12f))
                            )
                            OutlinedButton(
                                onClick = { showPackagePicker = true },
                                modifier = Modifier.height(scale.buttonHeight),
                                shape = RoundedCornerShape(scale.dp(12f)),
                                border = BorderStroke(scale.dp(1f), Border),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Foreground)
                            ) {
                                Icon(Icons.Outlined.Search, contentDescription = "Browse", modifier = Modifier.size(scale.iconSizeMedium))
                                Spacer(Modifier.width(scale.spacingXS))
                                Text(if (targetType == "package") "Apps" else "Activities", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = targetValue,
                            onValueChange = { targetValue = it },
                            placeholder = { Text(placeholderText, color = MutedForeground.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Foreground),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = Border,
                                focusedTextColor = Foreground,
                                unfocusedTextColor = Foreground,
                                cursorColor = Primary,
                                focusedLabelColor = Primary,
                                unfocusedLabelColor = MutedForeground
                            ),
                            shape = RoundedCornerShape(scale.dp(12f))
                        )
                    }
                }
            }

            // === Priority ===
            Column(verticalArrangement = Arrangement.spacedBy(scale.spacingXS)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Priority", style = MaterialTheme.typography.labelLarge, color = MutedForeground)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(scale.spacingS)) {
                        Text(
                            text = if (highPriority) "High" else "Normal",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (highPriority) Warning else MutedForeground
                        )
                        Switch(
                            checked = highPriority,
                            onCheckedChange = { highPriority = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Warning,
                                checkedThumbColor = Foreground,
                                uncheckedTrackColor = Muted,
                                uncheckedThumbColor = MutedForeground
                            )
                        )
                    }
                }
            }

            // === Save Button ===
            Button(
                onClick = {
                    if (name.isEmpty()) return@Button
                    Timber.i("Saving rule: $name")
                    onSave(
                        RuleCreateRequest(
                            name = name,
                            matchTag = matchTag.ifEmpty { null },
                            targetType = targetType,
                            targetValue = if (targetType == "none") "" else targetValue,
                            priority = if (highPriority) 5 else 0,
                            enabled = enabled
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = scale.buttonHeight),
                shape = RoundedCornerShape(scale.dp(50f)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = PrimaryForeground,
                    disabledContainerColor = Muted,
                    disabledContentColor = MutedForeground
                ),
                enabled = name.isNotEmpty()
            ) {
                Text(
                    text = if (isEditing) "Update Rule" else "Create Rule",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryForeground
                )
            }
        }
    }

    // === PACKAGE PICKER DIALOG ===
    if (showPackagePicker) {
        PackagePickerDialog(
            targetType = targetType,
            onDismiss = { showPackagePicker = false },
            onSelected = { value ->
                targetValue = value
                showPackagePicker = false
            },
            scale = scale
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackagePickerDialog(
    targetType: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
    scale: ScreenScale
) {
    val context = LocalContext.current
    val pm = context.packageManager

    data class PickerItem(val label: String, val value: String, val icon: Drawable?)

    val items = remember {
        if (targetType == "package") {
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .map { resolveInfo ->
                    val label = resolveInfo.loadLabel(pm).toString()
                    val pkg = resolveInfo.activityInfo.packageName
                    val icon = resolveInfo.loadIcon(pm)
                    PickerItem(label, pkg, icon)
                }
                .sortedBy { it.label }
        } else {
            val packages = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES)
            val activities = mutableListOf<PickerItem>()
            packages.forEach { pkgInfo ->
                pkgInfo.activities?.forEach { activityInfo ->
                    if (activityInfo.exported) {
                        val label = activityInfo.loadLabel(pm).toString().takeIf { it.isNotEmpty() } ?: "${activityInfo.packageName}/${activityInfo.name}"
                        val component = "${activityInfo.packageName}/${activityInfo.name}"
                        val icon = try {
                            activityInfo.loadIcon(pm)
                        } catch (e: Exception) {
                            null
                        }
                        activities.add(PickerItem(label, component, icon))
                    }
                }
            }
            activities.distinctBy { it.value }.sortedBy { it.label }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = items.filter { it.label.contains(searchQuery, ignoreCase = true) || it.value.contains(searchQuery, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Background,
        contentColor = Foreground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = scale.spacingM),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(scale.dp(32f))
                        .height(scale.dp(4f))
                        .clip(RoundedCornerShape(scale.dp(2f)))
                        .background(Muted.copy(alpha = 0.5f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = scale.spacingL, vertical = scale.spacingM)
                .padding(bottom = scale.spacingXXL)
        ) {
            Text(
                text = if (targetType == "package") "Choose App" else "Choose Activity",
                style = MaterialTheme.typography.headlineSmall,
                color = Foreground
            )
            Spacer(Modifier.height(scale.spacingM))

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search...", color = MutedForeground.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Foreground),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border,
                    focusedTextColor = Foreground,
                    unfocusedTextColor = Foreground,
                    cursorColor = Primary
                ),
                shape = RoundedCornerShape(scale.dp(12f)),
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = MutedForeground, modifier = Modifier.size(scale.iconSizeMedium)) }
            )

            Spacer(Modifier.height(scale.spacingM))

            // List
            LazyColumn(
                modifier = Modifier.heightIn(max = scale.dp(400f)),
                verticalArrangement = Arrangement.spacedBy(scale.spacingXS)
            ) {
                items(filteredItems) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(item.value) }
                            .clip(RoundedCornerShape(scale.dp(12f)))
                            .background(Muted.copy(alpha = 0.2f))
                            .padding(scale.spacingM),
                        horizontalArrangement = Arrangement.spacedBy(scale.spacingM),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App/Activity icon
                        if (item.icon != null) {
                            androidx.compose.foundation.Image(
                                bitmap = item.icon.toBitmap().asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(scale.dp(32f))
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(scale.dp(32f)).clip(CircleShape).background(Muted.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Apps, contentDescription = null, modifier = Modifier.size(scale.iconSizeMedium), tint = MutedForeground)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.label, style = MaterialTheme.typography.bodyMedium, color = Foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = item.value, style = MaterialTheme.typography.bodySmall, color = MutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
