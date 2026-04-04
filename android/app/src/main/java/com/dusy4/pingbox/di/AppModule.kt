package com.dusy4.pingbox.di

import com.dusy4.pingbox.data.preferences.AppPreferences
import com.dusy4.pingbox.data.remote.TriggerApiService
import com.dusy4.pingbox.ui.viewmodel.HistoryViewModel
import com.dusy4.pingbox.ui.viewmodel.RulesViewModel
import com.dusy4.pingbox.ui.viewmodel.SettingsViewModel
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

val networkModule = module {
    single {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }
    }

    single {
        val json = get<Json>()
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://your-server.example.com/")
            .client(get<OkHttpClient>())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    single {
        get<Retrofit>().create(TriggerApiService::class.java)
    }
}

val preferencesModule = module {
    single {
        AppPreferences(androidContext())
    }
}

val viewModelModule = module {
    viewModelOf(::SettingsViewModel)
    viewModelOf(::RulesViewModel)
    viewModelOf(::HistoryViewModel)
}

val appModule = listOf(networkModule, preferencesModule, viewModelModule)
