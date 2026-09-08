package com.example.kivo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.kivo.ui.theme.KivoBlack
import com.example.kivo.ui.theme.KivoPurple

@Composable
fun ShortsScreen() {
    val pagerState = rememberPagerState(pageCount = { 10 })
    
    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        ShortItem(page)
    }
}

@Composable
fun ShortItem(page: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(KivoPurple.copy(alpha = 0.2f), KivoBlack)
                )
            )
    ) {
        // Mock Content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡ KIVO SHORT #$page",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
        
        // UI Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "@usuario_kivo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Este es un chisme increíble que acaba de pasar 🔥 #Tendencia #Kivo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShortAction(icon = Icons.Default.Favorite, label = "21K")
                    ShortAction(icon = Icons.Default.Chat, label = "8.2K")
                    ShortAction(icon = Icons.Default.Share, label = "Share")
                    ShortAction(icon = Icons.Default.Bookmark, label = "Save")
                }
            }
        }
    }
}

@Composable
fun ShortAction(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        IconButton(onClick = { /* TODO */ }) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}
