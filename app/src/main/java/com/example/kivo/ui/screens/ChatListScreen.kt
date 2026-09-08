package com.example.kivo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kivo.data.models.Conversation
import com.example.kivo.data.models.User
import com.example.kivo.data.remote.ApiClient
import com.example.kivo.data.repositories.AuthRepository
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.ChatListViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListScreen(
    onChatClick: (String, String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: ChatListViewModel = viewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    val usersInfo by viewModel.usersInfo.collectAsState()
    val myId = AuthRepository.getCurrentUserId() ?: ""

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KivoBlack
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mensajes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(KivoSurface2)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White)
                }
            }
            
            if (conversations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aún no tienes conversaciones.\n¡Busca a alguien para empezar!",
                        color = KivoTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conversations) { conversation ->
                        val otherUserId = conversation.participants.find { it != myId } ?: ""
                        val otherUser = usersInfo[otherUserId]
                        
                        ConversationItem(
                            conversation = conversation,
                            otherUser = otherUser,
                            onClick = { onChatClick(conversation.conversationId, otherUserId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    otherUser: User?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KivoSurface1)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = ApiClient.resolveUrl(otherUser?.photoUrl) ?: "https://www.w3schools.com/howto/img_avatar.png",
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(KivoSurface3),
                contentScale = ContentScale.Crop
            )
            if (otherUser?.isOnline == true) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(KivoGreen)
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .background(KivoBlack, CircleShape)
                        .padding(2.dp)
                        .background(KivoGreen, CircleShape)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = otherUser?.displayName ?: "Cargando...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatTime(conversation.updatedAt),
                    color = KivoTextSecondary,
                    fontSize = 12.sp
                )
            }
            
            Text(
                text = conversation.lastMessage.ifEmpty { "Inicia una conversación" },
                color = KivoTextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
