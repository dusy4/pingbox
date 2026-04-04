package com.dusy4.pingbox.push

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dusy4.pingbox.TriggerApp
import com.dusy4.pingbox.R
import com.dusy4.pingbox.data.local.NotificationEntity
import com.dusy4.pingbox.data.local.TriggerDatabase
import com.dusy4.pingbox.router.IntentRouter
import com.dusy4.pingbox.router.ResolvedTarget
import kotlinx.coroutines.*

class UnifiedPushReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        val payload = NotificationPayload(
            id = bundle.getString("ntf_id") ?: "ntf_${System.currentTimeMillis()}",
            title = bundle.getString("title") ?: "PingBox",
            body = bundle.getString("body") ?: "",
            icon = bundle.getString("icon"),
            tag = bundle.getString("tag") ?: "",
            targetType = bundle.getString("target_type") ?: "none",
            targetValue = bundle.getString("target_value") ?: ""
        )

        receiverScope.launch {
            try {
                showNotification(context, payload)
                saveToHistory(context, payload)
            } catch (e: Exception) {
                android.util.Log.e("UnifiedPushReceiver", "Error: ${e.message}")
            }
        }
    }

    private fun showNotification(context: Context, payload: NotificationPayload) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val resolvedTarget = ResolvedTarget(
            type = payload.targetType,
            value = payload.targetValue,
            notificationId = payload.id
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            payload.id.hashCode(),
            IntentRouter.buildPendingIntentTarget(context, resolvedTarget),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, TriggerApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(payload.id.hashCode(), notification)
    }

    private suspend fun saveToHistory(context: Context, payload: NotificationPayload) {
        val database = TriggerApp.getDatabase(context)
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
}
