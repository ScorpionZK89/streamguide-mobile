package com.example.streamguidemobile

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.streamguidemobile.data.ChannelEntity
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<StreamGuideViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
                StreamGuideApp(viewModel)
            }
        }
    }
}

@Composable
private fun StreamGuideApp(viewModel: StreamGuideViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedChannel by remember { mutableStateOf<ChannelEntity?>(null) }

    selectedChannel?.let { channel ->
        PlayerScreen(
            channel = channel,
            viewModel = viewModel,
            onBack = { selectedChannel = null },
            onOpenChannel = { selectedChannel = it }
        )
        return
    }

    if (state.playlists.isEmpty()) {
        AddPlaylistScreen(state, viewModel)
    } else {
        HomeScreen(
            state = state,
            viewModel = viewModel,
            onOpen = { channel ->
                viewModel.markWatched(channel)
                selectedChannel = channel
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlaylistScreen(state: StreamGuideState, viewModel: StreamGuideViewModel) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("StreamGuide Mobile") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Voeg je eigen legale M3U-bron toe. StreamGuide levert zelf geen content.")
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Playlist naam") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("M3U URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = { viewModel.importPlaylist(name, url) }, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                Text("Opslaan en laden")
            }
            StatusBlock(state)
            Text("Gebruik uitsluitend bronnen waarvoor je toestemming hebt.", color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(state: StreamGuideState, viewModel: StreamGuideViewModel, onOpen: (ChannelEntity) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StreamGuide Mobile") },
                actions = {
                    IconButton(onClick = viewModel::syncFirstPlaylist) { Icon(Icons.Default.Refresh, contentDescription = "Sync") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !state.showFavorites, onClick = { viewModel.setFavoritesOnly(false) }, label = { Text("Alle") })
                FilterChip(selected = state.showFavorites, onClick = { viewModel.setFavoritesOnly(true) }, label = { Text("Favorieten") })
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Zoeken") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true
            )
            StatusBlock(state)
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(state.channels, key = { it.id }) { channel ->
                    ChannelRow(channel, onOpen = { onOpen(channel) }, onFavorite = { viewModel.toggleFavorite(channel) })
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(channel: ChannelEntity, onOpen: () -> Unit, onFavorite: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(channel.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(channel.groupTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        IconButton(onClick = onFavorite) {
            Icon(if (channel.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Favoriet")
        }
    }
}

@Composable
private fun PlayerScreen(channel: ChannelEntity, viewModel: StreamGuideViewModel, onBack: () -> Unit, onOpenChannel: (ChannelEntity) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player = remember(channel.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.streamUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    PlayerView(it).apply {
                        useController = true
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        this.player = player
                    }
                },
                update = { it.player = player }
            )
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Terug", tint = Color.White) }
                Text(channel.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = { viewModel.toggleFavorite(channel) }) { Text("Favoriet") }
            }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.55f)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = {
                    scope.launch {
                        val id = viewModel.nextChannelId(channel.id, -1)
                        val next = state.channels.firstOrNull { it.id == id }
                        if (next != null) onOpenChannel(next)
                    }
                }) { Text("Vorige") }
                TextButton(onClick = {
                    scope.launch {
                        val id = viewModel.nextChannelId(channel.id, 1)
                        val next = state.channels.firstOrNull { it.id == id }
                        if (next != null) onOpenChannel(next)
                    }
                }) { Text("Volgende") }
            }
        }
    }
}

@Composable
private fun StatusBlock(state: StreamGuideState) {
    if (state.isLoading || state.message != null || state.error != null) {
        Card(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isLoading) CircularProgressIndicator()
                state.message?.let { Text(it) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
