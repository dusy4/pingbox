package com.dusy4.pingbox.push

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dusy4.pingbox.data.remote.DeviceRegisterRequest
import com.dusy4.pingbox.data.remote.TriggerApiService
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber

private val Context.workerDataStore by preferencesDataStore(name = "app_settings")

class DeviceRegistrationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        Timber.d("Starting device registration work")
        return try {
            val fcmToken = inputData.getString(KEY_FCM_TOKEN)
                ?: FirebaseMessaging.getInstance().token.await()
            val deviceName = inputData.getString(KEY_DEVICE_NAME) ?: android.os.Build.MODEL
            
            val prefs = applicationContext.workerDataStore.data.first()
            val apiKey = prefs[API_KEY_KEY] ?: ""
            
            Timber.d("Device: $deviceName")
            
            if (apiKey.isEmpty()) {
                Timber.e("API key is empty, cannot register device")
                return Result.failure()
            }

            val apiService: TriggerApiService = org.koin.core.context.GlobalContext.get().get()
            
            Timber.i("Registering device with FCM token")
            val response = apiService.registerDevice(
                "Bearer $apiKey",
                DeviceRegisterRequest(
                    fcmToken = fcmToken,
                    deviceName = deviceName,
                    platform = "android"
                )
            )
            
            if (response.isSuccessful) {
                val deviceId = response.body()?.id
                Timber.i("Device registered successfully: $deviceId")
                Result.success()
            } else {
                Timber.e("Device registration failed: ${response.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Timber.e(e, "Exception during device registration")
            Result.retry()
        }
    }
    
    companion object {
        private val API_KEY_KEY = stringPreferencesKey("api_key")
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_DEVICE_NAME = "device_name"
        
        fun enqueue(context: Context, token: String, deviceName: String? = null) {
            val builder = androidx.work.OneTimeWorkRequestBuilder<DeviceRegistrationWorker>()
                .setInputData(
                    androidx.work.workDataOf(
                        KEY_FCM_TOKEN to token,
                        KEY_DEVICE_NAME to (deviceName ?: "")
                    )
                )
            
            androidx.work.WorkManager.getInstance(context).enqueue(builder.build())
        }
    }
}
