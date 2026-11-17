package com.muoffline.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val MuObsidian = Color(0xFF0B0D13)
val MuAbyssSlate = Color(0xFF121622)
val MuStoneCard = Color(0xFF181D2B)
val MuBorderSlate = Color(0xFF2B3246)
val MuGold = Color(0xFFF59E0B)
val MuAmberBright = Color(0xFFFBBF24)
val MuGoldMuted = Color(0xFFB45309)
val MuSky = Color(0xFF38BDF8)
val MuEmerald = Color(0xFF10B981)
val MuCrimson = Color(0xFFEF4444)
val MuPurple = Color(0xFFA855F7)
val MuTextPrimary = Color(0xFFF1F5F9)
val MuTextSecondary = Color(0xFF94A3B8)

private val MuDarkColorScheme = darkColorScheme(
    primary = MuGold,
    onPrimary = Color(0xFF1B1202),
    primaryContainer = Color(0xFF3B2809),
    onPrimaryContainer = Color(0xFFFDE68A),
    secondary = MuSky,
    onSecondary = Color(0xFF041C2C),
    secondaryContainer = Color(0xFF132D46),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = MuEmerald,
    onTertiary = Color(0xFF022016),
    tertiaryContainer = Color(0xFF0D382B),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = MuObsidian,
    onBackground = MuTextPrimary,
    surface = MuAbyssSlate,
    onSurface = MuTextPrimary,
    surfaceVariant = MuStoneCard,
    onSurfaceVariant = MuTextSecondary,
    outline = MuBorderSlate,
    outlineVariant = Color(0xFF1F2536),
    error = MuCrimson,
    onError = Color.White,
    errorContainer = Color(0xFF451218),
    onErrorContainer = Color(0xFFFECACA)
)

val MuTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.5.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.4.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

val MuShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun MuOfflineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MuDarkColorScheme,
        typography = MuTypography,
        shapes = MuShapes,
        content = content
    )
}
