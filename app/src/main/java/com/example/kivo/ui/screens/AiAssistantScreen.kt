package com.example.kivo.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kivo.data.models.Song
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.KivoAiMessage
import com.example.kivo.ui.viewmodels.KivoAiViewModel
import com.example.kivo.ui.viewmodels.MusicViewModel
import com.example.kivo.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    musicViewModel: MusicViewModel,
    aiViewModel: KivoAiViewModel = viewModel(),
    onBack: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val messages = aiViewModel.messages
    val isProcessing by aiViewModel.isProcessing
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KivoBlack
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "KIVO AI", 
                            fontWeight = FontWeight.Black, 
                            letterSpacing = 1.sp,
                            color = KivoPurpleElectric
                        ) 
                    },
                    navigationIcon = {
                        KivoIconButton(icon = Icons.Default.Close, onClick = onBack)
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = KivoBlack,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KivoBlack)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    if (messages.size <= 2 && !isProcessing) {
                        QuickActionsPro(onActionClick = { aiViewModel.sendQuickResponse(it, musicViewModel) })
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = KivoSurface2,
                        border = androidx.compose.foundation.BorderStroke(1.dp, KivoBorder.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = text,
                                onValueChange = { text = it },
                                placeholder = { Text("Pregúntale a Kivo AI...", color = KivoTextDisabled, fontSize = 14.sp) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    if (text.isNotBlank() && !isProcessing) {
                                        aiViewModel.sendMessage(text, musicViewModel)
                                        text = ""
                                    }
                                }),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = KivoPurpleMain
                                )
                            )
                            
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = KivoPurpleElectric
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        if (text.isNotBlank()) {
                                            aiViewModel.sendMessage(text, musicViewModel)
                                            text = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Brush.linearGradient(GradientAction), CircleShape)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send, 
                                        contentDescription = "Enviar", 
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = KivoBlack
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                items(messages) { message ->
                    ChatBubblePro(message, onPlayClick = { it?.let { musicViewModel.playSong(it) } })
                }
                
                if (isProcessing) {
                    item {
                        Text(
                            text = "Kivo está pensando...",
                            style = MaterialTheme.typography.labelMedium,
                            color = KivoTextDisabled,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun QuickActionsPro(onActionClick: (String) -> Unit) {
    val actions = listOf(
        "🎵 Recomiéndame algo",
        "😔 Música triste",
        "🔥 Para entrenar",
        "🎧 Sorpréndeme"
    )
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        actions.forEach { action ->
            item {
                Surface(
                    onClick = { onActionClick(action) },
                    shape = RoundedCornerShape(12.dp),
                    color = KivoSurface2,
                    border = androidx.compose.foundation.BorderStroke(1.dp, KivoBorder.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = action,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubblePro(message: KivoAiMessage, onPlayClick: (Song?) -> Unit) {
    val isUser = message.isUser
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) KivoPurpleDeep else KivoSurface1,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, KivoBorder.copy(alpha = 0.3f)) else null
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = Color.White
                )
                
                if (message.song != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AiSongCardPro(song = message.song, onPlayClick = { onPlayClick(message.song) })
                }
            }
        }
        
        Text(
            text = if (isUser) "Tú" else "KIVO AI",
            style = MaterialTheme.typography.labelSmall,
            color = if (isUser) KivoPurpleElectric.copy(alpha = 0.7f) else KivoTextDisabled,
            modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AiSongCardPro(song: Song, onPlayClick: () -> Unit) {
    KivoCard(
        backgroundColor = Color.Black.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(
                    song.title, 
                    style = MaterialTheme.typography.labelLarge, 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Text(
                    song.artist, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = KivoTextSecondary, 
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }
            
            Surface(
                onClick = onPlayClick,
                modifier = Modifier.size(32.dp),
                color = KivoPurpleMain,
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
