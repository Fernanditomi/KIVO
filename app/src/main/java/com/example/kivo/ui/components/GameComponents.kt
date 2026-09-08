package com.example.kivo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kivo.ui.theme.Diff_Badge_Bg

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
                // White Icon
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                Spacer(modifier = Modifier.width(16.dp))
                
                // Text Column (Pure White)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = label, fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)
                    Text(text = desc, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, maxLines = 1)
                }
                
                // Points Badge (Black semi-transparent)
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
