package com.example.kivo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kivo.ui.components.*
import com.example.kivo.ui.theme.*

@Composable
fun GamesScreen(
    onNavigateToViralTrivia: () -> Unit,
    onNavigateToTrivia: () -> Unit,
    onNavigateToTapChallenge: () -> Unit,
    onNavigateToWhoSaidThat: () -> Unit,
    onNavigateToSaveSong: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KivoBlack
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = "KIVO GAMES",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Juega. Compite. Demuestra lo que sabes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KivoTextSecondary
                    )
                }
            }

            // Active Streak Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                KivoActiveStreakCard()
            }

            // Game Grid
            items(gameHubListPro) { game ->
                KivoGamePremiumCard(
                    game = game,
                    onClick = {
                        when (game.title) {
                            "Trivia Viral" -> onNavigateToViralTrivia()
                            "¿Quién dijo eso?" -> onNavigateToWhoSaidThat()
                            "Adivina la canción" -> onNavigateToTrivia()
                            "Tap Challenge" -> onNavigateToTapChallenge()
                            "¿Qué canción salvas?" -> onNavigateToSaveSong()
                        }
                    }
                )
            }
            
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun KivoActiveStreakCard() {
    KivoCard(
        backgroundColor = KivoSurface2
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(KivoOrange.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Whatshot, null, tint = KivoOrange, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "7 días de racha",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Continúa jugando para mantenerla.",
                        style = MaterialTheme.typography.labelSmall,
                        color = KivoTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun KivoGamePremiumCard(game: ModernGamePro, onClick: () -> Unit) {
    KivoCard(
        backgroundColor = KivoSurface1,
        onClick = onClick,
        modifier = Modifier.height(200.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(KivoPurpleDeep.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(game.icon, null, tint = KivoPurpleElectric, modifier = Modifier.size(28.dp))
            }
            
            Column {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = game.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = KivoTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = onClick,
                    modifier = Modifier.size(36.dp),
                    color = Color.White,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow, 
                            null, 
                            tint = KivoPurpleMain, 
                            modifier = Modifier.size(20.dp).padding(start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

data class ModernGamePro(
    val title: String,
    val description: String,
    val icon: ImageVector
)

val gameHubListPro = listOf(
    ModernGamePro("Trivia Viral", "Cultura pop y desafíos", Icons.Default.Lightbulb),
    ModernGamePro("¿Quién dijo eso?", "Letras y artistas", Icons.Default.QuestionAnswer),
    ModernGamePro("Adivina la canción", "Reto musical rítmico", Icons.Default.MusicNote),
    ModernGamePro("Tap Challenge", "Velocidad y reflejos", Icons.Default.TouchApp),
    ModernGamePro("¿Qué canción salvas?", "Vota por tu favorita", Icons.Default.CompareArrows),
    ModernGamePro("Reto diario", "Gana XP extra hoy", Icons.Default.EmojiEvents)
)
