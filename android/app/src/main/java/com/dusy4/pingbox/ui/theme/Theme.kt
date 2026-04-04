package com.dusy4.pingbox.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Extended semantic colors beyond M3 defaults
data class PingBoxExtendedColors(
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val statusGreen: Color,
    val statusGreenDim: Color,
    val cardBackground: Color,
    val muted: Color,
    val mutedForeground: Color,
    val border: Color,
    val destructiveDim: Color
)

val LocalPingBoxColors = staticCompositionLocalOf {
    PingBoxExtendedColors(
        warning = Color(0xFFFF9500),
        onWarning = Color(0xFF000000),
        warningContainer = Color(0x33FF9500),
        statusGreen = Color(0xFF34C759),
        statusGreenDim = Color(0x1A34C759),
        cardBackground = Color(0xFF141414),
        muted = Color(0xFF262626),
        mutedForeground = Color(0xFFA3A3A3),
        border = Color(0xFF262626),
        destructiveDim = Color(0xFF1A1111)
    )
}

// M3 dark color scheme mapped from PingBox design
private val PingBoxColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = PrimaryForeground,
    primaryContainer = Color(0xFF404040),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Secondary,
    onSecondary = SecondaryForeground,
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFA3A3A3),
    tertiary = StatusGreen,
    onTertiary = Color(0xFF000000),
    tertiaryContainer = StatusGreenDim,
    onTertiaryContainer = StatusGreen,
    background = Background,
    onBackground = Foreground,
    surface = CardBackground,
    onSurface = Foreground,
    surfaceVariant = Muted,
    onSurfaceVariant = MutedForeground,
    error = Destructive,
    onError = Color.White,
    errorContainer = DestructiveDim,
    onErrorContainer = Destructive,
    outline = Border,
    outlineVariant = Border,
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF000000),
    scrim = Color(0x80000000),
)

val PingBoxShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PingBoxTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    val extendedColors = PingBoxExtendedColors(
        warning = Warning,
        onWarning = Color(0xFF000000),
        warningContainer = Color(0x33FF9500),
        statusGreen = StatusGreen,
        statusGreenDim = StatusGreenDim,
        cardBackground = CardBackground,
        muted = Muted,
        mutedForeground = MutedForeground,
        border = Border,
        destructiveDim = DestructiveDim
    )

    CompositionLocalProvider(LocalPingBoxColors provides extendedColors) {
        MaterialTheme(
            colorScheme = PingBoxColorScheme,
            shapes = PingBoxShapes,
            content = content
        )
    }
}

object PingBoxColors {
    val warning: Color
        @Composable get() = LocalPingBoxColors.current.warning
    val onWarning: Color
        @Composable get() = LocalPingBoxColors.current.onWarning
    val warningContainer: Color
        @Composable get() = LocalPingBoxColors.current.warningContainer
    val statusGreen: Color
        @Composable get() = LocalPingBoxColors.current.statusGreen
    val statusGreenDim: Color
        @Composable get() = LocalPingBoxColors.current.statusGreenDim
    val cardBackground: Color
        @Composable get() = LocalPingBoxColors.current.cardBackground
    val muted: Color
        @Composable get() = LocalPingBoxColors.current.muted
    val mutedForeground: Color
        @Composable get() = LocalPingBoxColors.current.mutedForeground
    val border: Color
        @Composable get() = LocalPingBoxColors.current.border
    val destructiveDim: Color
        @Composable get() = LocalPingBoxColors.current.destructiveDim
}
