package com.example.kivo.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.kivo.ui.navigation.Screen
import com.example.kivo.ui.navigation.bottomNavItems
import com.example.kivo.ui.screens.*
import com.example.kivo.ui.theme.*
import com.example.kivo.ui.viewmodels.MusicViewModel
import com.example.kivo.ui.viewmodels.ProfileViewModel
import com.example.kivo.ui.viewmodels.QueCancionSalvasViewModel
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.kivo.data.local.ChatSettingsStore
import com.example.kivo.data.models.kivoSongs
import com.example.kivo.data.notifications.KivoNotification
import com.example.kivo.data.notifications.NotificationCategory
import com.example.kivo.data.notifications.NotificationDestination
import com.example.kivo.data.notifications.NotificationCenter
import com.example.kivo.data.notifications.NotificationStore
import com.example.kivo.data.repositories.ChatRepository
import com.example.kivo.data.repositories.AuthRepository
import com.example.kivo.data.remote.MessageNotifier
import com.example.kivo.data.remote.SocketManager
import com.example.kivo.ui.components.KivoBadgedIconButton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.kivo.ui.viewmodels.AuthViewModel
import com.example.kivo.ui.state.AuthState
import com.example.kivo.ui.components.KivoIconButton
import androidx.media3.common.Player
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

@Composable
fun MainScreen(
    musicViewModel: MusicViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    queCancionSalvasViewModel: QueCancionSalvasViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()

    when (authState) {
        is AuthState.CheckingSession -> {
            Box(modifier = Modifier.fillMaxSize().background(KivoBlack), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KivoPurpleMain)
            }
        }
        is AuthState.Unauthenticated -> {
            AuthScreen()
        }
        is AuthState.Authenticated -> {
            MainAppContent(
                musicViewModel,
                profileViewModel,
                queCancionSalvasViewModel,
                authViewModel,
                navController,
                (authState as AuthState.Authenticated).user
            )
        }
        is AuthState.Error -> {
            val isOnline by com.example.kivo.data.remote.NetworkMonitor.online.collectAsState()
            var retryCount by remember { mutableIntStateOf(0) }
            LaunchedEffect(authState, isOnline) {
                if (isOnline && retryCount < 5) {
                    delay(5000L * (retryCount + 1))
                    retryCount++
                    authViewModel.checkSession()
                }
            }
            Box(
                modifier = Modifier.fillMaxSize().background(KivoBlack),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.CloudOff else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = KivoPink,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isOnline) {
                            "No pudimos conectar con el servidor de KIVO.\nEstamos reintentando solos..."
                        } else {
                            "No tienes conexion a internet.\nConectate y reintentaremos solos."
                        },
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { authViewModel.checkSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = KivoPurpleMain)
                    ) {
                        Text("Reintentar ahora", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun MainAppContent(
    musicViewModel: MusicViewModel,
    profileViewModel: ProfileViewModel,
    queCancionSalvasViewModel: QueCancionSalvasViewModel,
    authViewModel: AuthViewModel,
    navController: androidx.navigation.NavHostController,
    currentUser: com.example.kivo.data.models.User
) {
    val currentSong by musicViewModel.currentSong.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val progress by musicViewModel.progress.collectAsState()
    val playbackState by musicViewModel.playbackState.collectAsState()
    val favoriteIds by musicViewModel.favoriteSongIds.collectAsState()
    val isMiniPlayerVisible by musicViewModel.isMiniPlayerVisible.collectAsState()
    val isShuffleEnabled by musicViewModel.isShuffleEnabled.collectAsState()
    val isOnline by com.example.kivo.data.remote.NetworkMonitor.online.collectAsState()
    val unreadNotificationCount by NotificationStore.notifications
        .map { list -> list.count { !it.isRead } }
        .collectAsState(initial = 0)

    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val onMiniPlayerClick: () -> Unit = {
        navController.navigate(Screen.Player.route)
    }

    // Navegación resultante de tocar una notificación (centro o push)
    val handleNotificationTap: (KivoNotification) -> Unit = { notification ->
        val toTab: (String) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        when (notification.destination) {
            NotificationDestination.PLAYER -> {
                val song = kivoSongs.find { it.id == notification.destinationId }
                if (song != null) {
                    musicViewModel.playSong(song)
                    navController.navigate(Screen.Player.route)
                } else {
                    toTab(Screen.Music.route)
                }
            }
            NotificationDestination.CHAT -> {
                val parts = notification.destinationId.split("|")
                if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
                    navController.navigate("chat_detail/${parts[0]}/${parts[1]}")
                } else {
                    toTab(Screen.Chat.route)
                }
            }
            NotificationDestination.GAME -> navController.navigate(Screen.TapChallenge.route)
            NotificationDestination.GAMES -> toTab(Screen.Games.route)
            NotificationDestination.MUSIC,
            NotificationDestination.PLAYLIST -> toTab(Screen.Music.route)
            NotificationDestination.PROFILE -> toTab(Screen.Profile.route)
            NotificationDestination.NOTIFICATIONS -> {
                navController.navigate(Screen.Notifications.route) { launchSingleTop = true }
            }
        }
    }
    
    // Handle notification intent
    val activity = LocalActivity.current
    // Deep links de notificaciones: se guardan como "pendientes" y se procesan en un
    // LaunchedEffect para evitar navegar antes de que NavHost tenga el grafo configurado.
    var pendingChatDestination by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingNotificationTap by remember { mutableStateOf<KivoNotification?>(null) }

    DisposableEffect(activity, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val convId = activity?.intent?.getStringExtra("conversationId")
                val otherId = activity?.intent?.getStringExtra("otherUserId")
                if (convId != null && otherId != null) {
                    pendingChatDestination = convId to otherId
                    activity?.intent?.removeExtra("conversationId")
                    activity?.intent?.removeExtra("otherUserId")
                }

                val deepDestination = activity?.intent?.getStringExtra("kivo_destination")
                if (deepDestination != null) {
                    val deepDestinationId = activity?.intent?.getStringExtra("kivo_destination_id") ?: ""
                    val destination = runCatching {
                        NotificationDestination.valueOf(deepDestination)
                    }.getOrDefault(NotificationDestination.NOTIFICATIONS)
                    pendingNotificationTap = KivoNotification(
                        id = "deep_$deepDestination",
                        category = NotificationCategory.SYSTEM,
                        type = "deep_link",
                        title = "",
                        message = "",
                        timestamp = 0L,
                        isRead = true,
                        destination = destination,
                        destinationId = deepDestinationId
                    )
                    activity?.intent?.removeExtra("kivo_destination")
                    activity?.intent?.removeExtra("kivo_destination_id")
                    activity?.intent?.removeExtra("kivo_category")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Procesa los deep links una vez que el NavGraph está listo.
    LaunchedEffect(pendingChatDestination, pendingNotificationTap) {
        pendingChatDestination?.let { (convId, otherId) ->
            pendingChatDestination = null
            navController.navigate("chat_detail/$convId/$otherId")
        }
        pendingNotificationTap?.let { notification ->
            pendingNotificationTap = null
            handleNotificationTap(notification)
        }
    }

    // Conversación abierta actualmente (para no notificar al estar dentro del chat)
    val context = LocalContext.current
    var openConversationId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(navController) {
        snapshotFlow {
            val entry = navController.currentBackStackEntry
            if (entry?.destination?.route == Screen.ChatDetail.route) {
                entry.arguments?.getString("conversationId")
            } else null
        }.collect { openConversationId = it }
    }

    // Notificación cuando llega un mensaje y no estás viendo ese chat (sin silenciados ni bloqueados)
    LaunchedEffect(Unit) {
        SocketManager.newMessages.collect { msg ->
            val convId = msg.conversationId
            if (convId != null
                && convId != openConversationId
                && msg.senderId != currentUser.userId
                && !ChatSettingsStore.isConversationMuted(convId)
                && !ChatSettingsStore.isUserBlocked(msg.senderId)
            ) {
                MessageNotifier.show(context, msg)
                NotificationCenter.chatMessage(msg)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val userId = currentUser.userId
            if (event == Lifecycle.Event.ON_START) {
                scope.launch {
                    ChatRepository.setUserPresence(userId, true)
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                scope.launch {
                    ChatRepository.setUserPresence(userId, false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showTopBar = currentDestination?.route in listOf(
                Screen.Home.route, 
                Screen.Chat.route, 
                Screen.Games.route, 
                Screen.Music.route, 
                Screen.Profile.route
            )
            
            if (showTopBar) {
                KivoTopHeader(
                    onAiClick = { navController.navigate(Screen.AI.route) },
                    onNotificationsClick = {
                        navController.navigate(Screen.Notifications.route) { launchSingleTop = true }
                    },
                    unreadNotifications = unreadNotificationCount,
                    onProfileClick = { 
                        if (currentDestination?.route != Screen.Profile.route) {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    offline = !isOnline
                )
            }
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Screens that should NOT show the bottom bar
            val fullScreenRoutes = listOf(
                Screen.Player.route,
                Screen.Shorts.route,
                Screen.AI.route,
                Screen.Ambient.route,
                Screen.Equalizer.route,
                Screen.Trivia.route,
                Screen.TapChallenge.route,
                Screen.ViralTrivia.route,
                Screen.WhoSaidThat.route,
                Screen.QueCancionSalvas.route,
                Screen.EditProfile.route,
                Screen.ChatDetail.route,
                Screen.Call.route,
                Screen.UserSearch.route,
                Screen.Notifications.route,
                Screen.NotificationSettings.route
            )
            
            val isFullScreen = currentDestination?.route in fullScreenRoutes

            if (!isLandscape && !isFullScreen) {
                Column {
                    if (isMiniPlayerVisible && currentSong != null && playbackState != Player.STATE_IDLE) {
                        KivoMiniPlayerProCompact(
                            currentSong = currentSong!!,
                            isPlaying = isPlaying,
                            progress = progress,
                            isLoading = playbackState == Player.STATE_BUFFERING,
                            isFavorite = favoriteIds.contains(currentSong!!.id),
                            isShuffleEnabled = isShuffleEnabled,
                            onPlayPauseClick = { musicViewModel.togglePlayPause() },
                            onNextClick = { musicViewModel.nextSong() },
                            onPreviousClick = { musicViewModel.previousSong() },
                            onFavoriteClick = { musicViewModel.toggleFavorite(currentSong!!.id) },
                            onShuffleClick = { musicViewModel.toggleShuffle() },
                            onDismiss = { musicViewModel.dismissMiniPlayer() },
                            onClick = onMiniPlayerClick
                        )
                    }
                    KivoBottomNavigation(navController, currentDestination)
                }
            }
        },
        containerColor = KivoBlack
    ) { innerPadding ->
        val entry by navController.currentBackStackEntryAsState()
        val destination = entry?.destination
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
            ) {
            if (isLandscape) {
                KivoBottomNavigationRail(navController, destination)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
            composable(Screen.Home.route) { 
                HomeScreen(
                    onShortsClick = { navController.navigate(Screen.Shorts.route) },
                    onAiClick = { navController.navigate(Screen.AI.route) }
                ) 
            }
            composable(Screen.Chat.route) { 
                ChatListScreen(
                    onChatClick = { conversationId, otherUserId -> 
                        navController.navigate("chat_detail/$conversationId/$otherUserId")
                    },
                    onSearchClick = { navController.navigate(Screen.UserSearch.route) }
                ) 
            }
            composable(Screen.Games.route) { 
                GamesScreen(
                    onNavigateToViralTrivia = { navController.navigate(Screen.ViralTrivia.route) },
                    onNavigateToTrivia = { navController.navigate(Screen.Trivia.route) },
                    onNavigateToTapChallenge = { navController.navigate(Screen.TapChallenge.route) },
                    onNavigateToWhoSaidThat = { navController.navigate(Screen.WhoSaidThat.route) },
                    onNavigateToSaveSong = { navController.navigate(Screen.QueCancionSalvas.route) }
                ) 
            }
            composable(Screen.Music.route) { 
                MusicScreen(
                    viewModel = musicViewModel,
                    onYouTubeClick = { navController.navigate(Screen.YouTubeSearch.route) },
                    onSavedSongsClick = { navController.navigate(Screen.SavedSongs.route) }
                ) 
            }
            composable(Screen.SavedSongs.route) {
                SavedSongsScreen(
                    viewModel = musicViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.YouTubeSearch.route) {
                YouTubeSearchScreen(
                    viewModel = musicViewModel,
                    onPlayVideo = { video -> musicViewModel.playYouTubeResult(video) }
                )
            }
            composable(Screen.Profile.route) { 
                ProfileScreen(
                    viewModel = profileViewModel,
                    onEditClick = { navController.navigate(Screen.EditProfile.route) },
                    onNotificationSettingsClick = { navController.navigate(Screen.NotificationSettings.route) },
                    onSavedSongsClick = { navController.navigate(Screen.SavedSongs.route) }
                ) 
            }
            composable(Screen.Player.route) { 
                MusicPlayerScreen(
                    viewModel = musicViewModel,
                    onAmbientClick = { navController.navigate(Screen.Ambient.route) },
                    onEqualizerClick = { navController.navigate(Screen.Equalizer.route) },
                    onBack = { navController.popBackStack() }
                ) 
            }
            composable(Screen.Shorts.route) {
                ShortsScreen()
            }
            composable(Screen.AI.route) {
                AiAssistantScreen(
                    musicViewModel = musicViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Ambient.route) {
                AmbientModeScreen(
                    viewModel = musicViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Equalizer.route) {
                EqualizerScreen(
                    viewModel = musicViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Trivia.route) {
                TriviaScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.TapChallenge.route) {
                TapChallengeScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.QueCancionSalvas.route) {
                QueCancionSalvasScreen(
                    viewModel = queCancionSalvasViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ViralTrivia.route) {
                ViralTriviaScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.WhoSaidThat.route) {
                WhoSaidThatScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    viewModel = profileViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                Screen.ChatDetail.route,
                arguments = listOf(
                    androidx.navigation.navArgument("conversationId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("otherUserId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
                ChatScreen(
                    conversationId = conversationId,
                    otherUserId = otherUserId,
                    onBack = { navController.popBackStack() },
                    onCall = { convId, userId, userName, type ->
                        navController.navigate("call/$convId/$userId/$userName/$type")
                    }
                )
            }
            composable(
                Screen.Call.route,
                arguments = listOf(
                    androidx.navigation.navArgument("conversationId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("otherUserId") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("otherUserName") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("callType") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
                val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: "Usuario"
                val callType = backStackEntry.arguments?.getString("callType") ?: "voice"
                CallScreen(
                    conversationId = conversationId,
                    otherUserId = otherUserId,
                    otherUserName = otherUserName,
                    callType = callType,
                    onEndCall = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.UserSearch.route) {
                UserSearchScreen(
                    onUserClick = { userId ->
                        // This will be handled in ViewModel to get/create conversation
                    },
                    onBack = { navController.popBackStack() },
                    navController = navController
                )
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onNotificationClick = { notification ->
                        handleNotificationTap(notification)
                    },
                    onDiscoverClick = {
                        navController.navigate(Screen.Music.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.NotificationSettings.route) {
                NotificationSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
                }
                if (isLandscape && isMiniPlayerVisible && currentSong != null && playbackState != Player.STATE_IDLE) {
                    KivoMiniPlayerProCompact(
                        currentSong = currentSong!!,
                        isPlaying = isPlaying,
                        progress = progress,
                        isLoading = playbackState == Player.STATE_BUFFERING,
                        isFavorite = favoriteIds.contains(currentSong!!.id),
                        isShuffleEnabled = isShuffleEnabled,
                        onPlayPauseClick = { musicViewModel.togglePlayPause() },
                        onNextClick = { musicViewModel.nextSong() },
                        onPreviousClick = { musicViewModel.previousSong() },
                        onFavoriteClick = { musicViewModel.toggleFavorite(currentSong!!.id) },
                        onShuffleClick = { musicViewModel.toggleShuffle() },
                        onDismiss = { musicViewModel.dismissMiniPlayer() },
                        onClick = onMiniPlayerClick
                    )
                }
            }
        }
        }
    }
}

@Composable
fun KivoTopHeader(
    onAiClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    unreadNotifications: Int,
    onProfileClick: () -> Unit,
    offline: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth().background(KivoBlack)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KivoBlack)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KIVO",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = KivoPurpleMain,
                modifier = Modifier.weight(1f),
                letterSpacing = 1.sp
            )
            
            KivoIconButton(icon = Icons.Default.SmartToy, onClick = onAiClick, tint = KivoPurpleMain)
            KivoBadgedIconButton(
                icon = Icons.Default.Notifications,
                onClick = onNotificationsClick,
                badgeCount = unreadNotifications
            )
            KivoIconButton(icon = Icons.Default.Search, onClick = { /* TODO */ })
            KivoIconButton(icon = Icons.Default.Person, onClick = onProfileClick)
        }
        if (offline) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KivoPink.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = KivoPink,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sin conexión: mostrando datos guardados, reintentando...",
                    color = KivoPink,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun KivoBottomNavigation(
    navController: androidx.navigation.NavController,
    currentDestination: androidx.navigation.NavDestination?
) {
    NavigationBar(
        containerColor = KivoPureBlack,
        contentColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.height(70.dp)
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = screen.icon, 
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    ) 
                },
                label = { 
                    Text(
                        text = screen.title,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                selected = selected,
                onClick = {
                    if (currentDestination?.route != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = KivoPurpleMain,
                    selectedTextColor = KivoPurpleMain,
                    unselectedIconColor = KivoTextDisabled,
                    unselectedTextColor = KivoTextDisabled,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun KivoBottomNavigationRail(
    navController: androidx.navigation.NavController,
    currentDestination: androidx.navigation.NavDestination?
) {
    NavigationRail(
        containerColor = KivoPureBlack,
        contentColor = Color.White,
        modifier = Modifier.fillMaxHeight(),
        windowInsets = NavigationRailDefaults.windowInsets
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationRailItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        fontSize = 8.sp,
                        maxLines = 1,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = selected,
                alwaysShowLabel = false,
                onClick = {
                    if (currentDestination?.route != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = KivoPurpleElectric,
                    selectedTextColor = KivoPurpleElectric,
                    unselectedIconColor = KivoTextDisabled,
                    unselectedTextColor = KivoTextDisabled,
                    indicatorColor = KivoPurpleDeep.copy(alpha = 0.4f)
                )
            )
        }
    }
}

@Composable
fun KivoMiniPlayerProCompact(
    currentSong: com.example.kivo.data.models.Song,
    isPlaying: Boolean,
    progress: Float,
    isLoading: Boolean,
    isFavorite: Boolean,
    isShuffleEnabled: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .height(64.dp) // Reducido a 64dp para mayor elegancia
            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = KivoPurpleMain.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(KivoSurface2, KivoPurpleDeep.copy(alpha = 0.4f))))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            // High fidelity ultra-thin progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(GradientAction))
                )
            }

            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art (Compact)
                AsyncImage(
                    model = currentSong.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Song Info with Marquee
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp)
                ) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = Color.White,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = KivoTextSecondary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                }

                // controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    IconButton(onClick = onShuffleClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Shuffle, null, 
                            tint = if (isShuffleEnabled) KivoPurpleElectric else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(onClick = onFavoriteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                            tint = if (isFavorite) KivoPink else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onPreviousClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 2.dp, color = KivoPurpleElectric)
                        }
                        IconButton(onClick = onPlayPauseClick) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }

                    IconButton(onClick = onNextClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Mini Close Button at the very top-right
        IconButton(
            onClick = onDismiss, 
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(10.dp))
        }
    }
}
