package com.example.kivo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kivo.data.models.Song
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.TriviaDifficulty
import com.example.kivo.ui.viewmodels.TriviaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriviaScreen(onBack: () -> Unit, triviaViewModel: TriviaViewModel = viewModel()) {
    val gameState by triviaViewModel.gameState

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KivoDeepDark
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("ADIVINA LA CANCIÓN", fontWeight = FontWeight.Black, color = KivoNeonPurple) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = KivoDeepDark
                    )
                )
            },
            containerColor = KivoDeepDark
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (gameState.difficulty == null) {
                    TriviaDifficultySelection(onDifficultySelected = { triviaViewModel.startNewGame(it) })
                } else if (gameState.isGameOver) {
                    GameOverScreen(score = gameState.score, onRestart = { triviaViewModel.startNewGame(gameState.difficulty!!) }, onBack = onBack)
                } else {
                    TriviaGameplay(viewModel = triviaViewModel)
                }
            }
        }
    }
}

@Composable
fun TriviaDifficultySelection(onDifficultySelected: (TriviaDifficulty) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon in soft purple circle
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(KivoPurple.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(50.dp), tint = KivoNeonPurple)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "¿Qué tan bueno eres?", 
            style = MaterialTheme.typography.headlineMedium, 
            fontWeight = FontWeight.ExtraBold, 
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Adivina la canción escuchando solo un fragmento.", 
            style = MaterialTheme.typography.bodyMedium, 
            color = Color.White.copy(alpha = 0.7f), 
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        DifficultyCardUnified(
            label = "FÁCIL",
            desc = "Fragmentos de 10 segundos",
            reward = "x1 Pts",
            icon = Icons.Default.Timer,
            startColor = Diff_Easy_Start,
            endColor = Diff_Easy_End,
            onClick = { onDifficultySelected(TriviaDifficulty.EASY) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyCardUnified(
            label = "MEDIO",
            desc = "Fragmentos de 5 segundos",
            reward = "x2 Pts",
            icon = Icons.Default.Bolt,
            startColor = Diff_Normal_Start,
            endColor = Diff_Normal_End,
            onClick = { onDifficultySelected(TriviaDifficulty.MEDIUM) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyCardUnified(
            label = "DIFÍCIL",
            desc = "Solo 2.5 segundos de audio",
            reward = "x3 Pts",
            icon = Icons.Default.Whatshot,
            startColor = Diff_Hard_Start,
            endColor = Diff_Hard_End,
            onClick = { onDifficultySelected(TriviaDifficulty.HARD) }
        )
    }
}

@Composable
fun TriviaGameplay(viewModel: TriviaViewModel) {
    val gameState by viewModel.gameState
    val timeProgress by viewModel.timerProgress.collectAsState()
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Pregunta ${gameState.currentQuestionIndex + 1}/10",
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    // Indicador de dificultad
                    val diffLabel = when(gameState.difficulty) {
                        TriviaDifficulty.EASY -> "FÁCIL"
                        TriviaDifficulty.MEDIUM -> "NORMAL"
                        TriviaDifficulty.HARD -> "DIFÍCIL"
                        else -> ""
                    }
                    val diffColor = when(gameState.difficulty) {
                        TriviaDifficulty.EASY -> T_Easy_Start
                        TriviaDifficulty.MEDIUM -> T_Med_Start
                        TriviaDifficulty.HARD -> T_Hard_Start
                        else -> Color.Gray
                    }
                    Text(
                        text = "Modo: $diffLabel",
                        color = diffColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp
                    )
                }
                Text("Puntos: ${gameState.score}", color = KivoNeonPurple, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
        
        item {
            LinearProgressIndicator(
                progress = { timeProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(100.dp)),
                color = if (timeProgress > 0.3f) KivoNeonPurple else Color.Red,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                contentAlignment = Alignment.Center, 
                modifier = Modifier
                    .size(160.dp)
                    .clickable { viewModel.replaySnippet() }
            ) {
                val infiniteTransition = rememberInfiniteTransition()
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse)
                )
                
                Box(
                    modifier = Modifier
                        .size(130.dp * pulseScale)
                        .background(T_Eq_Start.copy(alpha = 0.1f), CircleShape)
                )
                
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .shadow(15.dp, CircleShape, spotColor = T_Eq_End)
                        .background(Brush.sweepGradient(listOf(T_Eq_Start, T_Eq_End, T_Eq_Start)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        items(gameState.options) { song ->
            OptionButton(
                song = song,
                isSelected = gameState.selectedSong?.id == song.id,
                isCorrect = gameState.correctSong?.id == song.id,
                showResult = gameState.selectedSong != null,
                onClick = { viewModel.submitAnswer(song) }
            )
        }
    }
}

@Composable
fun OptionButton(
    song: Song,
    isSelected: Boolean,
    isCorrect: Boolean,
    showResult: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        showResult && isCorrect -> Color(0xFF00B894)
        showResult && isSelected && !isCorrect -> Color(0xFFD63031)
        else -> Color.Transparent
    }
    
    val borderStroke = if (borderColor != Color.Transparent) BorderStroke(2.dp, borderColor) else null

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = T_Option_Bg),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GameOverScreen(score: Int, onRestart: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(120.dp), tint = G_Gold_Start)
        Spacer(modifier = Modifier.height(24.dp))
        Text("¡PARTIDA FINALIZADA!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
        Text("Has conseguido $score puntos", style = MaterialTheme.typography.headlineSmall, color = KivoNeonPurple, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(64.dp).shadow(10.dp, RoundedCornerShape(16.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = KivoNeonPurple),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("REINTENTAR", fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onBack) {
            Text("SALIR AL MENÚ", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
        }
    }
}
