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

private val MaterialStreamGuideColors = darkColorScheme(
    primary = Color(0xFFFFB52E),
    onPrimary = Color(0xFF2B1700),
    primaryContainer = Color(0xFF4D3100),
    onPrimaryContainer = Color(0xFFFFE0AA),
    secondary = Color(0xFF8FC7FF),
    onSecondary = Color(0xFF002746),
    secondaryContainer = Color(0xFF173A5D),
    onSecondaryContainer = Color(0xFFD4E9FF),
    tertiary = Color(0xFF70D6C6),
    background = Color(0xFF07090D),
    onBackground = Color(0xFFF1F3F5),
    surface = Color(0xFF10151C),
    onSurface = Color(0xFFF1F3F5),
    surfaceVariant = Color(0xFF18212B),
    onSurfaceVariant = Color(0xFFB5C0CD),
    outline = Color(0xFF344554),
    error = Color(0xFFFF7D78),
    onError = Color(0xFF360300),
    errorContainer = Color(0xFF55201E),
    onErrorContainer = Color(0xFFFFDAD7)
)

private val MaterialStreamGuideTypography = Typography(
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
        colorScheme = MaterialStreamGuideColors,
        typography = MaterialStreamGuideTypography,
        shapes = StreamGuideShapes,
        content = content
    )
}
