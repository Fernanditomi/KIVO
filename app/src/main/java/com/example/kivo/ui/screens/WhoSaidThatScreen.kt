package com.example.kivo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.kivo.ui.components.DifficultyCardUnified
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.WhoSaidThatStatus
import com.example.kivo.ui.viewmodels.WhoSaidThatViewModel

@Composable
fun WhoSaidThatScreen(onBack: () -> Unit, viewModel: WhoSaidThatViewModel = viewModel()) {
    val state by viewModel.state

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KivoDeepDark
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            WhoSaidThatHeader(
                title = "¿QUIÉN DIJO ESO?",
                onBack = { if (state.status == WhoSaidThatStatus.MENU) onBack() else viewModel.resetToMenu() }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (state.status) {
                    WhoSaidThatStatus.MENU -> WhoSaidThatMenu(viewModel)
                    WhoSaidThatStatus.DIFFICULTY_SELECT -> WhoSaidThatDifficultySelect(viewModel)
                    WhoSaidThatStatus.PLAYING -> WhoSaidThatGameplay(viewModel)
                    WhoSaidThatStatus.RESULTS -> WhoSaidThatResults(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoSaidThatHeader(title: String, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(title, fontWeight = FontWeight.Black, color = KivoNeonPurple, fontSize = 20.sp) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun WhoSaidThatMenu(viewModel: WhoSaidThatViewModel) {
    val state by viewModel.state
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(KivoPurple.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(50.dp), tint = KivoNeonPurple)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "¿Quién dijo eso?", 
            style = MaterialTheme.typography.headlineMedium, 
            fontWeight = FontWeight.ExtraBold, 
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Identifica al artista por la frase de su canción.", 
            style = MaterialTheme.typography.bodyMedium, 
            color = Color.White.copy(alpha = 0.7f), 
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(label = "Récord", value = "${state.highScore}", icon = Icons.Default.EmojiEvents, modifier = Modifier.weight(1f))
            StatCard(label = "Mejor Racha", value = "${state.maxStreak}", icon = Icons.Default.Whatshot, modifier = Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        MenuButton("JUGAR CLÁSICO", KivoPurple) { viewModel.selectMode("Clásico") }
        Spacer(modifier = Modifier.height(12.dp))
        MenuButton("10 PREGUNTAS", KivoBlue) { viewModel.selectMode("10 Preguntas") }
    }
}

@Composable
fun WhoSaidThatDifficultySelect(viewModel: WhoSaidThatViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SELECCIONA DIFICULTAD", fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(32.dp))
        
        DifficultyCardUnified(
            label = "FÁCIL",
            desc = "Letras icónicas y conocidas",
            reward = "x1 Pts",
            icon = Icons.Default.Timer,
            startColor = Diff_Easy_Start,
            endColor = Diff_Easy_End,
            onClick = { viewModel.startGame(difficulty = "Fácil") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyCardUnified(
            label = "NORMAL",
            desc = "Retos de nivel medio",
            reward = "x2 Pts",
            icon = Icons.Default.Bolt,
            startColor = Diff_Normal_Start,
            endColor = Diff_Normal_End,
            onClick = { viewModel.startGame(difficulty = "Normal") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyCardUnified(
            label = "DIFÍCIL",
            desc = "Solo para expertos reales",
            reward = "x3 Pts",
            icon = Icons.Default.Whatshot,
            startColor = Diff_Hard_Start,
            endColor = Diff_Hard_End,
            onClick = { viewModel.startGame(difficulty = "Difícil") }
        )
    }
}

@Composable
fun WhoSaidThatGameplay(viewModel: WhoSaidThatViewModel) {
    val state by viewModel.state
    val currentQuestion = state.questions[state.currentQuestionIndex]
    
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp).verticalScroll(rememberScrollState())
    ) {
        // Top Info
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Pregunta ${state.currentQuestionIndex + 1}/${state.questions.size}", color = Color.Gray, fontSize = 12.sp)
                val diffColor = when(state.difficulty) {
                    "Fácil" -> Diff_Easy_Start
                    "Normal" -> Diff_Normal_Start
                    "Difícil" -> Diff_Hard_Start
                    else -> Color.Gray
                }
                Text("Modo: ${state.difficulty.uppercase()}", color = diffColor, fontWeight = FontWeight.Black, fontSize = 10.sp)
                Text("⭐ ${state.score}", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
            }
            
            if (state.streak > 0) {
                Surface(color = Color(0xFFFD79A8).copy(alpha = 0.2f), shape = RoundedCornerShape(100.dp)) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥 Racha x${state.streak}", color = Color(0xFFFD79A8), fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Row {
                repeat(3) { index ->
                    Icon(
                        if (index < state.lives) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (index < state.lives) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LinearProgressIndicator(
            progress = { state.timeLeft / 15f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(100.dp)),
            color = if (state.timeLeft > 5) KivoNeonPurple else Color.Red,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Lyric Card
        Card(
            modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = KivoPurple, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "\"${currentQuestion.quote}\"",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("¿Quién interpreta esta canción?", color = KivoNeonPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Options
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.currentOptions.forEach { option ->
                val isSelected = state.selectedAnswer == option
                val isCorrect = option == currentQuestion.correctAnswer
                
                val backgroundColor = when {
                    state.selectedAnswer == null -> Color(0xFF1E1C2A)
                    isCorrect -> Color(0xFF00B894).copy(alpha = 0.2f)
                    isSelected -> Color(0xFFD63031).copy(alpha = 0.2f)
                    else -> Color(0xFF1E1C2A)
                }
                
                val borderColor = when {
                    state.selectedAnswer == null -> Color.Transparent
                    isCorrect -> Color(0xFF00B894)
                    isSelected -> Color(0xFFD63031)
                    else -> Color.Transparent
                }

                Surface(
                    onClick = { if (state.selectedAnswer == null) viewModel.submitAnswer(option) },
                    color = backgroundColor,
                    shape = RoundedCornerShape(16.dp),
                    border = if (borderColor != Color.Transparent) androidx.compose.foundation.BorderStroke(2.dp, borderColor) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = option,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.selectedAnswer != null) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Default.CheckCircle else if (isSelected) Icons.Default.Cancel else Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = if (isCorrect) Color(0xFF00B894) else if (isSelected) Color(0xFFD63031) else Color.Gray.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun WhoSaidThatResults(viewModel: WhoSaidThatViewModel) {
    val state by viewModel.state
    val precision = if (state.totalPlayed > 0) (state.totalCorrect.toFloat() / state.totalPlayed * 100).toInt() else 0
    
    val feedbackMessage = when {
        precision == 100 -> "👑 ¡PERFECTO! ¡Eres una enciclopedia musical!"
        precision >= 81 -> "🔥 ¡Eres un experto musical!"
        precision >= 61 -> "¡Muy buen conocimiento musical!"
        precision >= 31 -> "¡Nada mal! Puedes mejorar."
        else -> "Necesitas escuchar más música 😅"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("¡PARTIDA TERMINADA!", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text(feedbackMessage, color = KivoNeonPurple, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ResultStatCard("PUNTOS", "${state.score}", KivoPurple, Modifier.weight(1f))
            ResultStatCard("PRECISIÓN", "$precision%", KivoBlue, Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ResultStatCard("CORRECTAS", "${state.totalCorrect}", Color(0xFF00B894), Modifier.weight(1f))
            ResultStatCard("RACHA MÁX", "${state.maxStreak}", Color(0xFFFD79A8), Modifier.weight(1f))
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { viewModel.startGame(difficulty = state.difficulty) },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KivoPurple),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("JUGAR DE NUEVO", fontWeight = FontWeight.Black)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = { viewModel.resetToMenu() },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
        ) {
            Text("VOLVER AL MENÚ", color = Color.White)
        }
    }
}

@Composable
fun ResultStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, fontWeight = FontWeight.Black, color = Color.White, fontSize = 24.sp)
        }
    }
}

@Composable
fun MenuButton(text: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = KivoNeonPurple, modifier = Modifier.size(24.dp))
            Text(value, fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}
