package com.example.kivo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.kivo.data.models.Song
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.MusicViewModel
import com.example.kivo.ui.components.KivoSectionTitle

@Composable
fun MusicScreen(
    viewModel: MusicViewModel,
    onYouTubeClick: () -> Unit = {}
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val favoriteIds by viewModel.favoriteSongIds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    
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
            
            // Search Bar (PRO Redesign)
            KivoSearchBarPro(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // YouTube Search Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onYouTubeClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KivoSurface2
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Buscar en KIVO",
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            KivoSectionTitle(text = if (searchQuery.isEmpty()) "Populares" else "Resultados")
            
            if (filteredSongs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No se encontraron canciones", color = KivoTextDisabled)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredSongs,
                        key = { it.id }
                    ) { song ->
                        val isSelected = remember(currentSong?.id) { currentSong?.id == song.id }
                        val isFavorite = remember(favoriteIds) { favoriteIds.contains(song.id) }
                        
                        KivoMusicItemPro(
                            song = song,
                            isSelected = isSelected,
                            isFavorite = isFavorite,
                            onFavoriteClick = { viewModel.toggleFavorite(song.id) },
                            onClick = { viewModel.playSong(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KivoSearchBarPro(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp)),
        placeholder = { Text("Buscar canción, artista o álbum...", color = KivoTextDisabled, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KivoTextDisabled) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
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
}

@Composable
fun KivoMusicItemPro(
    song: Song,
    isSelected: Boolean,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
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
            .background(if (isSelected) KivoSurface2 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                         Icons.Default.PlayArrow, 
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
}
