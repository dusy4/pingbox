package com.dusy4.pingbox.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * PingBox typography — matches the React design (Inter / system sans-serif)
 * Uses system default sans-serif since we don't bundle a custom font.
 * Heading styles use bold weights with tight tracking like the design.
 */
val PingBoxTypography = Typography(
    // Used for screen titles (e.g. "Settings", "Rules") — text-3xl font-bold
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp  // tracking-tight
    ),
    // Used for sub-screen titles (e.g. "Add New Rule") — text-2xl font-bold
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.25).sp
    ),
    // Card item titles (e.g. "GitHub Actions") — font-bold text-base
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // Input values — font-medium text-base
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // Secondary text (descriptions, tag values) — text-xs font-medium
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // Field labels — text-[13px] font-semibold
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    // Uppercase labels (e.g. "RULE NAME") — text-[11px] font-bold uppercase
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.sp  // tracking-wider
    ),
    // Bottom nav labels, action labels — text-[10px] font-bold
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
)
