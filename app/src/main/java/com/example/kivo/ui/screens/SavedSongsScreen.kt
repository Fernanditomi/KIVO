package com.example.kivo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kivo.ui.theme.KivoBlack
import com.example.kivo.ui.theme.KivoTextDisabled
import com.example.kivo.ui.viewmodels.MusicViewModel

@Composable
fun SavedSongsScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val favoriteIds by viewModel.favoriteSongIds.collectAsState()
    val songs by viewModel.songs.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KivoBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Canciones guardadas",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }

            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron canciones guardadas", color = KivoTextDisabled)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = songs,
                        key = { it.id }
                    ) { song ->
                        val isSelected = remember(currentSong?.id) { currentSong?.id == song.id }
                        val isFavorite = remember(favoriteIds) { favoriteIds.contains(song.id) }

                        KivoMusicItemPro(
                            song = song,
                            isSelected = isSelected,
                            isPlaying = isSelected && isPlaying,
                            progress = if (isSelected) progress else 0f,
                            isFavorite = isFavorite,
                            onPlayPauseClick = {
                                if (isSelected) viewModel.togglePlayPause() else viewModel.playSong(song)
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                            onClick = { viewModel.playSong(song) }
                        )
                    }
                }
            }
        }
    }
}
