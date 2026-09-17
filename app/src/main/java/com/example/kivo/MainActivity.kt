package com.example.kivo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kivo.media.YouTubeBackgroundPlayer
import com.example.kivo.ui.MainScreen
import com.example.kivo.ui.theme.KIVOTheme

/**
 * WebView que nunca consume toques, para que la UI Compose que está por encima
 * reciba la interacción aunque este view ocupe la pantalla.
 */
private class NonTouchableWebView(context: Context) : WebView(context) {
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean = false
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Forzar orientación vertical (Portrait)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        super.onCreate(savedInstanceState)

        // Solicitar permiso de notificaciones para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }

        // Solicitar permiso de almacenamiento para reproducir canciones locales
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 1)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            }
        }

        val canOverlay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Settings.canDrawOverlays(this)

        // Fallback: si el usuario no concedió "Aparecer encima de otras apps",
        // el WebView se aloja en la Activity (solo reproduce en primer plano).
        if (!canOverlay) {
            val contentRoot = findViewById<ViewGroup>(android.R.id.content)
            val ytWebView = NonTouchableWebView(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                alpha = 0f
                YouTubeBackgroundPlayer.attach(this)
            }
            contentRoot.addView(ytWebView, 0)
        }

        enableEdgeToEdge()
        setContent {
            KIVOTheme {
                var needsOverlay by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { needsOverlay = !canOverlay }
                MainScreen()
                if (needsOverlay) {
                    AlertDialog(
                        onDismissRequest = { needsOverlay = false },
                        title = { Text("Reproducción en segundo plano") },
                        text = {
                            Text(
                                "Para que la música de YouTube siga sonando con la pantalla apagada " +
                                    "o usando otras apps, KIVO necesita el permiso \"Aparecer encima " +
                                    "de otras apps\"."
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                needsOverlay = false
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                                try {
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    startActivity(
                                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                    )
                                }
                            }) { Text("Conceder") }
                        },
                        dismissButton = {
                            TextButton(onClick = { needsOverlay = false }) { Text("Ahora no") }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
