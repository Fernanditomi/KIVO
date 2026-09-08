package com.example.kivo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kivo.data.models.Song
import com.example.kivo.data.models.kivoSongs
import com.example.kivo.ui.components.DifficultyCardUnified
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.GameStatus
import com.example.kivo.ui.viewmodels.TapChallengeViewModel
import com.example.kivo.ui.viewmodels.TapDifficulty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapChallengeScreen(onBack: () -> Unit, viewModel: TapChallengeViewModel = viewModel()) {
    val gameState by viewModel.gameState

    Box(modifier = Modifier.fillMaxSize().background(KivoDeepDark)) {
        // Fondo con brillo radial suave en la parte superior
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(KivoPurple.copy(alpha = 0.15f), Color.Transparent),
                        startY = 0f,
                        endY = 500f
                    )
                )
        )

        if (gameState.currentSong != null) {
            AsyncImage(
                model = gameState.currentSong!!.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(50.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.2f
            )
        }

        when {
            gameState.currentSong == null -> {
                TapSongSelection(onSongSelected = { viewModel.selectSong(it) }, onBack = onBack)
            }
            gameState.status == GameStatus.IDLE -> {
                TapDifficultySelection(
                    onDifficultySelected = { viewModel.selectDifficulty(it) },
                    onStart = { viewModel.startCountdown() }
                )
            }
            else -> {
                TapGameContent(viewModel = viewModel)
            }
        }

        if (gameState.status == GameStatus.COUNTDOWN && gameState.countdown != null) {
            TapCountdownOverlay(gameState.countdown!!)
        }

        if (gameState.isPaused) {
            TapPauseMenu(
                onContinue = { viewModel.resumeGame() },
                onRestart = { viewModel.restartGame() },
                onExit = onBack
            )
        }

        if (gameState.status == GameStatus.GAMEOVER) {
            TapGameOverScreen(
                score = gameState.score,
                maxCombo = gameState.maxCombo,
                highScore = gameState.highScore,
                onRestart = { viewModel.restartGame() },
                onBack = onBack
            )
        }

        if (gameState.status == GameStatus.VICTORY) {
            TapVictoryScreen(
                score = gameState.score,
                maxCombo = gameState.maxCombo,
                onRestart = { viewModel.restartGame() },
                onBack = onBack
            )
        }
    }
}

@Composable
fun TapSongSelection(onSongSelected: (Song) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
            }
            Text("TAP CHALLENGE", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(kivoSongs) { song ->
                Card(
                    onClick = { onSongSelected(song) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161822).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    border = borderStrokeSlight()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, fontSize = 12.sp, color = Color(0xFFA0A5B5), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        
                        // Circular Play Button Style
                        Surface(
                            modifier = Modifier.size(36.dp),
                            color = KivoPurple.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = KivoNeonPurple, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TapDifficultySelection(onDifficultySelected: (TapDifficulty) -> Unit, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(KivoPurple.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(50.dp), tint = KivoNeonPurple)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text("DIFICULTAD", fontWeight = FontWeight.Black, color = Color.White, fontSize = 24.sp)
        Text("Pon a prueba tu velocidad", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        
        Spacer(modifier = Modifier.height(40.dp))
        
        DifficultyCardUnified(
            label = "FÁCIL",
            desc = "Ritmo suave y lento",
            reward = "x1 Pts",
            icon = Icons.Default.Timer,
            startColor = Diff_Easy_Start,
            endColor = Diff_Easy_End,
            onClick = { onDifficultySelected(TapDifficulty.EASY) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyCardUnified(
            label = "NORMAL",
            desc = "Velocidad estándar",
            reward = "x2 Pts",
            icon = Icons.Default.Bolt,
            startColor = Diff_Normal_Start,
            endColor = Diff_Normal_End,
            onClick = { onDifficultySelected(TapDifficulty.NORMAL) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyCardUnified(
            label = "DIFÍCIL",
            desc = "Velocidad extrema",
            reward = "x3 Pts",
            icon = Icons.Default.Whatshot,
            startColor = Diff_Hard_Start,
            endColor = Diff_Hard_End,
            onClick = { onDifficultySelected(TapDifficulty.HARD) }
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Neon Purple Capsule Button
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(15.dp, RoundedCornerShape(100.dp), spotColor = KivoAction_Start),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(100.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(KivoAction_Start, KivoAction_End))),
                contentAlignment = Alignment.Center
            ) {
                Text("¡INICIAR!", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun TapGameContent(viewModel: TapChallengeViewModel) {
    val state by viewModel.gameState
    val beatMap by viewModel.songBeatMap.collectAsState()
    val gameTimeMs by viewModel.gameTimeMs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(state.currentSong?.title ?: "", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                val diffLabel = when(state.difficulty) {
                    TapDifficulty.EASY -> "FÁCIL"
                    TapDifficulty.NORMAL -> "NORMAL"
                    TapDifficulty.HARD -> "DIFÍCIL"
                }
                val diffColor = when(state.difficulty) {
                    TapDifficulty.EASY -> Diff_Easy_Start
                    TapDifficulty.NORMAL -> Diff_Normal_Start
                    TapDifficulty.HARD -> Diff_Hard_Start
                }
                Text("Dificultad: $diffLabel", color = diffColor, fontWeight = FontWeight.Black, fontSize = 10.sp)
                Text("Combo: x${state.combo}", color = KivoNeonPurple, fontWeight = FontWeight.Black)
            }
            Text("Score: ${state.score}", fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)
        }

        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = KivoPurple,
            trackColor = Color.White.copy(alpha = 0.1f)
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .pointerInput(Unit) {
                    detectTapGestures { _ ->
                        viewModel.onEmptyTap()
                    }
                }
        ) {
            val colWidth = maxWidth / 4
            val screenHeight = maxHeight

            Row(modifier = Modifier.fillMaxSize()) {
                repeat(4) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.05f)))
                }
            }

            beatMap.forEach { note ->
                val timeDiff = note.targetTimeMs - gameTimeMs
                val travelTime = state.difficulty.travelTime
                
                if (timeDiff <= travelTime && timeDiff > -500 && !note.isHit) {
                    val progressY = 1f - (timeDiff.toFloat() / travelTime)
                    
                    Box(
                        modifier = Modifier
                            .size(colWidth, screenHeight * 0.18f)
                            .graphicsLayer {
                                translationX = (colWidth * note.column).toPx()
                                translationY = (screenHeight * progressY).toPx()
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black)
                            .border(2.dp, KivoNeonPurple, RoundedCornerShape(14.dp))
                            .pointerInput(note.id) {
                                detectTapGestures { _ ->
                                    viewModel.onNoteTap(note.id)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = KivoNeonPurple.copy(alpha = 0.4f))
                    }
                }
            }

            IconButton(
                onClick = { viewModel.pauseGame() },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(Icons.Default.Pause, contentDescription = "Pausa", tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun borderStrokeSlight() = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))

@Composable
fun TapCountdownOverlay(count: Int) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
        Text(text = count.toString(), fontSize = 120.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
fun TapPauseMenu(onContinue: () -> Unit, onRestart: () -> Unit, onExit: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PAUSA", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.White)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("CONTINUAR") }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("REINTENTAR") }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onExit) { Text("SALIR AL MENÚ", color = Color.Gray) }
            }
        }
    }
}

@Composable
fun TapGameOverScreen(score: Int, maxCombo: Int, highScore: Int, onRestart: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GAME OVER", fontWeight = FontWeight.Black, fontSize = 48.sp, color = Color.Red)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Puntos: $score", fontSize = 24.sp, color = Color.White)
            Text("Combo: x$maxCombo", color = KivoNeonPurple)
            Text("Récord: $highScore", color = Color.Gray)
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth(0.7f).height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = KivoPurple)) {
                Text("REINTENTAR", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack) { Text("SALIR", color = Color.Gray) }
        }
    }
}

@Composable
fun TapVictoryScreen(score: Int, maxCombo: Int, onRestart: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color.Yellow)
            Text("¡COMPLETADO!", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Puntos: $score", fontSize = 24.sp, color = KivoNeonPurple, fontWeight = FontWeight.Bold)
            Text("Combo: x$maxCombo", color = Color.White)
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onRestart, modifier = Modifier.fillMaxWidth(0.7f).height(60.dp)) {
                Text("VOLVER A JUGAR", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack) { Text("VOLVER AL MENÚ", color = Color.Gray) }
        }
    }
}
