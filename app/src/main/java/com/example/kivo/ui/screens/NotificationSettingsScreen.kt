package com.example.kivo.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kivo.data.notifications.NotificationSettingsStore
import com.example.kivo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val settings by NotificationSettingsStore.values.collectAsState()
    val context = LocalContext.current

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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = KivoBlack)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Tipo de notificaciones",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KivoPurpleElectric,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsGroup {
                SettingSwitch(
                    icon = Icons.Default.Info,
                    color = KivoTextSecondary,
                    title = "Sistema",
                    subtitle = "Avisos, novedades e información de Kivo",
                    checked = settings.general,
                    onCheckedChange = NotificationSettingsStore::setGeneral
                )
                SettingSwitch(
                    icon = Icons.Default.MusicNote,
                    color = KivoPurpleElectric,
                    title = "Música",
                    subtitle = "Recomendaciones y canciones terminadas",
                    checked = settings.music,
                    onCheckedChange = NotificationSettingsStore::setMusic
                )
                SettingSwitch(
                    icon = Icons.Default.SportsEsports,
                    color = KivoOrange,
                    title = "Juegos",
                    subtitle = "Récords, retos y recompensas",
                    checked = settings.games,
                    onCheckedChange = NotificationSettingsStore::setGames
                )
                SettingSwitch(
                    icon = Icons.Default.People,
                    color = KivoGreen,
                    title = "Social",
                    subtitle = "Mensajes, llamadas e interacciones",
                    checked = settings.social,
                    onCheckedChange = NotificationSettingsStore::setSocial
                )
                SettingSwitch(
                    icon = Icons.Default.FileDownload,
                    color = KivoBlue,
                    title = "Descargas",
                    subtitle = "Progreso y contenido disponible",
                    checked = settings.downloads,
                    onCheckedChange = NotificationSettingsStore::setDownloads
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Modo de aviso",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KivoPurpleElectric,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsGroup {
                SettingSwitch(
                    icon = Icons.Default.VolumeUp,
                    color = KivoPurpleElectric,
                    title = "Sonido",
                    subtitle = "Reproducir sonido con cada notificación",
                    checked = settings.sound,
                    onCheckedChange = NotificationSettingsStore::setSound
                )
                SettingSwitch(
                    icon = Icons.Default.Vibration,
                    color = KivoPurpleElectric,
                    title = "Vibración",
                    subtitle = "Vibrar cuando llega una notificación",
                    checked = settings.vibration,
                    onCheckedChange = NotificationSettingsStore::setVibration
                )
                SettingSwitch(
                    icon = Icons.Default.Lock,
                    color = KivoPurpleElectric,
                    title = "Ocultar contenido en bloqueo",
                    subtitle = "Mostrar solo un aviso genérico en la pantalla de bloqueo",
                    checked = !settings.lockscreenContent,
                    onCheckedChange = { hidden ->
                        NotificationSettingsStore.setLockscreenContent(!hidden)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Opciones avanzadas",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KivoPurpleElectric,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            SettingsGroup {
                Surface(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(KivoSurface3),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = KivoTextSecondary, modifier = Modifier.size(18.dp))
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text("Canales del sistema", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Ajustar cada canal en los Ajustes del teléfono", color = KivoTextDisabled, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = KivoTextDisabled)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = KivoTextDisabled, modifier = Modifier.size(16.dp))
                Text(
                    "Los cambios se aplican a las notificaciones futuras.",
                    color = KivoTextDisabled,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KivoSurface2)
            .border(0.5.dp, KivoBorder, RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingSwitch(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = KivoTextDisabled, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = KivoPurpleMain,
                uncheckedThumbColor = KivoTextDisabled,
                uncheckedTrackColor = KivoSurface3
            )
        )
    }
}