package com.dusy4.pingbox

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import androidx.work.Configuration
import com.dusy4.pingbox.data.local.TriggerDatabase
import com.dusy4.pingbox.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class TriggerApp : Application(), Configuration.Provider {

    companion object {
        const val CHANNEL_ID = "triggerapp_notifications"
        const val CHANNEL_NAME = "PingBox Notifications"

        @Volatile
        private var instance: TriggerDatabase? = null

        fun getDatabase(context: android.content.Context): TriggerDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TriggerDatabase::class.java,
                    "trigger_database"
                )
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidLogger()
            androidContext(this@TriggerApp)
            modules(appModule + listOf(
                org.koin.dsl.module {
                    single { getDatabase(this@TriggerApp) }
                }
            ))
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "PingBox notification channel"
                enableVibration(true)
                setShowBadge(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
