package com.dusy4.pingbox

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dusy4.pingbox.push.DeviceRegistrationWorker
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

private val Context.bootDataStore by preferencesDataStore(name = "app_settings")

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val apiKey = runBlocking {
            context.bootDataStore.data.first()[stringPreferencesKey("api_key")] ?: ""
        }

        if (apiKey.isEmpty()) {
            Timber.d("BootReceiver: no API key, skipping registration")
            return
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            DeviceRegistrationWorker.enqueue(context, token)
            Timber.d("BootReceiver: device registration enqueued")
        }.addOnFailureListener { e ->
            Timber.e(e, "BootReceiver: failed to get FCM token")
        }
    }
}
