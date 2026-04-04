package com.dusy4.pingbox.util

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

/**
 * User-friendly error messages for network and API errors.
 * Classifies exceptions and HTTP errors into clear, actionable messages.
 */
object UserFriendlyError {

    /**
     * Convert any exception to a user-friendly message.
     */
    fun fromException(e: Throwable): String {
        return when (e) {
            is UnknownHostException -> "Cannot reach server. Check your internet connection."
            is SocketTimeoutException -> "Connection timed out. Check your network and try again."
            is SSLHandshakeException -> "Secure connection failed. Check your date/time settings."
            is java.net.ConnectException -> "Cannot connect to server. Check your internet connection."
            is java.io.IOException -> {
                val msg = e.message ?: ""
                when {
                    msg.contains("timeout", ignoreCase = true) -> "Connection timed out. Try again."
                    msg.contains("network", ignoreCase = true) -> "Network error. Check your connection."
                    else -> "Network error. Please try again."
                }
            }
            else -> "Unexpected error: ${e.message ?: "Unknown"}"
        }
    }

    /**
     * Format HTTP error response into user-friendly message.
     */
    fun fromHttpError(code: Int, errorBody: String?): String {
        val body = errorBody?.take(200) ?: ""
        return when (code) {
            400 -> {
                // Try to extract validation message from JSON
                val msg = extractErrorMessage(body)
                if (msg.isNotEmpty()) msg else "Invalid input. Please check your entries."
            }
            401 -> "Wrong email or password."
            403 -> "Access denied. Your account may be restricted."
            404 -> "Not found. The requested resource doesn't exist."
            409 -> {
                val msg = extractErrorMessage(body)
                if (msg.contains("email", ignoreCase = true)) "This email is already registered."
                else if (msg.isNotEmpty()) msg
                else "This already exists."
            }
            422 -> {
                val msg = extractErrorMessage(body)
                if (msg.isNotEmpty()) msg else "Invalid input. Please check your entries."
            }
            429 -> "Too many attempts. Wait a moment and try again."
            in 500..599 -> "Server error (${code}). Please try again later."
            else -> "Error ${code}: ${body.ifEmpty { "Unknown error" }}"
        }
    }

    private fun extractErrorMessage(body: String): String {
        return try {
            // Try to parse JSON error response
            val regex = """"(?:detail|message|error)"\s*:\s*"([^"]*)"""".toRegex()
            regex.find(body)?.groupValues?.get(1) ?: body
        } catch (e: Exception) {
            body
        }
    }
}

/**
 * Screen size categories for adaptive UI.
 */
enum class ScreenSize {
    COMPACT,    // < 360dp - small phones
    NORMAL,     // 360-414dp - most phones
    LARGE       // > 414dp - large phones, foldables, tablets
}

/**
 * Adaptive spacing and sizing based on screen width.
 * Use this instead of hardcoded dp values for better scaling.
 */
@Composable
fun rememberScreenScale(): ScreenScale {
    val config = LocalConfiguration.current
    val density = LocalDensity.current.density
    return remember(config.screenWidthDp, density) {
        ScreenScale(config.screenWidthDp, density)
    }
}

/**
 * Holds scaled dimensions for adaptive UI.
 */
data class ScreenScale(
    val screenWidthDp: Int,
    val density: Float
) {
    val screenSize: ScreenSize = when {
        screenWidthDp < 360 -> ScreenSize.COMPACT
        screenWidthDp <= 414 -> ScreenSize.NORMAL
        else -> ScreenSize.LARGE
    }

    private val factor: Float = when (screenSize) {
        ScreenSize.COMPACT -> 0.85f
        ScreenSize.NORMAL -> 1.0f
        ScreenSize.LARGE -> 1.15f
    }

    fun dp(base: Float): Dp = (base * factor).dp
    fun dp(base: Int): Dp = (base * factor).dp
    fun sp(base: Float): TextUnit = (base * factor).sp
    fun sp(base: Int): TextUnit = (base * factor).sp

    // Pre-computed common values
    val headerTopPadding = dp(40f)
    val headerBottomPadding = dp(24f)
    val sectionPadding = dp(24f)
    val cardPadding = dp(16f)
    val quickActionButtonSize = dp(48f)
    val quickActionButtonIconSize = dp(20f)
    val quickActionSpacing = dp(20f)
    val cardCornerRadius = dp(24f)
    val inputFieldHeight = dp(48f)
    val buttonHeight = dp(48f)
    val iconSizeSmall = dp(16f)
    val iconSizeMedium = dp(20f)
    val iconSizeLarge = dp(24f)
    val spacingXS = dp(4f)
    val spacingS = dp(8f)
    val spacingM = dp(12f)
    val spacingL = dp(16f)
    val spacingXL = dp(24f)
    val spacingXXL = dp(32f)
}

/**
 * Non-Compose version for ViewModels and workers.
 */
object ScreenScaleStatic {
    // Default to NORMAL - actual values computed in Compose
    const val DEFAULT_FACTOR = 1.0f
}
