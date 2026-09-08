package com.example.kivo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.kivo.data.models.User
import com.example.kivo.data.remote.ApiClient
import com.example.kivo.data.repositories.AuthRepository
import com.example.kivo.ui.state.*
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.UserSearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    onUserClick: (String) -> Unit,
    onBack: () -> Unit,
    navController: NavController,
    viewModel: UserSearchViewModel = viewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchState by viewModel.searchState.collectAsState()
    val scope = rememberCoroutineScope()
    val myId = AuthRepository.getCurrentUserId() ?: "temp_my_id"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = KivoBlack
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Buscar Personas",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("@username", color = KivoTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KivoPurpleMain) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KivoPurpleMain,
                    unfocusedBorderColor = KivoBorder,
                    focusedContainerColor = KivoSurface2,
                    unfocusedContainerColor = KivoSurface2,
                    cursorColor = KivoPurpleMain,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            when (searchState) {
                is UserSearchState.Searching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KivoPurpleMain)
                    }
                }
                is UserSearchState.NoResults -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No encontramos a este usuario.", color = KivoTextSecondary)
                    }
                }
                is UserSearchState.InvalidQuery -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Escribe al menos 3 caracteres", color = KivoTextSecondary)
                    }
                }
                is UserSearchState.Results -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items((searchState as UserSearchState.Results).users) { user ->
                            UserSearchItem(
                                user = user,
                                onClick = {
                                    scope.launch {
                                        val conversationId = viewModel.getOrCreateConversation(myId, user.userId)
                                        navController.navigate("chat_detail/$conversationId/${user.userId}")
                                    }
                                }
                            )
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Escribe un @username para buscar", color = KivoTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun UserSearchItem(
    user: User,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KivoSurface2)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ApiClient.resolveUrl(user.photoUrl) ?: "https://www.w3schools.com/howto/img_avatar.png",
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(KivoSurface3),
            contentScale = ContentScale.Crop
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = user.displayName.ifEmpty { "Usuario KIVO" },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "@${user.username}",
                color = KivoTextSecondary,
                fontSize = 14.sp
            )
        }
        
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = KivoPurpleMain),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("Mensaje", fontSize = 12.sp)
        }
    }
}
