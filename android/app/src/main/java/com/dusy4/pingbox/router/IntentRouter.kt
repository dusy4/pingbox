package com.dusy4.pingbox.router

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.dusy4.pingbox.RouterActivity

data class ResolvedTarget(
    val type: String,
    val value: String,
    val notificationId: String = ""
)

object IntentRouter {

    fun buildIntent(context: Context, target: ResolvedTarget): Intent {
        return when (target.type) {
            "url" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(target.value)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            "deeplink" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(target.value)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            "package" -> {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(target.value)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    launchIntent
                } else {
                    createFallbackIntent(context, "App not found: ${target.value}")
                }
            }

            "component" -> {
                try {
                    val parts = target.value.split("/")
                    if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                        return createFallbackIntent(context, "Invalid component format")
                    }
                    Intent().apply {
                        component = ComponentName(parts[0], parts[1])
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } catch (e: Exception) {
                    createFallbackIntent(context, "Cannot open component: ${target.value}")
                }
            }

            "intent_uri" -> {
                try {
                    Intent.parseUri(target.value, Intent.URI_INTENT_SCHEME).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } catch (e: Exception) {
                    createFallbackIntent(context, "Invalid intent URI")
                }
            }

            else -> {
                Intent(context, RouterActivity::class.java).apply {
                    putExtra("notification_id", target.notificationId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        }
    }

    fun buildPendingIntentTarget(context: Context, target: ResolvedTarget): Intent {
        val underlyingIntent = buildIntent(context, target)
        
        return Intent(context, RouterActivity::class.java).apply {
            putExtra("target_intent", underlyingIntent)
            putExtra("notification_id", target.notificationId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun createFallbackIntent(context: Context, errorMessage: String): Intent {
        return Intent(context, RouterActivity::class.java).apply {
            putExtra("error", errorMessage)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
