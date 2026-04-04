package com.dusy4.pingbox.push

import android.Manifest
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.dusy4.pingbox.R
import com.dusy4.pingbox.TriggerApp
import com.dusy4.pingbox.data.local.NotificationEntity
import com.dusy4.pingbox.data.local.TriggerDatabase
import com.dusy4.pingbox.router.IntentRouter
import com.dusy4.pingbox.router.ResolvedTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

class TriggerMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: TriggerDatabase

    override fun onCreate() {
        super.onCreate()
        database = TriggerApp.getDatabase(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        Timber.tag("TriggerMessaging").d("Message received with ${data.size} data fields")
        
        val payload = NotificationPayload(
            id = data["ntf_id"] ?: "ntf_${UUID.randomUUID().toString().take(12)}",
            title = data["title"] ?: "PingBox",
            body = data["body"] ?: "",
            icon = data["icon"],
            tag = data["tag"] ?: "",
            targetType = data["target_type"] ?: "none",
            targetValue = data["target_value"] ?: ""
        )
        
        Timber.tag("TriggerMessaging").i("Processing notification: ${payload.id} - ${payload.title}")

        serviceScope.launch {
            try {
                val localRules = withContext(Dispatchers.IO) {
                    database.ruleDao().getEnabled()
                }
                Timber.tag("TriggerMessaging").d("Found ${localRules.size} local rules")
                
                val resolvedTarget = RuleEngine.resolve(payload, localRules.map { it.toLocalRule() })
                Timber.tag("TriggerMessaging").d("Resolved target: ${resolvedTarget.type} -> ${resolvedTarget.value}")

                val pendingIntent = PendingIntent.getActivity(
                    this@TriggerMessagingService,
                    payload.id.hashCode(),
                    IntentRouter.buildPendingIntentTarget(this@TriggerMessagingService, resolvedTarget),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = NotificationCompat.Builder(this@TriggerMessagingService, TriggerApp.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(payload.title)
                    .setContentText(payload.body)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(this@TriggerMessagingService, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }

                if (hasPermission) {
                    NotificationManagerCompat.from(this@TriggerMessagingService)
                        .notify(payload.id.hashCode(), notification)
                    Timber.tag("TriggerMessaging").i("Notification displayed: ${payload.id}")
                } else {
                    Timber.tag("TriggerMessaging").w("No POST_NOTIFICATIONS permission")
                }

                // Save to history after showing notification
                withContext(Dispatchers.IO) {
                    database.notificationDao().insert(
                        NotificationEntity(
                            id = payload.id,
                            title = payload.title,
                            body = payload.body,
                            icon = payload.icon,
                            tag = payload.tag,
                            targetType = payload.targetType,
                            targetValue = payload.targetValue,
                            receivedAt = System.currentTimeMillis()
                        )
                    )
                }
                Timber.tag("TriggerMessaging").d("Notification saved to history")
            } catch (e: Exception) {
                Timber.tag("TriggerMessaging").e(e, "Error processing notification")
            }
        }
    }

    override fun onNewToken(token: String) {
        Timber.tag("TriggerMessaging").i("FCM token refreshed")
        Timber.tag("TriggerMessaging").d("New token: ${token.take(20)}...")
        DeviceRegistrationWorker.enqueue(this, token)
    }
}

data class NotificationPayload(
    val id: String,
    val title: String,
    val body: String,
    val icon: String?,
    val tag: String,
    val targetType: String,
    val targetValue: String
)

data class LocalRule(
    val id: String,
    val name: String,
    val matchTag: String?,
    val targetType: String,
    val targetValue: String,
    val priority: Int,
    val enabled: Boolean
)

object RuleEngine {
    fun resolve(payload: NotificationPayload, rules: List<LocalRule>): ResolvedTarget {
        Timber.tag("RuleEngine").d("Resolving target for tag: ${payload.tag}")
        val enabledRules = rules.filter { it.enabled }.sortedBy { it.priority }
        Timber.tag("RuleEngine").d("Checking ${enabledRules.size} enabled rules")
        
        for (rule in enabledRules) {
            if (matchesTag(payload.tag, rule.matchTag)) {
                Timber.tag("RuleEngine").i("Rule matched: ${rule.name} (${rule.matchTag})")
                return ResolvedTarget(
                    type = rule.targetType,
                    value = rule.targetValue,
                    notificationId = payload.id
                )
            }
        }
        
        Timber.tag("RuleEngine").d("No rule matched, using default target: ${payload.targetType}")
        return ResolvedTarget(
            type = payload.targetType,
            value = payload.targetValue,
            notificationId = payload.id
        )
    }

    private fun matchesTag(tag: String, pattern: String?): Boolean {
        if (pattern.isNullOrEmpty()) return false
        return when {
            pattern.contains("*") -> {
                val regex = pattern.replace("*", ".*").toRegex()
                tag.matches(regex)
            }
            else -> tag == pattern
        }
    }
}

private fun com.dusy4.pingbox.data.local.RuleEntity.toLocalRule(): LocalRule {
    return LocalRule(
        id = id,
        name = name,
        matchTag = matchTag,
        targetType = targetType,
        targetValue = targetValue,
        priority = priority,
        enabled = enabled
    )
}
