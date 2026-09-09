package com.example.kivo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.kivo.data.remote.AgoraManager
import com.example.kivo.data.remote.CallManager
import com.example.kivo.data.remote.CallType
import com.example.kivo.data.remote.CallState
import com.example.kivo.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    conversationId: String,
    otherUserId: String,
    otherUserName: String,
    callType: String = "voice",
    onEndCall: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val callState by CallManager.callState.collectAsState()
    val currentCall by CallManager.currentCall.collectAsState()
    val isMuted by CallManager.isMuted.collectAsState()
    val isSpeaker by CallManager.isSpeaker.collectAsState()
    val callDuration by CallManager.callDuration.collectAsState()
    val remoteUserJoined by CallManager.remoteUserJoined.collectAsState()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val isVideo = callType == "video"
    val agoraManager = remember { AgoraManager(context) }
    var localContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var remoteContainer by remember { mutableStateOf<FrameLayout?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (isVideo) permissions.add(Manifest.permission.CAMERA)
        permissionLauncher.launch(permissions.toTypedArray())
        CallManager.setAgoraManager(agoraManager)
        agoraManager.initialize()
    }

    LaunchedEffect(callState) {
        if (callState == CallState.Active || callState == CallState.Connecting) {
            if (isVideo) {
                localContainer?.let { agoraManager.setupLocalVideo(it) }
            }
        }
    }

    LaunchedEffect(remoteUserJoined) {
        if (remoteUserJoined && isVideo) {
            remoteContainer?.let { agoraManager.setupRemoteVideo(1, it) }
        }
    }

    LaunchedEffect(callState) {
        if (callState == CallState.Active) {
            while (true) {
                CallManager.updateDuration()
                delay(1000)
            }
        }
    }

    LaunchedEffect(callState) {
        if (callState == CallState.Ended) {
            agoraManager.destroy()
            onEndCall()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            agoraManager.destroy()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KivoBlack)
    ) {
        if (isVideo) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply { remoteContainer = this }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            localContainer = this
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(120.dp, 180.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = if (isVideo) 240.dp else 60.dp)
            ) {
                if (!isVideo) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(KivoSurface2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = otherUserName.take(1).uppercase(),
                            color = KivoPurpleMain,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = otherUserName,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (callState) {
                        CallState.Outgoing -> "Llamando..."
                        CallState.Incoming -> "Llamada entrante"
                        CallState.Connecting -> "Conectando..."
                        CallState.Active -> formatDuration(callDuration)
                        CallState.Ended -> "Llamada finalizada"
                        is CallState.Error -> "Error"
                        else -> ""
                    },
                    color = when (callState) {
                        CallState.Active -> KivoGreen
                        is CallState.Error -> KivoPink
                        else -> KivoTextSecondary
                    },
                    fontSize = 16.sp
                )
            }

            if (callState == CallState.Active && !isVideo) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { CallManager.toggleMute() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) KivoPink else KivoSurface3)
                        ) {
                            Icon(
                                if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = "Micrófono",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text("Micrófono", color = KivoTextSecondary, fontSize = 12.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { CallManager.toggleSpeaker() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isSpeaker) KivoPurpleMain else KivoSurface3)
                        ) {
                            Icon(
                                if (isSpeaker) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                                contentDescription = "Altavoz",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text("Altavoz", color = KivoTextSecondary, fontSize = 12.sp)
                    }
                }
            }

            if (callState == CallState.Active && isVideo) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { CallManager.toggleMute() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) KivoPink else KivoSurface3)
                        ) {
                            Icon(
                                if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = "Micrófono",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { CallManager.toggleCamera() },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(KivoSurface3)
                        ) {
                            Icon(
                                Icons.Filled.Cameraswitch,
                                contentDescription = "Cambiar cámara",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (callState == CallState.Incoming) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { CallManager.rejectCall() },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(KivoPink)
                        ) {
                            Icon(
                                Icons.Filled.CallEnd,
                                contentDescription = "Rechazar",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text("Rechazar", color = KivoTextSecondary, fontSize = 12.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { CallManager.acceptCall() },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(KivoGreen)
                        ) {
                            Icon(
                                Icons.Filled.Call,
                                contentDescription = "Aceptar",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text("Aceptar", color = KivoTextSecondary, fontSize = 12.sp)
                    }
                } else if (callState != CallState.Ended) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                CallManager.endCall()
                                onEndCall()
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(KivoPink)
                        ) {
                            Icon(
                                Icons.Filled.CallEnd,
                                contentDescription = "Finalizar",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text("Finalizar", color = KivoTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
