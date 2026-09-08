package com.example.kivo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kivo.data.notifications.KivoNotification
import com.example.kivo.data.notifications.NotificationCategory
import com.example.kivo.data.notifications.NotificationPriority
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNotificationClick: (KivoNotification) -> Unit,
    onDiscoverClick: () -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val filtered by viewModel.filteredNotifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val unread by viewModel.unreadCount.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = KivoBlack,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "NOTIFICACIONES",
                        fontWeight = FontWeight.Black,
                        color = KivoPurpleElectric,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.markAllRead() }, enabled = unread > 0) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = "Marcar todo como leído",
                            tint = if (unread > 0) KivoPurpleElectric else KivoTextDisabled
                        )
                    }
                    IconButton(onClick = { showClearDialog = true }, enabled = filtered.isNotEmpty()) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            contentDescription = "Vaciar notificaciones",
                            tint = if (filtered.isNotEmpty()) KivoPink else KivoTextDisabled
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = KivoBlack)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterChipsRow(
                selected = viewModel.filter.collectAsState().value,
                onSelect = viewModel::setFilter
            )

            when {
                isLoading -> SkeletonList()
                error -> ErrorState(onRetry = viewModel::retry)
                filtered.isEmpty() -> EmptyState(
                    onDiscover = onDiscoverClick,
                    hasNotifications = notificationsCount(viewModel) > 0
                )
                else -> NotificationList(
                    notifications = filtered,
                    viewModel = viewModel,
                    onNotificationClick = onNotificationClick
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = KivoSurface2,
            title = {
                Text("Vaciar notificaciones", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("¿Borrar todas las notificaciones? Esta acción no se puede deshacer.", color = KivoTextSecondary)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAll()
                    showClearDialog = false
                }) {
                    Text("Borrar todo", color = KivoPink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar", color = KivoTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun notificationsCount(viewModel: NotificationViewModel): Int {
    val notifications by viewModel.notifications.collectAsState()
    return notifications.size
}

@Composable
private fun FilterChipsRow(
    selected: NotificationCategory?,
    onSelect: (NotificationCategory?) -> Unit
) {
    val options = listOf<NotificationCategory?>(
        null,
        NotificationCategory.MUSIC,
        NotificationCategory.GAMES,
        NotificationCategory.SOCIAL,
        NotificationCategory.DOWNLOADS,
        NotificationCategory.SYSTEM
    )
    val labels = mapOf<NotificationCategory?, String>(
        null to "Todas",
        NotificationCategory.MUSIC to "Música",
        NotificationCategory.GAMES to "Juegos",
        NotificationCategory.SOCIAL to "Social",
        NotificationCategory.DOWNLOADS to "Descargas",
        NotificationCategory.SYSTEM to "Sistema"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = selected == option
            KivoFilterChip(
                label = labels.getValue(option),
                selected = isSelected,
                onClick = { onSelect(if (isSelected) null else option) }
            )
        }
    }
}

@Composable
private fun KivoFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) KivoPurpleMain else KivoSurface2,
        label = "chipColor"
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else KivoTextSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationList(
    notifications: List<KivoNotification>,
    viewModel: NotificationViewModel,
    onNotificationClick: (KivoNotification) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(KivoBlack),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(notifications, key = { it.id }) { notification ->
            SwipeableNotificationItem(
                notification = notification,
                onNotificationClick = { onNotificationClick(notification) },
                onDelete = { viewModel.delete(notification.id) },
                onToggleRead = {
                    if (notification.isRead) viewModel.markUnread(notification.id)
                    else viewModel.markRead(notification.id)
                },
                modifier = Modifier.animateItem()
            )
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationItem(
    notification: KivoNotification,
    onNotificationClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleRead()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val isDeleting = dismissState.currentValue == SwipeToDismissBoxValue.EndToStart
            val targetColor = if (isDeleting) KivoPink.copy(alpha = 0.85f) else KivoPurpleMain.copy(alpha = 0.85f)
            val background by animateColorAsState(targetValue = targetColor, label = "swipeBg")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(background),
                contentAlignment = if (isDeleting) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Icon(
                    if (isDeleting) Icons.Default.Delete else Icons.Default.Done,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    ) {
        NotificationCard(notification = notification, onClick = onNotificationClick)
    }
}

@Composable
private fun NotificationCard(notification: KivoNotification, onClick: () -> Unit) {
    val (icon, color) = categoryVisual(notification.category)
    val bgColor by animateColorAsState(
        targetValue = if (notification.isRead) KivoSurface1 else KivoSurface2,
        label = "cardBg"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (notification.isRead) KivoBorder.copy(alpha = 0.5f) else KivoBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.Bold,
                        color = if (notification.isRead) KivoTextSecondary else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatRelativeTime(notification.timestamp),
                        fontSize = 11.sp,
                        color = KivoTextDisabled
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = if (notification.isRead) KivoTextDisabled else KivoTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(KivoPurpleElectric)
                )
            }

            if (notification.priority == NotificationPriority.HIGH) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = "Importante",
                    tint = KivoOrange,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun categoryVisual(category: NotificationCategory): Pair<ImageVector, Color> {
    return when (category) {
        NotificationCategory.MUSIC -> Icons.Default.MusicNote to KivoPurpleElectric
        NotificationCategory.GAMES -> Icons.Default.SportsEsports to KivoOrange
        NotificationCategory.SOCIAL -> Icons.Default.People to KivoGreen
        NotificationCategory.DOWNLOADS -> Icons.Default.FileDownload to KivoBlue
        NotificationCategory.SYSTEM -> Icons.Default.Info to KivoTextSecondary
    }
}

@Composable
private fun SkeletonList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(6) {
            val transition = rememberInfiniteTransition(label = "skeleton")
            val alpha by transition.animateFloat(
                initialValue = 0.4f,
                targetValue = 0.9f,
                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                label = "skeletonAlpha"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(KivoSurface2.copy(alpha = alpha)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(KivoSurface3)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(KivoSurface3)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(220.dp)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(KivoSurface3)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = KivoPink, modifier = Modifier.size(56.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Algo salió mal", color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            "No pudimos cargar tus notificaciones.",
            color = KivoTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = KivoPurpleMain)
        ) {
            Text("Reintentar", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyState(onDiscover: () -> Unit, hasNotifications: Boolean) {
    val transition = rememberInfiniteTransition(label = "empty")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "emptyPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (hasNotifications) Icons.Default.NotificationsOff else Icons.Default.NotificationsNone,
            contentDescription = null,
            tint = KivoPurpleMain.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp * pulse)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (hasNotifications) "Sin resultados" else "No hay notificaciones",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            if (hasNotifications) "Prueba con otro filtro para encontrar notificaciones." else "Cuando recibas novedades, aparecerán aquí.",
            color = KivoTextSecondary,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onDiscover,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = KivoPurpleElectric
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, KivoPurpleElectric)
        ) {
            Text("Descubrir música", fontWeight = FontWeight.Bold)
        }
    }
}

fun formatRelativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Ahora"
        diff < 3_600_000 -> "hace ${diff / 60_000} min"
        diff < 86_400_000 -> "hace ${diff / 3_600_000} h"
        diff < 172_800_000 -> "Ayer"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}