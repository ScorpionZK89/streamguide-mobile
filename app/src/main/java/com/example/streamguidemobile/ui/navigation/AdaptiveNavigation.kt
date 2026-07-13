package com.example.streamguidemobile.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.streamguidemobile.ui.theme.CinematicColors
import com.example.streamguidemobile.ui.theme.StreamGuideRadii
import com.example.streamguidemobile.ui.theme.StreamGuideSpacing
import com.example.streamguidemobile.ui.theme.StreamGuideMotion

enum class AppDestination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Live("Live TV", Icons.Default.LiveTv),
    Guide("Gids", Icons.Default.DateRange),
    Movies("Films", Icons.Default.Movie),
    Series("Series", Icons.Default.VideoLibrary)
}

@Composable
fun StreamGuideBottomNavigation(
    selected: AppDestination,
    onSelected: (AppDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CinematicColors.CanvasTop.copy(alpha = 0.96f))
            .navigationBarsPadding()
            .height(50.dp)
            .padding(horizontal = StreamGuideSpacing.Sm, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppDestination.entries.forEach { destination ->
            NavigationItem(
                destination = destination,
                selected = destination == selected,
                onClick = { onSelected(destination) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StreamGuideNavigationRail(
    selected: AppDestination,
    onSelected: (AppDestination) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(84.dp)
            .fillMaxHeight()
            .background(CinematicColors.Panel)
            .padding(horizontal = 7.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(CinematicColors.Gold.copy(alpha = 0.12f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CinematicColors.Gold, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.height(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            AppDestination.entries.forEach { destination ->
                NavigationItem(destination, destination == selected, { onSelected(destination) })
            }
        }
        Spacer(Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSettings)
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Instellingen", tint = CinematicColors.TextSecondary)
            Text("Instellingen", color = CinematicColors.TextSecondary, style = com.example.streamguidemobile.ui.theme.CinematicTypography.Badge)
        }
    }
}

@Composable
private fun NavigationItem(
    destination: AppDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val contentColor = if (selected) CinematicColors.GoldBright else CinematicColors.TextSecondary
    val containerColor by animateColorAsState(
        targetValue = when {
            focused -> CinematicColors.Gold.copy(alpha = 0.16f)
            selected -> CinematicColors.Gold.copy(alpha = 0.09f)
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        animationSpec = tween(StreamGuideMotion.Quick),
        label = "navigation-container"
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) CinematicColors.Gold.copy(alpha = 0.72f) else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(StreamGuideMotion.Quick),
        label = "navigation-border"
    )
    val shape = RoundedCornerShape(StreamGuideRadii.Control)
    Column(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .background(containerColor, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(destination.icon, contentDescription = destination.label, tint = contentColor, modifier = Modifier.size(17.dp))
        }
        Text(destination.label, color = contentColor, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, style = com.example.streamguidemobile.ui.theme.CinematicTypography.Badge)
    }
}
