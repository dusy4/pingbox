package com.dusy4.pingbox.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Patterns
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dusy4.pingbox.ui.theme.*
import com.dusy4.pingbox.ui.viewmodel.SettingsEvent
import com.dusy4.pingbox.ui.viewmodel.SettingsViewModel
import com.dusy4.pingbox.util.*
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onNavigateToDebug: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scale = rememberScreenScale()

    // Show orphaned key warning on first detection
    LaunchedEffect(uiState.hasOrphanedKey) {
        if (uiState.hasOrphanedKey) {
            snackbarHostState.showSnackbar("API key from previous session found. Log in to generate a new key.")
            viewModel.clearOrphanedKey()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    var showAuthDialog by remember { mutableStateOf(false) }
    var showLogoutMenu by remember { mutableStateOf(false) }
    var deviceNameInput by remember { mutableStateOf(uiState.deviceName) }
    LaunchedEffect(uiState.deviceName) {
        deviceNameInput = uiState.deviceName
    }

    fun handleEvent(event: SettingsEvent) {
        scope.launch {
            when (event) {
                is SettingsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                SettingsEvent.ApiKeyGenerated -> snackbarHostState.showSnackbar("API key generated")
                SettingsEvent.LoggedIn -> {
                    snackbarHostState.showSnackbar("Logged in as ${uiState.userEmail}")
                    // Auto-generate API key after login/register
                    viewModel.generateApiKey { genEvent ->
                        if (genEvent is SettingsEvent.ShowSnackbar) {
                            scope.launch { snackbarHostState.showSnackbar(genEvent.message) }
                        }
                    }
                }
                SettingsEvent.LoggedOut -> snackbarHostState.showSnackbar("Logged out")
                SettingsEvent.OrphanedKeyCleared -> {}
            }
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = scale.sectionPadding)
        ) {
            // === HEADER ===
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = scale.headerTopPadding, bottom = scale.headerBottomPadding, start = scale.spacingXL, end = scale.spacingXL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(scale.spacingS)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(scale.spacingXS)
                ) {
                    Icon(
                        Icons.Outlined.Inbox,
                        contentDescription = "PingBox logo",
                        modifier = Modifier.size(scale.iconSizeSmall),
                        tint = MutedForeground
                    )
                    Text(text = "PINGBOX", style = MaterialTheme.typography.labelMedium, color = MutedForeground)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(scale.spacingM)
                ) {
                    Box(
                        modifier = Modifier.size(scale.dp(40f)).clip(CircleShape).background(CardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(scale.iconSizeMedium),
                            tint = Foreground
                        )
                    }
                    Text(text = "Settings", style = MaterialTheme.typography.headlineMedium, color = Foreground)
                }
                if (uiState.isLoggedIn) {
                    Surface(
                        shape = RoundedCornerShape(scale.dp(50f)),
                        color = CardBackground,
                        modifier = Modifier
                            .padding(top = scale.spacingXS)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { showLogoutMenu = true }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = scale.spacingM, vertical = scale.spacingXS),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(scale.spacingXS)
                        ) {
                            Box(
                                modifier = Modifier.size(scale.dp(8f)).clip(CircleShape).background(StatusGreen)
                            )
                            Text(
                                text = uiState.userEmail.ifEmpty { "Logged in" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedForeground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    // "Not logged in" status bar - clickable to open login dialog
                    Surface(
                        shape = RoundedCornerShape(scale.dp(50f)),
                        color = CardBackground,
                        modifier = Modifier
                            .padding(top = scale.spacingXS)
                            .clickable { showAuthDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = scale.spacingM, vertical = scale.spacingXS),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(scale.spacingXS)
                        ) {
                            Box(
                                modifier = Modifier.size(scale.dp(8f)).clip(CircleShape).background(MutedForeground)
                            )
                            Text(
                                text = "Not logged in",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedForeground
                            )
                            Spacer(Modifier.width(scale.spacingXS))
                            Icon(
                                Icons.Outlined.Login,
                                contentDescription = "Log in",
                                modifier = Modifier.size(scale.iconSizeSmall),
                                tint = Primary
                            )
                        }
                    }
                }
            }

            // === QUICK ACTION BUTTONS ===
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = scale.spacingXL).padding(bottom = scale.sectionPadding),
                horizontalArrangement = Arrangement.Center
            ) {
                QuickActionButton(
                    icon = Icons.Outlined.ContentCopy,
                    label = "Copy\nAPI Key",
                    contentDesc = "Copy API key to clipboard",
                    tint = Foreground,
                    bgColor = CardBackground,
                    scale = scale,
                    onClick = {
                        if (uiState.apiKey.isNotEmpty()) copyToClipboard("API Key", uiState.apiKey)
                        else scope.launch { snackbarHostState.showSnackbar("No API key to copy") }
                    }
                )
                Spacer(Modifier.width(scale.quickActionSpacing))
                QuickActionButton(
                    icon = Icons.Outlined.Key,
                    label = "Generate\nNew Key",
                    contentDesc = "Generate new API key",
                    tint = Foreground,
                    bgColor = CardBackground,
                    scale = scale,
                    onClick = { viewModel.generateApiKey(::handleEvent) }
                )
                Spacer(Modifier.width(scale.quickActionSpacing))
                if (uiState.isLoggedIn) {
                    QuickActionButton(
                        icon = Icons.Outlined.Logout,
                        label = "Log\nOut",
                        contentDesc = "Log out of account",
                        tint = Destructive,
                        bgColor = DestructiveDim,
                        scale = scale,
                        onClick = {
                            viewModel.logout()
                            handleEvent(SettingsEvent.LoggedOut)
                        }
                    )
                }
            }

            // === SETTINGS CARD ===
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = scale.spacingL),
                shape = RoundedCornerShape(scale.cardCornerRadius),
                color = CardBackground,
                tonalElevation = scale.dp(4f),
                shadowElevation = scale.dp(8f)
            ) {
                Column(
                    modifier = Modifier.padding(scale.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(scale.spacingS)
                ) {
                    SettingsInputRow(
                        icon = Icons.Outlined.Dns,
                        iconDesc = "Server",
                        label = "Server URL",
                        value = uiState.serverUrl,
                        onValueChange = { },
                        placeholder = "Server URL",
                        readOnly = true,
                        onLongPress = { copyToClipboard("Server URL", uiState.serverUrl) },
                        scale = scale
                    )
                    SettingsInputRow(
                        icon = Icons.Outlined.Password,
                        iconDesc = "API key",
                        label = "Active API Key",
                        value = uiState.apiKey.ifEmpty { "No API key" },
                        onValueChange = {},
                        placeholder = "No API key",
                        readOnly = true,
                        dimText = uiState.apiKey.isEmpty(),
                        onLongPress = { if (uiState.apiKey.isNotEmpty()) copyToClipboard("API Key", uiState.apiKey) },
                        scale = scale
                    )
                    SettingsInputRow(
                        icon = Icons.Outlined.Smartphone,
                        iconDesc = "Device",
                        label = "Device Identifier",
                        value = deviceNameInput,
                        onValueChange = { deviceNameInput = it },
                        placeholder = "Enter device name",
                        scale = scale
                    )
                }
            }

            // === SAVE BUTTON ===
            Button(
                onClick = {
                    viewModel.saveConfiguration(deviceNameInput, ::handleEvent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = scale.spacingL, vertical = scale.sectionPadding)
                    .heightIn(min = scale.buttonHeight),
                shape = RoundedCornerShape(scale.dp(50f)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = PrimaryForeground,
                    disabledContainerColor = Muted,
                    disabledContentColor = MutedForeground
                ),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = PrimaryForeground,
                        modifier = Modifier.size(scale.iconSizeMedium),
                        strokeWidth = scale.dp(2f)
                    )
                    Spacer(Modifier.width(scale.spacingS))
                }
                Text(
                    text = "Save Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.isLoading) PrimaryForeground.copy(alpha = 0.7f) else PrimaryForeground
                )
            }

            // === DEBUG MENU TOGGLE ===
            Text(
                text = "Debug Menu",
                style = MaterialTheme.typography.labelSmall,
                color = MutedForeground.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = scale.spacingXXL)
                    .clickable { onNavigateToDebug() }
                    .padding(scale.spacingL),
                textAlign = TextAlign.Center
            )
        }
    }

    // === LOGOUT MENU (long-press on status bar) ===
    if (showLogoutMenu && uiState.isLoggedIn) {
        DropdownMenu(
            expanded = showLogoutMenu,
            onDismissRequest = { showLogoutMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Log Out", color = Destructive) },
                onClick = {
                    viewModel.logout()
                    showLogoutMenu = false
                    handleEvent(SettingsEvent.LoggedOut)
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Logout, contentDescription = null, tint = Destructive)
                }
            )
        }
    }

    // === AUTH DIALOG ===
    if (showAuthDialog) {
        AuthDialog(
            viewModel = viewModel,
            onDismiss = { showAuthDialog = false },
            onAuthSuccess = { showAuthDialog = false }
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    contentDesc: String,
    tint: Color,
    bgColor: Color,
    scale: ScreenScale,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = scale.quickActionButtonSize + scale.spacingXL)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
    ) {
        Box(
            modifier = Modifier
                .size(scale.quickActionButtonSize)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                modifier = Modifier.size(scale.quickActionButtonIconSize),
                tint = tint
            )
        }
        Spacer(Modifier.height(scale.spacingS))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedForeground,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.labelSmall.lineHeight
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsInputRow(
    icon: ImageVector,
    iconDesc: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    readOnly: Boolean = false,
    dimText: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    scale: ScreenScale
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(scale.dp(20f)))
            .background(Background.copy(alpha = 0.4f))
            .then(if (onLongPress != null) Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress) else Modifier)
            .padding(scale.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(scale.spacingM)
    ) {
        Box(
            modifier = Modifier.size(scale.dp(40f)).clip(CircleShape).background(Muted.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconDesc,
                modifier = Modifier.size(scale.iconSizeMedium),
                tint = MutedForeground
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MutedForeground)
            Spacer(Modifier.height(scale.spacingXS))
            if (readOnly) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (dimText) Foreground.copy(alpha = 0.5f) else Foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Foreground),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = scale.spacingXS),
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedForeground.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scale = rememberScreenScale()
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    fun validateInputs(): Boolean {
        if (email.isEmpty() || password.isEmpty()) {
            errorMessage = "Email and password required"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage = "Invalid email address"
            return false
        }
        if (password.length < 8) {
            errorMessage = "Password must be at least 8 characters"
            return false
        }
        return true
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage = it }
    }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = scale.spacingL, vertical = scale.spacingM)
                .padding(bottom = scale.spacingXXL),
            verticalArrangement = Arrangement.spacedBy(scale.spacingL)
        ) {
            // === Header ===
            Text(
                text = if (isLogin) "Welcome Back" else "Create Account",
                style = MaterialTheme.typography.headlineSmall,
                color = Foreground
            )

            // === Login/Register Toggle ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(scale.spacingS)
            ) {
                Button(
                    onClick = { isLogin = true; errorMessage = "" },
                    modifier = Modifier.weight(1f).height(scale.buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLogin) Primary else Muted,
                        contentColor = if (isLogin) PrimaryForeground else MutedForeground
                    ),
                    shape = RoundedCornerShape(scale.dp(12f))
                ) {
                    Text(
                        "Login",
                        color = if (isLogin) PrimaryForeground else MutedForeground,
                        modifier = Modifier.background(
                            color = if (isLogin) Primary else Color.Transparent,
                            shape = RoundedCornerShape(scale.dp(12f))
                        )
                    )
                }
                Button(
                    onClick = { isLogin = false; errorMessage = "" },
                    modifier = Modifier.weight(1f).height(scale.buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isLogin) Primary else Muted,
                        contentColor = if (!isLogin) PrimaryForeground else MutedForeground
                    ),
                    shape = RoundedCornerShape(scale.dp(12f))
                ) {
                    Text(
                        "Register",
                        color = if (!isLogin) PrimaryForeground else MutedForeground,
                        modifier = Modifier.background(
                            color = if (!isLogin) Primary else Color.Transparent,
                            shape = RoundedCornerShape(scale.dp(12f))
                        )
                    )
                }
            }

            // === Email Field ===
            Column(verticalArrangement = Arrangement.spacedBy(scale.spacingXS)) {
                Text(text = "Email", style = MaterialTheme.typography.labelLarge, color = MutedForeground)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = "" },
                    placeholder = { Text("you@example.com", color = MutedForeground.copy(alpha = 0.5f)) },
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
                    shape = RoundedCornerShape(scale.dp(12f)),
                    leadingIcon = {
                        Icon(Icons.Outlined.Email, contentDescription = null, tint = MutedForeground, modifier = Modifier.size(scale.iconSizeMedium))
                    }
                )
            }

            // === Password Field ===
            Column(verticalArrangement = Arrangement.spacedBy(scale.spacingXS)) {
                Text(text = "Password", style = MaterialTheme.typography.labelLarge, color = MutedForeground)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = "" },
                    placeholder = { Text("••••••••", color = MutedForeground.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
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
                    shape = RoundedCornerShape(scale.dp(12f)),
                    leadingIcon = {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = MutedForeground, modifier = Modifier.size(scale.iconSizeMedium))
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = MutedForeground
                            )
                        }
                    }
                )
            }

            // === Error Message ===
            if (errorMessage.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(scale.dp(12f)),
                    color = Destructive.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(scale.spacingM),
                        horizontalArrangement = Arrangement.spacedBy(scale.spacingS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Error, contentDescription = null, tint = Destructive, modifier = Modifier.size(scale.iconSizeMedium))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Destructive,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // === Loading Indicator ===
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary, modifier = Modifier.size(scale.iconSizeMedium))
                }
            }

            // === Submit Button ===
            Button(
                onClick = {
                    if (!validateInputs()) return@Button
                    errorMessage = ""
                    if (isLogin) {
                        viewModel.login(email, password) { event ->
                            when (event) {
                                is SettingsEvent.LoggedIn -> onAuthSuccess()
                                is SettingsEvent.ShowSnackbar -> errorMessage = event.message
                                else -> {}
                            }
                        }
                    } else {
                        viewModel.register(email, password) { event ->
                            when (event) {
                                is SettingsEvent.LoggedIn -> onAuthSuccess()
                                is SettingsEvent.ShowSnackbar -> errorMessage = event.message
                                else -> {}
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = PrimaryForeground,
                    disabledContainerColor = Muted,
                    disabledContentColor = MutedForeground
                ),
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = scale.buttonHeight),
                shape = RoundedCornerShape(scale.dp(50f))
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = PrimaryForeground,
                        modifier = Modifier.size(scale.iconSizeSmall),
                        strokeWidth = scale.dp(2f)
                    )
                    Spacer(Modifier.width(scale.spacingS))
                }
                Text(
                    text = if (isLogin) "Log In" else "Create Account",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryForeground
                )
            }
        }
    }
}
