package com.example.kivo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kivo.data.models.Message
import com.example.kivo.data.models.User
import com.example.kivo.data.local.ChatSettingsStore
import com.example.kivo.data.remote.ApiClient
import com.example.kivo.data.repositories.AuthRepository
import com.example.kivo.data.repositories.ChatRepository
import com.example.kivo.ui.state.ChatState
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.components.KivoIconButton
import com.example.kivo.ui.viewmodels.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    otherUserId: String,
    onBack: () -> Unit,
    onCall: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val otherUser by viewModel.otherUser.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val chatState by viewModel.chatState.collectAsState()
    val listState = rememberLazyListState()
    val myId = AuthRepository.getCurrentUserId() ?: ""
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var showUserProfile by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var conversationMuted by remember { mutableStateOf(ChatSettingsStore.isConversationMuted(conversationId)) }
    var userBlocked by remember { mutableStateOf(ChatSettingsStore.isUserBlocked(otherUserId)) }

    val displayedMessages = if (showSearch && searchQuery.isNotBlank()) {
        messages.filter { it.type != "image" && it.text.contains(searchQuery, ignoreCase = true) }
    } else messages

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                viewModel.sendImage(conversationId, otherUserId, mime, bytes)
            }
        }
    }

    LaunchedEffect(conversationId) {
        viewModel.initChat(conversationId, otherUserId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.clickable { showUserProfile = true }
) {
                        AsyncImage(
                            model = ApiClient.resolveUrl(otherUser?.photoUrl) ?: "https://www.w3schools.com/howto/img_avatar.png",
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = otherUser?.displayName ?: "Usuario KIVO",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = when {
                                    userBlocked -> "Bloqueado"
                                    chatState is ChatState.Connecting -> "Conectando..."
                                    chatState is ChatState.Error -> "Sin conexión"
                                    else -> if (otherUser?.isOnline == true) "En línea" else "Desconectado"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (otherUser?.isOnline == true && chatState !is ChatState.Error) KivoGreen else KivoTextSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onCall(conversationId, otherUserId, otherUser?.displayName ?: "Usuario", "voice")
                    }) {
                        Icon(Icons.Filled.Call, contentDescription = "Llamada", tint = Color.White)
                    }
                    IconButton(onClick = {
                        onCall(conversationId, otherUserId, otherUser?.displayName ?: "Usuario", "video")
                    }) {
                        Icon(Icons.Filled.Videocam, contentDescription = "Videollamada", tint = Color.White)
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = KivoSurface2
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ver perfil del contacto", color = Color.White) },
                                onClick = { menuOpen = false; showUserProfile = true }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (conversationMuted) "Reactivar notificaciones" else "Silenciar notificaciones",
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    conversationMuted = !conversationMuted
                                    ChatSettingsStore.setConversationMuted(conversationId, conversationMuted)
                                    Toast.makeText(
                                        context,
                                        if (conversationMuted) "Chat silenciado" else "Notificaciones activadas",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Buscar en el chat", color = Color.White) },
                                onClick = { menuOpen = false; showSearch = true; searchQuery = "" }
                            )
                            DropdownMenuItem(
                                text = { Text("Vaciar chat", color = Color.White) },
                                onClick = { menuOpen = false; showClearDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar chat", color = Color.White) },
                                onClick = { menuOpen = false; showDeleteDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text(if (userBlocked) "Desbloquear" else "Bloquear", color = Color.White) },
                                onClick = { menuOpen = false; showBlockDialog = true }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KivoSurface2,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (userBlocked) {
                Surface(color = KivoSurface2, tonalElevation = 8.dp) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bloqueaste a ${otherUser?.displayName ?: "este usuario"}",
                            color = KivoTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                ChatInputBar(
                    text = inputText,
                    onTextChange = { viewModel.onInputTextChange(it) },
                    onSend = { viewModel.sendMessage(conversationId, otherUserId) },
                    onAttach = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onAudioRecord = {
                        viewModel.toggleAudioRecording(conversationId, otherUserId)
                    },
                    isRecording = viewModel.isRecording.collectAsState().value,
                    recordingAmplitude = viewModel.recordingAmplitude.collectAsState().value,
                    isSending = chatState is ChatState.SendingMessage
                )
            }
        },
        containerColor = KivoBlack
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (chatState is ChatState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = KivoPurpleMain)
            } else if (showSearch && searchQuery.isNotBlank() && displayedMessages.isEmpty()) {
                Text(
                    text = "Sin resultados",
                    color = KivoTextSecondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (chatState is ChatState.Empty && searchQuery.isBlank()) {
                Text(
                    text = "Dile hola a ${otherUser?.displayName ?: "tu amigo"} 👋",
                    color = KivoTextSecondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(modifier = Modifier.fillMaxSize()) {
                if (showSearch) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Buscar mensajes", color = KivoTextSecondary) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = KivoPurpleMain)
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = KivoSurface2,
                                unfocusedContainerColor = KivoSurface2,
                                focusedIndicatorColor = KivoPurpleMain,
                                unfocusedIndicatorColor = KivoSurface3,
                                cursorColor = KivoPurpleMain,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showSearch = false; searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar búsqueda", tint = Color.White)
                        }
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedMessages) { message ->
                        MessageBubble(
                            message = message,
                            isMine = message.senderId == myId,
                            onImageClick = { url -> previewUrl = url }
                        )
                    }
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Vaciar chat") },
                text = {
                    Text("Se borrarán todos los mensajes de esta conversación. Esta acción no se puede deshacer.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showClearDialog = false
                        viewModel.clearMessages(conversationId) { ok ->
                            Toast.makeText(
                                context,
                                if (ok) "Chat vaciado" else "No se pudo vaciar el chat",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Text("Vaciar", color = KivoPurpleElectric, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancelar", color = KivoTextSecondary)
                    }
                },
                containerColor = KivoSurface2,
                titleContentColor = Color.White,
                textContentColor = KivoTextSecondary
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Eliminar chat") },
                text = { Text("La conversación se eliminará de tu lista de chats.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteDialog = false
                        viewModel.deleteConversation(conversationId) { ok ->
                            Toast.makeText(
                                context,
                                if (ok) "Chat eliminado" else "No se pudo eliminar el chat",
                                Toast.LENGTH_SHORT
                            ).show()
                            if (ok) onBack()
                        }
                    }) {
                        Text("Eliminar", color = KivoPink, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancelar", color = KivoTextSecondary)
                    }
                },
                containerColor = KivoSurface2,
                titleContentColor = Color.White,
                textContentColor = KivoTextSecondary
            )
        }

        if (showBlockDialog) {
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                title = { Text(if (userBlocked) "Desbloquear" else "Bloquear") },
                text = {
                    Text(
                        if (userBlocked) {
                            "¿Dejar de bloquear a ${otherUser?.displayName ?: "este usuario"}?"
                        } else {
                            "¿Bloquear a ${otherUser?.displayName ?: "este usuario"}? No recibirás mensajes ni notificaciones de esta persona."
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showBlockDialog = false
                        userBlocked = !userBlocked
                        ChatSettingsStore.setUserBlocked(otherUserId, userBlocked)
                        Toast.makeText(
                            context,
                            if (userBlocked) "Usuario bloqueado" else "Usuario desbloqueado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text(if (userBlocked) "Desbloquear" else "Bloquear", color = KivoPink, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBlockDialog = false }) {
                        Text("Cancelar", color = KivoTextSecondary)
                    }
                },
                containerColor = KivoSurface2,
                titleContentColor = Color.White,
                textContentColor = KivoTextSecondary
            )
        }

        if (showUserProfile) {
            ChatUserProfileSheet(
                user = otherUser,
                images = messages.filter { it.type == "image" }.distinctBy { it.text },
                links = extractLinks(messages),
                onClose = { showUserProfile = false },
                onImageClick = { url -> previewUrl = url }
            )
        }

        previewUrl?.let { url ->
            ImagePreviewOverlay(
                url = url,
                onClose = { previewUrl = null },
                onSave = {
                    scope.launch {
                        val saved = try {
                            val bytes = ChatRepository.downloadImageBytes(url)
                            ChatRepository.saveImageToGallery(context, bytes, url)
                        } catch (e: Exception) {
                            false
                        }
                        Toast.makeText(
                            context,
                            if (saved) "Imagen guardada en Galería" else "No se pudo guardar la imagen",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }
}

@Composable
fun ChatUserProfileSheet(
    user: User?,
    images: List<Message>,
    links: List<String>,
    onClose: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(KivoSurface2)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(KivoTextDisabled.copy(alpha = 0.5f))
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ApiClient.resolveUrl(user?.photoUrl) ?: "https://www.w3schools.com/howto/img_avatar.png",
                    contentDescription = null,
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(KivoSurface3),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = user?.displayName ?: "Usuario KIVO",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${user?.username ?: "usuario"}",
                        color = KivoTextSecondary,
                        fontSize = 13.sp
                    )
                    if (!user?.bio.isNullOrBlank()) {
                        Text(
                            text = user?.bio.orEmpty(),
                            color = KivoTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                KivoIconButton(icon = Icons.Filled.Close, onClick = onClose)
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(KivoSurface3))

            Text(
                text = "Multimedia, enlaces y docs",
                modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${images.size} archivo${if (images.size == 1) "" else "s"}",
                modifier = Modifier.padding(start = 16.dp, top = 2.dp, end = 16.dp),
                color = KivoTextSecondary,
                fontSize = 12.sp
            )

            if (images.isEmpty()) {
                Text(
                    text = "Aún no hay multimedia compartida",
                    modifier = Modifier.padding(16.dp),
                    color = KivoTextSecondary,
                    fontSize = 13.sp
                )
            } else {
                images.chunked(3).forEachIndexed { idx, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = if (idx == 0) 12.dp else 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { img ->
                            AsyncImage(
                                model = ApiClient.resolveUrl(img.text),
                                contentDescription = "Foto compartida",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick(img.text) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            if (links.isNotEmpty()) {
                Text(
                    text = "Enlaces",
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Column(modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 16.dp)) {
                    links.forEach { link ->
                        Text(
                            text = link,
                            color = KivoPurpleElectric,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .clickable {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}

private fun extractLinks(messages: List<Message>): List<String> {
    val regex = Regex("https?://[^\\s]+")
    return messages
        .mapNotNull { regex.find(it.text)?.value }
        .map { it.trimEnd('.', ',', ';', ')', ']', '}') }
        .distinct()
}

@Composable
fun MessageBubble(
    message: Message,
    isMine: Boolean,
    onImageClick: ((String) -> Unit)? = null
) {
    val bubbleColor = if (isMine) KivoPurpleMain else KivoSurface2
    val alignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isMine) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = bubbleColor,
                shape = shape,
                shadowElevation = 2.dp
            ) {
                if (message.type == "image") {
                    AsyncImage(
                        model = ApiClient.resolveUrl(message.text),
                        contentDescription = "Foto",
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .heightIn(max = 280.dp)
                            .clip(shape)
                            .aspectRatio(1.2f, matchHeightConstraintsFirst = true)
                            .clickable { onImageClick?.invoke(message.text) },
                        contentScale = ContentScale.Crop
                    )
                } else if (message.type == "audio") {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .widthIn(min = 160.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = null,
                            tint = KivoPurpleElectric,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Mensaje de voz",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Toca para reproducir",
                                color = KivoTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = message.text,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 15.sp
                    )
                }
            }
            Text(
                text = buildTimeAndStatus(message, isMine),
                color = KivoTextSecondary,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit = {},
    onAudioRecord: () -> Unit = {},
    isRecording: Boolean = false,
    recordingAmplitude: Float = 0f,
    isSending: Boolean = false
) {
    Surface(
        color = KivoSurface2,
        tonalElevation = 8.dp,
        modifier = Modifier.imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onAttach,
                enabled = !isSending && !isRecording
            ) {
                Icon(
                    Icons.Filled.AddPhotoAlternate,
                    contentDescription = "Enviar foto",
                    tint = KivoPurpleMain
                )
            }

            if (isRecording) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(KivoSurface3)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = null,
                        tint = KivoPink,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grabando...",
                        color = KivoPink,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(KivoPink.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = recordingAmplitude)
                                .background(KivoPink)
                        )
                    }
                }
            } else {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    placeholder = { Text("Escribe un mensaje...", color = KivoTextSecondary) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = KivoSurface3,
                        unfocusedContainerColor = KivoSurface3,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = KivoPurpleMain,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 4,
                    enabled = !isSending
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            if (isRecording) {
                IconButton(
                    onClick = onAudioRecord,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(KivoPink)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar audio",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onAudioRecord,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (text.isNotBlank()) KivoPurpleMain else KivoSurface3),
                    enabled = !isSending
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Grabar audio",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun buildTimeAndStatus(message: Message, isMine: Boolean): AnnotatedString {
    return buildAnnotatedString {
        append(formatTime(message.createdAt))
        if (isMine) {
            append(" ")
            when (message.status) {
                "read" -> withStyle(SpanStyle(color = KivoPurpleElectric)) { append("✓✓") }
                "delivered" -> withStyle(SpanStyle(color = KivoTextSecondary)) { append("✓✓") }
                else -> withStyle(SpanStyle(color = KivoTextSecondary)) { append("✓") }
            }
        }
    }
}

@Composable
fun ImagePreviewOverlay(
    url: String,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ApiClient.resolveUrl(url),
            contentDescription = "Imagen completa",
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentScale = ContentScale.Fit
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onSave,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(KivoSurface2)
            ) {
                Icon(Icons.Filled.Download, contentDescription = "Guardar", tint = Color.White)
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(KivoSurface2)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}
