package com.example.streamguidemobile.ui.theme

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

private val StreamGuideColors = darkColorScheme(
    primary = Color(0xFF55E2C2),
    onPrimary = Color(0xFF06231D),
    primaryContainer = Color(0xFF123C35),
    onPrimaryContainer = Color(0xFFB8F5E8),
    secondary = Color(0xFFFFBD59),
    onSecondary = Color(0xFF2C1B00),
    secondaryContainer = Color(0xFF4A3512),
    onSecondaryContainer = Color(0xFFFFE2AC),
    tertiary = Color(0xFF8AB4FF),
    background = Color(0xFF090B0E),
    onBackground = Color(0xFFF1F3F5),
    surface = Color(0xFF111419),
    onSurface = Color(0xFFF1F3F5),
    surfaceVariant = Color(0xFF1C2128),
    onSurfaceVariant = Color(0xFFB8C0CA),
    outline = Color(0xFF444C57),
    error = Color(0xFFFF7D78),
    onError = Color(0xFF360300),
    errorContainer = Color(0xFF55201E),
    onErrorContainer = Color(0xFFFFDAD7)
)

private val StreamGuideTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    )
)

private val StreamGuideShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun StreamGuideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StreamGuideColors,
        typography = StreamGuideTypography,
        shapes = StreamGuideShapes,
        content = content
    )
}
