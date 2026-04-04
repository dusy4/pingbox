package com.dusy4.pingbox.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dusy4.pingbox.data.preferences.AppPreferences
import com.dusy4.pingbox.data.remote.*
import com.dusy4.pingbox.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import com.dusy4.pingbox.util.rememberScreenScale
import timber.log.Timber

data class DebugTestResult(
    val name: String,
    val status: TestStatus,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TestStatus {
    SUCCESS, FAILED, RUNNING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scale = rememberScreenScale()

    val preferences: AppPreferences = KoinJavaComponent.get(AppPreferences::class.java)
    val apiService: TriggerApiService = KoinJavaComponent.get(TriggerApiService::class.java)

    var testResults by remember { mutableStateOf<List<DebugTestResult>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
    }

    suspend fun runTest(name: String, block: suspend () -> Pair<Boolean, String>): DebugTestResult {
        return try {
            val (success, detail) = block()
            DebugTestResult(name, if (success) TestStatus.SUCCESS else TestStatus.FAILED, detail)
        } catch (e: Exception) {
            DebugTestResult(name, TestStatus.FAILED, "${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun runAllTests() {
        scope.launch {
            isRunning = true
            val results = mutableListOf<DebugTestResult>()
            val token = preferences.accessToken.first()
            val apiKey = preferences.apiKey.first()

            // 1. Health check
            results.add(runTest("Health Check") {
                val resp = apiService.healthCheck()
                if (resp.isSuccessful) Pair(true, "Status: ${resp.body()?.status}")
                else Pair(false, "HTTP ${resp.code()}")
            })

            // 2. Push - URL target
            results.add(runTest("Push: URL target") {
                token?.let {
                    val resp = apiService.sendPush("Bearer $it", PushRequest(
                        title = "Debug: URL Test",
                        body = "This is a test notification with URL target",
                        tag = "debug_url",
                        target = TargetRequest("url", "https://example.com"),
                        devices = listOf("all")
                    ))
                    if (resp.isSuccessful) Pair(true, "ID: ${resp.body()?.id}")
                    else Pair(false, "HTTP ${resp.code()}")
                } ?: Pair(false, "Not logged in")
            })

            // 3. Push - Package target (YouTube)
            results.add(runTest("Push: Package (YouTube)") {
                token?.let {
                    val resp = apiService.sendPush("Bearer $it", PushRequest(
                        title = "Debug: YouTube",
                        body = "Open YouTube app",
                        tag = "debug_youtube",
                        target = TargetRequest("package", "com.google.android.youtube"),
                        devices = listOf("all")
                    ))
                    if (resp.isSuccessful) Pair(true, "ID: ${resp.body()?.id}")
                    else Pair(false, "HTTP ${resp.code()}")
                } ?: Pair(false, "Not logged in")
            })

            // 4. Push - Deeplink target
            results.add(runTest("Push: Deeplink (Settings)") {
                token?.let {
                    val resp = apiService.sendPush("Bearer $it", PushRequest(
                        title = "Debug: Deeplink",
                        body = "Open Settings",
                        tag = "debug_deeplink",
                        target = TargetRequest("deeplink", "android.settings.SETTINGS"),
                        devices = listOf("all")
                    ))
                    if (resp.isSuccessful) Pair(true, "ID: ${resp.body()?.id}")
                    else Pair(false, "HTTP ${resp.code()}")
                } ?: Pair(false, "Not logged in")
            })

            // 5. Push - Component target (YouTube Search)
            results.add(runTest("Push: Component (YouTube Search)") {
                token?.let {
                    val resp = apiService.sendPush("Bearer $it", PushRequest(
                        title = "Debug: YouTube Search",
                        body = "Open YouTube search",
                        tag = "debug_component",
                        target = TargetRequest("component", "com.google.android.youtube/com.google.android.apps.youtube.app.SearchActivity"),
                        devices = listOf("all")
                    ))
                    if (resp.isSuccessful) Pair(true, "ID: ${resp.body()?.id}")
                    else Pair(false, "HTTP ${resp.code()}")
                } ?: Pair(false, "Not logged in")
            })

            // 6. Push - None target
            results.add(runTest("Push: None target") {
                token?.let {
                    val resp = apiService.sendPush("Bearer $it", PushRequest(
                        title = "Debug: No Action",
                        body = "Notification with no tap action",
                        tag = "debug_none",
                        target = TargetRequest("none", ""),
                        devices = listOf("all")
                    ))
                    if (resp.isSuccessful) Pair(true, "ID: ${resp.body()?.id}")
                    else Pair(false, "HTTP ${resp.code()}")
                } ?: Pair(false, "Not logged in")
            })

            // 7. Push - Battery settings component
            results.add(runTest("Push: Battery Settings") {
                token?.let {
                    val resp = apiService.sendPush("Bearer $it", PushRequest(
                        title = "Debug: Battery",
                        body = "Open Battery Settings",
                        tag = "debug_battery",
                        target = TargetRequest("component", "com.android.settings/.Settings\$PowerUsageSummaryActivity"),
                        devices = listOf("all")
                    ))
                    if (resp.isSuccessful) Pair(true, "ID: ${resp.body()?.id}")
                    else Pair(false, "HTTP ${resp.code()}")
                } ?: Pair(false, "Not logged in")
            })

            // 8. List Devices
            results.add(runTest("List Devices") {
                apiKey?.let {
                    val resp = apiService.listDevices("Bearer $it")
                    if (resp.isSuccessful) {
                        val devices = resp.body()?.devices ?: emptyList()
                        Pair(true, "${devices.size} device(s)")
                    } else Pair(false, "HTTP ${resp.code()}")
                } ?: Pair(false, "No API key")
            })

            // 9. Get Rules
            results.add(runTest("Get Rules") {
                token?.let {
                    val resp = apiService.getRules("Bearer $it")
                    if (resp.isSuccessful) {
                        val rules = resp.body()?.rules ?: emptyList()
                        Pair(true, "${rules.size} rule(s)")
                    } else Pair(false, "HTTP ${resp.code()}")
                } ?: Pair(false, "Not logged in")
            })

            // 10. Create Rule
            results.add(runTest("Create Rule") {
                token?.let {
                    val resp = apiService.createRule("Bearer $it", RuleCreateRequest(
                        name = "Debug Test Rule",
                        matchTag = "debug_*",
                        targetType = "none",
                        targetValue = "",
                        priority = 0,
                        enabled = true
                    ))
                    if (resp.isSuccessful) Pair(true, "ID: ${resp.body()?.id}")
                    else Pair(false, "HTTP ${resp.code()}")
                } ?: Pair(false, "Not logged in")
            })

            // 11. Get History
            results.add(runTest("Get History") {
                token?.let {
                    val resp = apiService.getHistory("Bearer $it")
                    if (resp.isSuccessful) {
                        val history = resp.body()
                        Pair(true, "${history?.total ?: 0} total, page ${history?.page ?: 0}")
                    } else Pair(false, "HTTP ${resp.code()}")
                } ?: Pair(false, "Not logged in")
            })

            // 12. List API Keys
            results.add(runTest("List API Keys") {
                token?.let {
                    val resp = apiService.listApiKeys("Bearer $it")
                    if (resp.isSuccessful) {
                        val keys = resp.body()?.keys ?: emptyList()
                        Pair(true, "${keys.size} key(s)")
                    } else Pair(false, "HTTP ${resp.code()}")
                } ?: Pair(false, "Not logged in")
            })

            testResults = results
            isRunning = false
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
        topBar = {
            TopAppBar(
                title = { Text("Debug", color = Foreground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Foreground)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val logText = buildString {
                            appendLine("=== PingBox Debug Results ===")
                            testResults.forEach { r ->
                                appendLine("[${r.status.name}] ${r.name}: ${r.detail}")
                            }
                        }
                        copyToClipboard("Debug Results", logText)
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy Results", tint = Foreground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Run all tests button
            Button(
                onClick = { runAllTests() },
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(color = PrimaryForeground, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Running tests...")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Run All Endpoint Tests")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Test results
            if (testResults.isNotEmpty()) {
                Text(
                    text = "Results (${testResults.count { it.status == TestStatus.SUCCESS }}/${testResults.size} passed)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Foreground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(testResults) { result ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = CardBackground,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = when (result.status) {
                                        TestStatus.SUCCESS -> Icons.Default.CheckCircle
                                        TestStatus.FAILED -> Icons.Default.Error
                                        TestStatus.RUNNING -> Icons.Default.Refresh
                                    },
                                    contentDescription = null,
                                    tint = when (result.status) {
                                        TestStatus.SUCCESS -> StatusGreen
                                        TestStatus.FAILED -> Destructive
                                        TestStatus.RUNNING -> MutedForeground
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Foreground
                                    )
                                    Text(
                                        text = result.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedForeground,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.BugReport,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MutedForeground.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tap the button above to test all endpoints",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedForeground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
