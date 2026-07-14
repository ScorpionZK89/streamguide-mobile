package com.example.streamguidemobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MaterialStreamGuideColors = darkColorScheme(
    primary = CinematicColors.Gold,
    onPrimary = CinematicColors.OnGold,
    primaryContainer = CinematicColors.GoldMuted,
    onPrimaryContainer = CinematicColors.GoldBright,
    secondary = CinematicColors.TextSecondary,
    onSecondary = CinematicColors.Canvas,
    secondaryContainer = CinematicColors.PanelRaised,
    onSecondaryContainer = CinematicColors.TextPrimary,
    tertiary = CinematicColors.GoldBright,
    background = CinematicColors.Canvas,
    onBackground = CinematicColors.TextPrimary,
    surface = CinematicColors.Panel,
    onSurface = CinematicColors.TextPrimary,
    surfaceVariant = CinematicColors.PanelRaised,
    onSurfaceVariant = CinematicColors.TextSecondary,
    outline = CinematicColors.BorderStrong,
    error = CinematicColors.Error,
    onError = CinematicColors.Canvas,
    errorContainer = CinematicColors.ErrorContainer,
    onErrorContainer = CinematicColors.TextPrimary
)

private val MaterialStreamGuideTypography = Typography(
    headlineSmall = CinematicTypography.HeroTitle,
    titleLarge = CinematicTypography.HeroTitle,
    titleMedium = CinematicTypography.SectionTitle,
    bodyLarge = CinematicTypography.Body,
    bodyMedium = CinematicTypography.Body,
    labelLarge = CinematicTypography.CardTitle
)

private val StreamGuideShapes = Shapes(
    extraSmall = RoundedCornerShape(StreamGuideRadii.Badge),
    small = RoundedCornerShape(StreamGuideRadii.Small),
    medium = RoundedCornerShape(StreamGuideRadii.Control),
    large = RoundedCornerShape(StreamGuideRadii.Card),
    extraLarge = RoundedCornerShape(StreamGuideRadii.Hero)
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
