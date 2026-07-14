package com.example.streamguidemobile.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CinematicColors {
    val Canvas = Color(0xFF04070B)
    val CanvasDeep = Color(0xFF020407)
    val CanvasTop = Color(0xFF08111A)
    val Panel = Color(0xFF0A1017)
    val PanelRaised = Color(0xFF111922)
    val PanelPressed = Color(0xFF19232E)
    val Gold = Color(0xFFFFB116)
    val GoldBright = Color(0xFFFFC34B)
    val GoldMuted = Color(0xFF332407)
    val OnGold = Color(0xFF241500)
    val TextPrimary = Color(0xFFF5F7FA)
    val TextSecondary = Color(0xFFAAB4C0)
    val TextMuted = Color(0xFF778390)
    val Border = Color(0x12FFFFFF)
    val BorderStrong = Color(0x28FFFFFF)
    val Live = Color(0xFFE5484D)
    val Error = Color(0xFFFF7D78)
    val ErrorContainer = Color(0xFF55201E)
    val Scrim = Color(0xE606090D)
}

object StreamGuideSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 24.dp
    val Xxl = 32.dp
}

object StreamGuideRadii {
    val Badge = 5.dp
    val Small = 6.dp
    val Control = 9.dp
    val Card = 10.dp
    val Hero = 14.dp
}

object StreamGuideMotion {
    const val Quick = 180
    const val Standard = 210
    const val Emphasis = 250
}

object CinematicTypography {
    val HeroTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    )
    val SectionTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    )
    val CardTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.sp
    )
    val Metadata = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.sp
    )
    val Badge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        letterSpacing = 0.sp
    )
    val Body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
    val TimelineTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        letterSpacing = 0.sp
    )
    val TimelineMeta = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 8.sp,
        lineHeight = 10.sp,
        letterSpacing = 0.sp
    )
}

val StreamGuideColors = CinematicColors
val StreamGuideTypography = CinematicTypography
