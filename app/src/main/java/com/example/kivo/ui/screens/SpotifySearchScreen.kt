package com.example.kivo.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.kivo.data.models.SpotifyTrack
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.MusicViewModel

@Composable
fun SpotifySearchScreen(viewModel: MusicViewModel) {
    val searchResults by viewModel.spotifySearchResults.collectAsState()
    val isSearching by viewModel.isSearchingSpotify.collectAsState()
    val isConnected by viewModel.spotifyConnected.collectAsState()
    val isPlayingFullSong by viewModel.isPlayingFullSong.collectAsState()
    val youtubeError by viewModel.youtubeError.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val activity = LocalContext.current as? Activity

    val spotifyLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val pendingToken = com.example.kivo.media.SpotifyPendingAuth.consume()
        if (pendingToken != null) {
            viewModel.handleSpotifyToken(pendingToken)
        } else {
            viewModel.handleSpotifyActivityResult(0, result.resultCode, result.data)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KivoBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "SPOTIFY",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp),
                letterSpacing = 1.sp
            )

            if (!isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (activity != null) {
                                val intent = viewModel.getSpotifyLoginIntent(activity)
                                spotifyLoginLauncher.launch(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1DB954)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Conectar Spotify para reproducir", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Conectado a Spotify", color = Color(0xFF1DB954), fontSize = 12.sp)
                    }
                    TextButton(onClick = { viewModel.logoutSpotify() }) {
                        Text("Cerrar sesión", color = KivoTextDisabled, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            TextField(
                value = searchText,
                onValueChange = { 
                    searchText = it
                    viewModel.searchSpotify(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                placeholder = { 
                    Text("Buscar en Spotify...", color = KivoTextDisabled, fontSize = 14.sp) 
                },
                leadingIcon = { 
                    Icon(Icons.Default.Search, contentDescription = null, tint = KivoTextDisabled) 
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = KivoTextDisabled)
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = KivoSurface2,
                    unfocusedContainerColor = KivoSurface2,
                    cursorColor = KivoPurpleMain,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = KivoPurpleElectric)
                }
            } else if (searchResults.isEmpty() && searchText.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron resultados", color = KivoTextDisabled)
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = KivoTextDisabled
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Busca cualquier canción en Spotify",
                            color = KivoTextDisabled,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Asegúrate de tener Spotify abierto en algún dispositivo",
                            color = KivoTextDisabled,
                            fontSize = 12.sp
                        )
                    }
                }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = searchResults,
                            key = { it.id }
                        ) { track ->
                            SpotifyTrackItem(
                                track = track,
                                isLoading = isPlayingFullSong,
                                onClick = { viewModel.playSpotifyTrack(track) }
                            )
                        }
                    }
                }
        }
    }
}

@Composable
fun SpotifyTrackItem(
    track: SpotifyTrack,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { 
                clip = true
                shape = RoundedCornerShape(12.dp)
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KivoSurface3),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF1DB954),
                    strokeWidth = 2.dp
                )
            } else {
                AsyncImage(
                    model = track.album.images.firstOrNull()?.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.labelMedium,
                color = KivoTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = KivoTextDisabled,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun formatDuration(ms: Int): String {
    val minutes = ms / 60000
    val seconds = (ms % 60000) / 1000
    return String.format("%d:%02d", minutes, seconds)
}
