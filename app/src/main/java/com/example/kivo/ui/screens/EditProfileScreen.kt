package com.example.kivo.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.kivo.data.remote.ApiClient
import com.example.kivo.data.repositories.ChatRepository
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.ProfileViewModel
import com.example.kivo.ui.components.KivoIconButton
import kotlinx.coroutines.launch
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(viewModel: ProfileViewModel, onBack: () -> Unit) {
    val name by viewModel.name.collectAsState()
    val username by viewModel.username.collectAsState()
    val description by viewModel.description.collectAsState()
    val pronouns by viewModel.pronouns.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val following by viewModel.following.collectAsState()
    val followers by viewModel.followers.collectAsState()
    val likes by viewModel.likes.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingField by remember { mutableStateOf("") }
    var tempValue by remember { mutableStateOf("") }

    // Launcher para seleccionar imagen de la galería
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Vista previa inmediata (copia local)
            viewModel.updateProfileImage(uri.toString())

            // Subida al servidor para que la foto persista
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null) {
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                scope.launch {
                    val url = runCatching { ChatRepository.uploadImage(mime, bytes) }.getOrNull()
                    if (url != null) {
                        viewModel.updateProfilePhotoUrl(url)
                        Toast.makeText(context, "Foto de perfil guardada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No se pudo subir la foto", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar $editingField", color = Color.White) },
            text = {
                TextField(
                    value = tempValue,
                    onValueChange = { tempValue = it },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = KivoSurface2,
                        unfocusedContainerColor = KivoSurface2
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    when (editingField) {
                        "Nombre" -> viewModel.updateName(tempValue)
                        "Usuario" -> viewModel.updateUsername(tempValue)
                        "Descripción" -> viewModel.updateDescription(tempValue)
                        "Pronombre" -> viewModel.updatePronouns(tempValue)
                        "Siguiendo" -> viewModel.updateFollowing(tempValue)
                        "Seguidores" -> viewModel.updateFollowers(tempValue)
                        "Me gusta" -> viewModel.updateLikes(tempValue)
                    }
                    showEditDialog = false
                }) {
                    Text("Guardar", color = KivoPurpleElectric)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancelar", color = KivoTextDisabled)
                }
            },
            containerColor = KivoSurface1
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Editar perfil", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    KivoIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = KivoBlack,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = KivoBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Profile Image Section with PRO Styling
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.clickable { 
                    launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(KivoSurface3)
                        .border(2.dp, Brush.linearGradient(GradientAction), CircleShape)
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = ApiClient.resolveUrl(profileImageUri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center).size(32.dp),
                            tint = KivoTextDisabled
                        )
                    }
                }
                
                // Overlay Camera Icon
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = CircleShape,
                    modifier = Modifier.size(110.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                    }
                }
            }
            
            TextButton(onClick = { 
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Text("Cambiar foto", color = KivoPurpleElectric, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Info Section 1
            Surface(
                color = KivoSurface1,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column {
                    EditItem(label = "Nombre", value = name) {
                        editingField = "Nombre"
                        tempValue = name
                        showEditDialog = true
                    }
                    EditItem(label = "Nombre de usuario", value = username) {
                        editingField = "Usuario"
                        tempValue = username
                        showEditDialog = true
                    }
                    EditItem(label = "", value = "kivo.com/@$username", showCopy = true) {
                        // Copy logic
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Información básica",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                color = KivoTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Info Section 2
            Surface(
                color = KivoSurface1,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column {
                    EditItem(
                        label = "Descripción", 
                        value = if (description.isEmpty()) "Escribe una descripción" else description, 
                        isMultiLine = true
                    ) {
                        editingField = "Descripción"
                        tempValue = description
                        showEditDialog = true
                    }
                    EditItem(label = "Pronombre", value = if (pronouns.isEmpty()) "Agregar pronombres" else pronouns) {
                        editingField = "Pronombre"
                        tempValue = pronouns
                        showEditDialog = true
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Estadísticas sociales",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                color = KivoTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Info Section 3 (Social stats)
            Surface(
                color = KivoSurface1,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column {
                    EditItem(label = "Siguiendo", value = following) {
                        editingField = "Siguiendo"
                        tempValue = following
                        showEditDialog = true
                    }
                    EditItem(label = "Seguidores", value = followers) {
                        editingField = "Seguidores"
                        tempValue = followers
                        showEditDialog = true
                    }
                    EditItem(label = "Me gusta", value = likes) {
                        editingField = "Me gusta"
                        tempValue = likes
                        showEditDialog = true
                    }
                }
            }
        }
    }
}

@Composable
fun EditItem(
    label: String,
    value: String,
    showCopy: Boolean = false,
    isMultiLine: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = if (isMultiLine) Alignment.Top else Alignment.CenterVertically
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                modifier = Modifier.width(110.dp),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            Spacer(modifier = Modifier.width(110.dp))
        }

        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = if (value.contains("Escribe") || value.contains("Agregar")) KivoTextDisabled else Color.White,
            fontSize = 15.sp,
            maxLines = if (isMultiLine) 3 else 1,
            overflow = TextOverflow.Ellipsis
        )

        if (showCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = KivoTextDisabled, modifier = Modifier.size(18.dp))
        } else {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = KivoTextDisabled.copy(alpha = 0.5f))
        }
    }
}
