package com.example.kivo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.kivo.data.models.Song
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.MusicViewModel

@Composable
fun MusicScreen(
    viewModel: MusicViewModel,
    onYouTubeClick: () -> Unit = {},
    onSavedSongsClick: () -> Unit = {}
) {
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
            Text(
                text = "MÚSICA",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp),
                letterSpacing = 1.sp
            )

            // 1. Canciones guardadas (tarjeta/botón principal)
            KivoSavedSongsCard(
                count = songs.size,
                onClick = onSavedSongsClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. YouTube (fila simple, mismo estilo que las canciones)
            YouTubeListRow(onClick = onYouTubeClick)
        }
    }
}

@Composable
fun KivoSavedSongsCard(
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(KivoPurpleDeep, KivoSurface2)))
            .border(0.5.dp, KivoPurpleMain.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(KivoPurpleMain, KivoPurpleElectric))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Canciones guardadas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (count == 1) "1 canción descargada" else "$count canciones descargadas",
                style = MaterialTheme.typography.labelMedium,
                color = KivoTextSecondary
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun YouTubeListRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(KivoSurface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KivoSurface3),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.SmartDisplay,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = "YouTube",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Busca y reproduce música de YouTube",
                style = MaterialTheme.typography.labelMedium,
                color = KivoTextSecondary,
                maxLines = 1
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = KivoTextDisabled,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun KivoMusicItemPro(
    song: Song,
    isSelected: Boolean,
    isPlaying: Boolean,
    progress: Float,
    isFavorite: Boolean,
    onPlayPauseClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) KivoSurface2 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(KivoSurface3)
            ) {
                AsyncImage(
                    model = song.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = KivoPurpleElectric,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) KivoPurpleElectric else Color.White,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelMedium,
                    color = KivoTextSecondary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }

            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isSelected && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isSelected) KivoPurpleElectric else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) KivoPink else KivoTextDisabled,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = song.duration,
                style = MaterialTheme.typography.labelSmall,
                color = KivoTextDisabled,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (isSelected && progress > 0f) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = KivoPurpleElectric,
                trackColor = Color.White.copy(alpha = 0.08f)
            )
        }
    }
}