package com.example.kivo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.kivo.data.models.kivoSongs
import com.example.kivo.data.remote.ApiClient
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.ProfileViewModel
import com.example.kivo.ui.viewmodels.AuthViewModel
import com.example.kivo.ui.components.*

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    authViewModel: AuthViewModel = viewModel(),
    onEditClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onSavedSongsClick: () -> Unit
) {
    val name by viewModel.name.collectAsState()
    val username by viewModel.username.collectAsState()
    val description by viewModel.description.collectAsState()
    val profileImageUri by viewModel.profileImageUri.collectAsState()
    val following by viewModel.following.collectAsState()
    val followers by viewModel.followers.collectAsState()
    val likes by viewModel.likes.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KivoBlack
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Info
            item(span = { GridItemSpan(maxLineSpan) }) {
                ProfileHeaderPro(
                    name,
                    username,
                    profileImageUri,
                    onEditClick,
                    onSwitchAccountClick = { authViewModel.logout() },
                    onLogoutClick = { authViewModel.logout() },
                    onNotificationSettingsClick = onNotificationSettingsClick
                )
            }

            // Stats Row
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStatItemPro(following, "Siguiendo")
                    ProfileStatItemPro(followers, "Seguidores")
                    ProfileStatItemPro(likes, "Me gusta")
                }
            }

            // XP & Level Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                KivoCard(gradient = listOf(KivoSurface2, KivoPurpleDeep)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("NIVEL 12", color = KivoPurpleElectric, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            Text("EXPLORADOR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("1,250 XP", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            KivoProgressBar(progress = 0.6f, modifier = Modifier.width(100.dp))
                        }
                    }
                }
            }

            // Description
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        color = KivoTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                KivoSectionTitle("CANCIONES GUARDADAS")
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                KivoSavedSongsCard(
                    count = kivoSongs.size,
                    onClick = onSavedSongsClick
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                KivoSectionTitle("MI ACTIVIDAD")
            }

            // Activity Grid
            items(9) { _ ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(KivoSurface2)
                ) {
                    Icon(
                        Icons.Default.GridView, 
                        null, 
                        tint = KivoTextDisabled, 
                        modifier = Modifier.align(Alignment.Center).size(24.dp)
                    )
                }
            }
            
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun ProfileHeaderPro(
    name: String,
    username: String,
    imageUri: String?,
    onEditClick: () -> Unit,
    onSwitchAccountClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            // Avatar with PRO border
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(KivoSurface3)
                    .border(2.dp, Brush.linearGradient(GradientAction), CircleShape)
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ApiClient.resolveUrl(imageUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person, 
                        null, 
                        tint = KivoTextDisabled, 
                        modifier = Modifier.fillMaxSize().padding(20.dp)
                    )
                }
            }
            
            // PRO Settings Button (Pencil)
            Surface(
                onClick = { menuExpanded = true },
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.BottomEnd)
                    .shadow(8.dp, CircleShape, spotColor = KivoPurpleMain),
                color = Color.Transparent,
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(GradientAction))
                        .border(2.dp, KivoBlack, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit, 
                        contentDescription = "Ajustes", 
                        tint = Color.White, 
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Ajustes del perfil
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = KivoSurface2
            ) {
                DropdownMenuItem(
                    text = { Text("Editar perfil", color = Color.White, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = KivoPurpleElectric) },
                    onClick = {
                        menuExpanded = false
                        onEditClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Cambiar cuenta", color = Color.White, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.SwitchAccount, null, tint = KivoPurpleElectric) },
                    onClick = {
                        menuExpanded = false
                        onSwitchAccountClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Configuración de notificaciones", color = Color.White, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Notifications, null, tint = KivoPurpleElectric) },
                    onClick = {
                        menuExpanded = false
                        onNotificationSettingsClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Cerrar sesión", color = Color.White, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = KivoPink) },
                    onClick = {
                        menuExpanded = false
                        onLogoutClick()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "@$username",
                style = MaterialTheme.typography.bodyLarge,
                color = KivoPurpleElectric,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProfileStatItemPro(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = KivoTextSecondary,
            letterSpacing = 1.sp
        )
    }
}
