package com.example.kivo.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.kivo.data.remote.YouTubeResult
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.MusicViewModel

@Composable
fun YouTubeSearchScreen(
    viewModel: MusicViewModel,
    onPlayVideo: (YouTubeResult) -> Unit
) {
    val youtubeResults by viewModel.youtubeSearchResults.collectAsState()
    val isSearchingYouTube by viewModel.isSearchingYouTube.collectAsState()
    val youtubeError by viewModel.youtubeError.collectAsState()
    var searchText by remember { mutableStateOf("") }

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
                text = "KIVO",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp),
                letterSpacing = 1.sp
            )

            TextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.searchYouTube(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                placeholder = {
                    Text("Buscar en KIVO...", color = KivoTextDisabled, fontSize = 14.sp)
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

            if (isSearchingYouTube) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = KivoPurpleElectric)
                }
            } else if (youtubeResults.isEmpty() && searchText.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron resultados", color = KivoTextDisabled)
                }
            } else if (youtubeResults.isEmpty()) {
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
                            "Busca cualquier canción en KIVO",
                            color = KivoTextDisabled,
                            fontSize = 14.sp
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
                        items = youtubeResults,
                        key = { it.videoId }
                    ) { video ->
                        YouTubeResultItem(
                            video = video,
                            onClick = { onPlayVideo(video) }
                        )
                    }
                }
            }

            if (youtubeError != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = KivoSurface2,
                    contentColor = KivoPink
                ) {
                    Text(youtubeError!!)
                }
            }
        }
    }
}

@Composable
fun YouTubeResultItem(
    video: YouTubeResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KivoSurface3),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = video.channelName,
                style = MaterialTheme.typography.labelMedium,
                color = KivoTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (video.lengthSeconds > 0) {
            Text(
                text = formatDuration(video.lengthSeconds),
                style = MaterialTheme.typography.labelSmall,
                color = KivoTextDisabled,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}
