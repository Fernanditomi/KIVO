package com.example.kivo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.GameMode
import com.example.kivo.ui.viewmodels.GameSong
import com.example.kivo.ui.viewmodels.QueCancionSalvasViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueCancionSalvasScreen(onBack: () -> Unit, viewModel: QueCancionSalvasViewModel = viewModel()) {
    val state by viewModel.state

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0D0E15)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(KivoPurple.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
            )

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("¿QUÉ CANCIÓN SALVAS?", fontWeight = FontWeight.Black, color = KivoNeonPurple, fontSize = 18.sp) },
                        navigationIcon = {
                            IconButton(onClick = { if (state.mode == null) onBack() else viewModel.resetToMenu() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    when {
                        state.mode == null -> {
                            SaveSongMenu(
                                ranking = state.ranking,
                                onModeSelected = { viewModel.setMode(it) }
                            )
                        }
                        state.isGameOver -> {
                            SaveSongFinalScreen(
                                winner = state.gameWinner!!,
                                onRestart = { viewModel.setMode(state.mode ?: GameMode.SURVIVAL) },
                                onBack = { viewModel.resetToMenu() }
                            )
                        }
                        state.winner != null && state.challenger != null -> {
                            SaveSongGameplay(
                                state = state,
                                onPlay = { viewModel.playSong(it.id, it.previewUrl ?: "") },
                                onSelect = { viewModel.selectWinner(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaveSongMenu(ranking: List<GameSong>, onModeSelected: (GameMode) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.CompareArrows, null, modifier = Modifier.size(60.dp), tint = KivoNeonPurple)
        Spacer(modifier = Modifier.height(16.dp))
        Text("ELIGE TU MODO", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ModeCard("SUPERVIVENCIA", "La ganadora se enfrenta a nuevos rivales", Icons.Default.FlashOn, G_Purple_Start, G_Purple_End) {
                    onModeSelected(GameMode.SURVIVAL)
                }
            }
            item {
                Text("RANKING GLOBAL", color = KivoNeonPurple, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            }
            items(ranking) { song ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.albumImage,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(song.artist, color = Color.Gray, fontSize = 12.sp)
                    }
                    Text("${song.wins} 🏆", color = Color.Yellow, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun ModeCard(title: String, desc: String, icon: ImageVector, startColor: Color, endColor: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(startColor, endColor)))) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(desc, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SaveSongGameplay(
    state: com.example.kivo.ui.viewmodels.QueCancionSalvasState,
    onPlay: (GameSong) -> Unit,
    onSelect: (GameSong) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("RONDA ${state.round}", color = Color.White, fontWeight = FontWeight.Bold)
            Text("SALVADAS: ${state.savedCount}", color = KivoNeonPurple, fontWeight = FontWeight.ExtraBold)
        }
        
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
            color = KivoPurple,
            trackColor = Color.White.copy(alpha = 0.1f)
        )

        SongDuelCard(
            song = state.winner!!,
            isPlaying = state.playingSongId == state.winner!!.id,
            onPlay = { onPlay(state.winner!!) },
            onSelect = { onSelect(state.winner!!) },
            isWinner = true
        )

        Text(
            "VS",
            color = KivoNeonPurple,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            modifier = Modifier.shadow(20.dp, CircleShape, spotColor = KivoPurple)
        )

        SongDuelCard(
            song = state.challenger!!,
            isPlaying = state.playingSongId == state.challenger!!.id,
            onPlay = { onPlay(state.challenger!!) },
            onSelect = { onSelect(state.challenger!!) },
            isWinner = false
        )
    }
}

@Composable
fun SongDuelCard(
    song: GameSong,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onSelect: () -> Unit,
    isWinner: Boolean
) {
    // Usamos key para que el estado se reinicie cuando cambie la canción
    key(song.id) {
        var isSelected by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isSelected) 1.05f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "scale"
        )

        Card(
            onClick = { 
                if (!isSelected) {
                    isSelected = true
                    onSelect()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .scale(scale)
                .shadow(if (isPlaying) 20.dp else 0.dp, RoundedCornerShape(24.dp), spotColor = KivoNeonPurple),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = if (isWinner) 
                                listOf(Color(0xFF2E1A47), Color(0xFF161822)) 
                            else 
                                listOf(Color(0xFF161822), Color(0xFF1A2133))
                        )
                    )
                    .border(
                        width = if (isPlaying) 2.dp else 1.dp,
                        color = if (isPlaying) KivoNeonPurple else Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = song.albumImage,
                            contentDescription = null,
                            modifier = Modifier.size(110.dp).clip(RoundedCornerShape(16.dp)).background(Color.DarkGray),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            onClick = onPlay,
                            modifier = Modifier.size(40.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = KivoPurple.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, KivoNeonPurple.copy(alpha = 0.3f))
                        ) {
                            Text("SALVAR", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = KivoNeonPurple, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaveSongFinalScreen(winner: GameSong, onRestart: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("TU CANCIÓN GANADORA", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = KivoNeonPurple)
        Spacer(modifier = Modifier.height(32.dp))
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(260.dp).shadow(50.dp, RoundedCornerShape(32.dp), spotColor = KivoPurple).background(KivoPurple.copy(alpha = 0.1f), RoundedCornerShape(32.dp)))
            AsyncImage(model = winner.albumImage, contentDescription = null, modifier = Modifier.size(240.dp).clip(RoundedCornerShape(32.dp)), contentScale = ContentScale.Crop)
            Icon(Icons.Default.Star, null, tint = Color.Yellow, modifier = Modifier.size(48.dp).align(Alignment.TopEnd).offset(x = 12.dp, y = (-12).dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(winner.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center)
        Text(winner.artist, style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Victorias totales: ${winner.wins}", color = KivoNeonPurple, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = KivoPurple), shape = RoundedCornerShape(16.dp)) {
            Text("JUGAR DE NUEVO", fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { /* TODO: Share */ }, modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))) {
            Text("COMPARTIR RESULTADO", color = Color.White)
        }
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("VOLVER AL MENÚ", color = Color.Gray) }
    }
}
