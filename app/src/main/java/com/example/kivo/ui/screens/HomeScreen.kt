package com.example.kivo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kivo.data.models.dummyTrends
import com.example.kivo.ui.components.*
import com.example.kivo.ui.theme.*

@Composable
fun HomeScreen(onShortsClick: () -> Unit, onAiClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KivoBlack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "¿QUÉ ESTÁ PASANDO HOY?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Descubre las últimas novedades en Kivo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KivoTextSecondary
                    )
                }
            }

            item {
                KivoShortsPremiumCard(onShortsClick)
            }

            item {
                KivoDailyPremiumCard()
            }
            
            item {
                KivoSectionTitle(text = "🔥 Tendencias")
            }
            
            items(dummyTrends.take(3)) { trend ->
                KivoTrendCard(trend)
            }
            
            item {
                Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar
            }
        }
    }
}

@Composable
fun KivoShortsPremiumCard(onClick: () -> Unit) {
    KivoCard(
        onClick = onClick,
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
                        .size(48.dp)
                        .background(KivoPurpleDeep.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = KivoPurpleElectric, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "KIVO SHORTS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Descubre lo más popular ahora",
                        style = MaterialTheme.typography.labelMedium,
                        color = KivoTextSecondary
                    )
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = KivoTextDisabled)
        }
    }
}

@Composable
fun KivoDailyPremiumCard() {
    KivoCard(
        gradient = listOf(KivoBlack, KivoPurpleDeep)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "TU KIVO DE HOY",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = KivoPurpleElectric
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            DailyPremiumItem(icon = Icons.Default.MusicNote, category = "Canción", info = "Starboy - The Weeknd", color = KivoPurpleMain)
            DailyPremiumItem(icon = Icons.Default.Whatshot, category = "Tendencia", info = "#DramaDelDía", color = KivoPink)
            DailyPremiumItem(icon = Icons.Default.Star, category = "Reto", info = "Consigue 500 XP", color = KivoBlue)
            DailyPremiumItem(icon = Icons.Default.Poll, category = "Vota", info = "¿Cuál es el mejor álbum?", color = KivoGreen)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Tu progreso de hoy",
                style = MaterialTheme.typography.labelMedium,
                color = KivoTextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                KivoProgressBar(progress = 0.8f, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "4 de 5",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            KivoButton(
                text = "¡COMPLETAR KIVO!",
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DailyPremiumItem(icon: androidx.compose.ui.graphics.vector.ImageVector, category: String, info: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = category, style = MaterialTheme.typography.labelSmall, color = KivoTextSecondary)
            Text(text = info, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}
