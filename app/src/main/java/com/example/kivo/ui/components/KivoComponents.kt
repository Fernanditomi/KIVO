package com.example.kivo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kivo.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Composable
fun KivoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = KivoSurface2,
    gradient: List<Color>? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    Card(
        modifier = modifier
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (gradient != null) Brush.linearGradient(gradient)
                    else Brush.verticalGradient(listOf(backgroundColor, backgroundColor))
                )
                .clickable(
                    enabled = onClick != null,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = { onClick?.invoke() }
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun KivoButton(
    text: String,
    modifier: Modifier = Modifier,
    gradient: List<Color> = GradientAction,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, label = "scale")

    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .scale(scale)
            .shadow(10.dp, RoundedCornerShape(28.dp), spotColor = gradient.first().copy(alpha = 0.4f)),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun KivoSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier.padding(vertical = 16.dp),
        letterSpacing = 1.sp
    )
}

@Composable
fun KivoProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(KivoBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(GradientAction))
        )
    }
}

@Composable
fun KivoIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun KivoBadgedIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    badgeCount: Int,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Box(contentAlignment = Alignment.TopEnd) {
        KivoIconButton(icon = icon, onClick = onClick, modifier = modifier, tint = tint)
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 4.dp)
                    .clip(CircleShape)
                    .background(KivoPink)
                    .border(1.5.dp, KivoBlack, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun KivoTrendCard(trend: com.example.kivo.data.models.Trend) {
    KivoCard(
        backgroundColor = KivoSurface1,
        modifier = Modifier.border(1.dp, KivoBorder, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trend.tag,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (trend.tag.contains("Drama")) KivoPink else Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Whatshot, null, tint = KivoOrange, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(text = "Popular ahora", style = MaterialTheme.typography.labelMedium, color = KivoTextSecondary)
                }
                Text(
                    text = "${trend.comments} personas participando",
                    style = MaterialTheme.typography.labelSmall,
                    color = KivoTextDisabled,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = KivoTextDisabled)
        }
    }
}
