package com.example.kivo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.util.Locale
import com.example.kivo.ui.theme.KivoBlack
import com.example.kivo.ui.theme.KivoPink
import com.example.kivo.ui.theme.KivoPurple
import com.example.kivo.ui.theme.KivoPurpleElectric
import com.example.kivo.ui.theme.KivoPurpleMain
import com.example.kivo.ui.viewmodels.MusicViewModel
import androidx.media3.common.Player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    viewModel: MusicViewModel,
    onAmbientClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onBack: () -> Unit
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val totalTime by viewModel.totalTime.collectAsState()
    val favoriteIds by viewModel.favoriteSongIds.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    if (currentSong == null) return
    val isFavorite = favoriteIds.contains(currentSong!!.id)
    val isLoading = playbackState == Player.STATE_BUFFERING
    
    var showMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(KivoPurple.copy(alpha = 0.3f), KivoBlack)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cerrar", modifier = Modifier.size(32.dp), tint = Color.White)
                }
                Text("REPRODUCIENDO", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Modo Ambiente") },
                            onClick = {
                                showMenu = false
                                onAmbientClick()
                            },
                            leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Ecualizador") },
                            onClick = {
                                showMenu = false
                                onEqualizerClick()
                            },
                            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Album Art
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(KivoPurple.copy(alpha = 0.2f))
            ) {
                AsyncImage(
                    model = currentSong!!.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.weight(0.5f))
            
            // Song Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong!!.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Text(
                        text = currentSong!!.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                }
                
                // Shuffle moved here
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Aleatorio",
                        modifier = Modifier.size(28.dp),
                        tint = if (isShuffleEnabled) KivoPurpleElectric else Color.White.copy(alpha = 0.6f)
                    )
                }

                IconButton(onClick = { viewModel.toggleFavorite(currentSong!!.id) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        modifier = Modifier.size(32.dp),
                        tint = if (isFavorite) KivoPink else Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progress Bar
            Slider(
                value = progress,
                onValueChange = { viewModel.updateProgress(it) },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = currentTime, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                Text(text = totalTime, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Controls Symmetrical (WITHOUT Shuffle or Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.seekBackward() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Replay10, contentDescription = "Retroceder 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                IconButton(onClick = { viewModel.previousSong() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(36.dp), tint = Color.White)
                }
                
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(68.dp), strokeWidth = 3.dp, color = KivoPurpleElectric)
                    }
                    FloatingActionButton(
                        onClick = { viewModel.togglePlayPause() },
                        containerColor = Color.White,
                        contentColor = KivoBlack,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            modifier = Modifier.size(36.dp),
                            tint = KivoPurpleMain
                        )
                    }
                }

                IconButton(onClick = { viewModel.nextSong() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(36.dp), tint = Color.White)
                }
                
                IconButton(onClick = { viewModel.seekForward() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Forward10, contentDescription = "Avanzar 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
        
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        )
    }
}
