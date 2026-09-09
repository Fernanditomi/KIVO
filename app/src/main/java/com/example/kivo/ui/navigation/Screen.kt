package com.example.kivo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Call

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Chat : Screen("chat", "Chat", Icons.AutoMirrored.Filled.Chat)
    object Games : Screen("games", "Games", Icons.Default.SportsEsports)
    object Music : Screen("music", "Música", Icons.Default.MusicNote)
    object Profile : Screen("profile", "Perfil", Icons.Default.Person)
    object Player : Screen("player", "Reproductor", Icons.Default.MusicNote)
    object Shorts : Screen("shorts", "Shorts", Icons.Default.Bolt)
    object AI : Screen("ai", "Kivo AI", Icons.Default.SmartToy)
    object Ambient : Screen("ambient", "Modo Ambiente", Icons.Default.MusicNote)
    object Equalizer : Screen("equalizer", "Ecualizador", Icons.Default.MusicNote)
    object Trivia : Screen("trivia", "Adivina la canción", Icons.Default.MusicNote)
    object EditProfile : Screen("edit_profile", "Editar perfil", Icons.Default.Edit)
    object TapChallenge : Screen("tap_challenge", "Tap Challenge", Icons.Default.TouchApp)
    object ViralTrivia : Screen("viral_trivia", "Trivia Viral", Icons.Default.Lightbulb)
    object WhoSaidThat : Screen("who_said_that", "¿Quién dijo eso?", Icons.Default.QuestionAnswer)
    object QueCancionSalvas : Screen("que_cancion_salvas", "¿Qué canción salvas?", Icons.Default.CompareArrows)
    object ChatDetail : Screen("chat_detail/{conversationId}/{otherUserId}", "Chat", Icons.AutoMirrored.Filled.Chat)
    object Call : Screen("call/{conversationId}/{otherUserId}/{otherUserName}/{callType}", "Llamada", Icons.Filled.Call)
    object UserSearch : Screen("user_search", "Buscar Personas", Icons.Default.Search)
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Register : Screen("register", "Registro", Icons.Default.Person)
    object Notifications : Screen("notifications", "Notificaciones", Icons.Default.Notifications)
    object NotificationSettings : Screen("notification_settings", "Configuración de notificaciones", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Chat,
    Screen.Games,
    Screen.Music,
    Screen.Profile
)
