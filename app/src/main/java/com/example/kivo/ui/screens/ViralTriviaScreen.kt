package com.example.kivo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.QuestionDifficulty
import com.example.kivo.ui.viewmodels.ViralTriviaViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViralTriviaScreen(onBack: () -> Unit, viewModel: ViralTriviaViewModel = viewModel()) {
    val state by viewModel.state

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KivoDeepDark
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("TRIVIA VIRAL", fontWeight = FontWeight.Black, color = KivoNeonPurple) },
                    navigationIcon = {
                        IconButton(onClick = if (state.selectedDifficulty == null) onBack else { { viewModel.resetToMenu() } }) {
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
                when {
                    state.selectedDifficulty == null -> {
                        ViralDifficultySelection(onDifficultySelected = { viewModel.startNewGame(it) })
                    }
                    state.isGameOver -> {
                        ViralGameOverScreen(
                            score = state.score, 
                            onRestart = { viewModel.startNewGame(state.selectedDifficulty!!) }, 
                            onBack = { viewModel.resetToMenu() }
                        )
                    }
                    state.questions.isNotEmpty() -> {
                        ViralTriviaGameplay(
                            state = state, 
                            onAnswer = { viewModel.submitAnswer(it) }, 
                            onNext = { viewModel.nextQuestion() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ViralDifficultySelection(onDifficultySelected: (QuestionDifficulty) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Bulb Icon in soft purple circle
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(KivoPurple.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(50.dp), tint = KivoNeonPurple)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "¿Cuánto sabes de música?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Selecciona un nivel y demuestra que eres un experto.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        DifficultyCardUnified(
            label = "FÁCIL",
            desc = "Artistas Pop y datos globales",
            reward = "x1 Puntos",
            icon = Icons.Default.Timer,
            startColor = Diff_Easy_Start,
            endColor = Diff_Easy_End,
            onClick = { onDifficultySelected(QuestionDifficulty.EASY) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyCardUnified(
            label = "NORMAL",
            desc = "Géneros y detalles de artistas",
            reward = "x2 Puntos",
            icon = Icons.Default.Bolt,
            startColor = Diff_Normal_Start,
            endColor = Diff_Normal_End,
            onClick = { onDifficultySelected(QuestionDifficulty.NORMAL) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyCardUnified(
            label = "DIFÍCIL",
            desc = "Historia, técnica y nichos",
            reward = "x3 Puntos",
            icon = Icons.Default.Whatshot,
            startColor = Diff_Hard_Start,
            endColor = Diff_Hard_End,
            onClick = { onDifficultySelected(QuestionDifficulty.HARD) }
        )
    }
}

@Composable
fun DifficultyCardUnified(
    label: String, 
    desc: String, 
    reward: String, 
    icon: ImageVector, 
    startColor: Color, 
    endColor: Color, 
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(startColor, endColor)))
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = label, fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)
                    Text(text = desc, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, maxLines = 1)
                }
                
                Surface(
                    color = Diff_Badge_Bg,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = reward,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ViralTriviaGameplay(
    state: com.example.kivo.ui.viewmodels.ViralTriviaState,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit
) {
    val currentQuestion = state.questions[state.currentQuestionIndex]

    LaunchedEffect(state.selectedAnswer) {
        if (state.selectedAnswer != null) {
            delay(2000)
            onNext()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Pregunta ${state.currentQuestionIndex + 1}/10", color = Color.White.copy(alpha = 0.6f))
                val diffLabel = when(state.selectedDifficulty) {
                    QuestionDifficulty.EASY -> "FÁCIL"
                    QuestionDifficulty.NORMAL -> "NORMAL"
                    QuestionDifficulty.HARD -> "DIFÍCIL"
                    else -> ""
                }
                val diffColor = when(state.selectedDifficulty) {
                    QuestionDifficulty.EASY -> Diff_Easy_Start
                    QuestionDifficulty.NORMAL -> Diff_Normal_Start
                    QuestionDifficulty.HARD -> Diff_Hard_Start
                    else -> Color.Gray
                }
                Text("Modo: $diffLabel", color = diffColor, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
            Text("Score: ${state.score}", color = KivoNeonPurple, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Question Card
        Card(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 150.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentQuestion.text,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Options
        currentQuestion.options.forEach { option ->
            val isSelected = state.selectedAnswer == option
            val isCorrect = option == currentQuestion.correctAnswer
            
            val borderColor = when {
                state.selectedAnswer == null -> Color.Transparent
                isCorrect -> Color(0xFF00B894)
                isSelected -> Color(0xFFD63031)
                else -> Color.Transparent
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable(enabled = state.selectedAnswer == null) { onAnswer(option) },
                color = if (isSelected || (state.selectedAnswer != null && isCorrect)) borderColor.copy(alpha = 0.1f) else Color(0xFF1E1C2A),
                shape = RoundedCornerShape(16.dp),
                border = if (borderColor != Color.Transparent) androidx.compose.foundation.BorderStroke(2.dp, borderColor) else null
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        color = if (borderColor != Color.Transparent) Color.White else Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp,
                        fontWeight = if (borderColor != Color.Transparent) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ViralGameOverScreen(score: Int, onRestart: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(100.dp), tint = KivoNeonPurple)
        Spacer(modifier = Modifier.height(24.dp))
        Text("¡TRIVIA FINALIZADA!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
        Text("Tu puntaje: $score", style = MaterialTheme.typography.headlineSmall, color = KivoNeonPurple, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KivoNeonPurple)
        ) {
            Text("REINTENTAR NIVEL", fontWeight = FontWeight.Bold, color = Color.Black)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onBack) {
            Text("CAMBIAR DIFICULTAD", color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}
