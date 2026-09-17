package com.example.kivo.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.kivo.MainActivity
import com.example.kivo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que expone la reproducción de YouTube como una sesión
 * multimedia del sistema (aviso/"isla" de reproducción y controles en pantalla de
 * bloqueo) y mantiene el proceso vivo para que el audio siga en segundo plano.
 */
class YouTubeMediaService : Service() {

    private lateinit var session: MediaSessionCompat
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var metadataJob: Job? = null
    private var stateJob: Job? = null
    private var artwork: Bitmap? = null
    private var lastArtworkUrl: String? = null
    private var isForeground = false
    private var overlayView: WebView? = null

    private data class Meta(
        val id: String?,
        val title: String?,
        val artist: String?,
        val thumbnail: String?,
        val durationMs: Long
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        session = MediaSessionCompat(this, "KIVO_YouTube").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setSessionActivity(contentIntent())
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = YouTubeBackgroundPlayer.resume()
                override fun onPause() = YouTubeBackgroundPlayer.pause()
                override fun onSkipToNext() = YouTubeBackgroundPlayer.next()
                override fun onSkipToPrevious() = YouTubeBackgroundPlayer.previous()
                override fun onSeekTo(pos: Long) = YouTubeBackgroundPlayer.seekTo(pos)
                override fun onStop() = YouTubeBackgroundPlayer.stop()
            })
            isActive = true
        }
        ensureOverlayWebView()
        observePlayer()
    }

    /**
     * El WebView de YouTube vive dentro de una ventana overlay de 4x4 px que nunca
     * deja de ser "visible" para Chromium. Si estuviera dentro de la Activity, al pasar
     * la app a segundo plano la pagina se marcaria como oculta y Chromium suspenderia
     * el audio. Al no ser táctil ni enfocable no interfiere con el usuario.
     */
    private fun ensureOverlayWebView() {
        if (overlayView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Sin permiso SYSTEM_ALERT_WINDOW: no habra reproduccion en 2do plano")
            return
        }
        val wm = getSystemService(WindowManager::class.java) ?: return
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            OVERLAY_SIZE,
            OVERLAY_SIZE,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
        val view = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        YouTubeBackgroundPlayer.attach(view)
        try {
            wm.addView(view, params)
            overlayView = view
            Log.d(TAG, "Overlay WebView agregado")
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo agregar el overlay WebView: ${e.message}")
        }
    }

    private fun removeOverlayWebView() {
        val view = overlayView ?: return
        overlayView = null
        try {
            (getSystemService(WindowManager::class.java))?.removeView(view)
        } catch (_: Exception) {
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> YouTubeBackgroundPlayer.resume()
            ACTION_PAUSE -> YouTubeBackgroundPlayer.pause()
            ACTION_NEXT -> YouTubeBackgroundPlayer.next()
            ACTION_PREVIOUS -> YouTubeBackgroundPlayer.previous()
            ACTION_STOP -> {
                YouTubeBackgroundPlayer.stop()
                return START_NOT_STICKY
            }
        }
        if (!isForeground && YouTubeBackgroundPlayer.currentVideoId.value != null) {
            startForegroundNotification()
        }
        return START_STICKY
    }

    private fun observePlayer() {
        metadataJob = scope.launch {
            combine(
                YouTubeBackgroundPlayer.currentVideoId,
                YouTubeBackgroundPlayer.title,
                YouTubeBackgroundPlayer.artist,
                YouTubeBackgroundPlayer.thumbnail,
                YouTubeBackgroundPlayer.durationMs
            ) { id, title, artist, thumbnail, durationMs ->
                Meta(id, title, artist, thumbnail, durationMs)
            }.collect { meta ->
                if (meta.id == null) {
                    stopForegroundAndSelf()
                    return@collect
                }
                if (meta.thumbnail != lastArtworkUrl) {
                    lastArtworkUrl = meta.thumbnail
                    artwork = null
                    meta.thumbnail?.let { loadArtwork(it) }
                }
                session.setMetadata(
                    MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, meta.title ?: "KIVO")
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, meta.artist ?: "")
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, meta.durationMs)
                        .apply { artwork?.let { putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it) } }
                        .build()
                )
                refreshNotification()
                if (!isForeground) startForegroundNotification()
            }
        }
        stateJob = scope.launch {
            combine(
                YouTubeBackgroundPlayer.isPlaying,
                YouTubeBackgroundPlayer.positionMs
            ) { playing, position -> playing to position }
                .collect { (playing, position) ->
                    session.setPlaybackState(
                        PlaybackStateCompat.Builder()
                            .setActions(
                                PlaybackStateCompat.ACTION_PLAY or
                                    PlaybackStateCompat.ACTION_PAUSE or
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                    PlaybackStateCompat.ACTION_SEEK_TO or
                                    PlaybackStateCompat.ACTION_STOP
                            )
                            .setState(
                                if (playing) PlaybackStateCompat.STATE_PLAYING
                                else PlaybackStateCompat.STATE_PAUSED,
                                position,
                                1f
                            )
                            .build()
                    )
                    refreshNotification()
                }
        }
    }

    private fun loadArtwork(url: String) {
        scope.launch {
            try {
                val request = ImageRequest.Builder(this@YouTubeMediaService)
                    .data(url)
                    .allowHardware(false)
                    .size(512, 512)
                    .build()
                val result = this@YouTubeMediaService.imageLoader.execute(request)
                val drawable = (result as? SuccessResult)?.drawable
                if (drawable is BitmapDrawable) {
                    artwork = drawable.bitmap
                    session.setMetadata(
                        MediaMetadataCompat.Builder()
                            .putString(
                                MediaMetadataCompat.METADATA_KEY_TITLE,
                                YouTubeBackgroundPlayer.title.value ?: "KIVO"
                            )
                            .putString(
                                MediaMetadataCompat.METADATA_KEY_ARTIST,
                                YouTubeBackgroundPlayer.artist.value ?: ""
                            )
                            .putLong(
                                MediaMetadataCompat.METADATA_KEY_DURATION,
                                YouTubeBackgroundPlayer.durationMs.value
                            )
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
                            .build()
                    )
                    refreshNotification()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun startForegroundNotification() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
        )
        isForeground = true
    }

    private fun refreshNotification() {
        if (!isForeground) return
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun stopForegroundAndSelf() {
        if (isForeground) {
            isForeground = false
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val playing = YouTubeBackgroundPlayer.isPlaying.value
        val title = YouTubeBackgroundPlayer.title.value
        val artist = YouTubeBackgroundPlayer.artist.value
        val style = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(session.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_kivo)
            .setContentTitle(title ?: "KIVO")
            .setContentText(artist ?: "")
            .setLargeIcon(artwork)
            .setContentIntent(contentIntent())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(playing)
            .setShowWhen(false)
            .addAction(
                android.R.drawable.ic_media_previous,
                "Anterior",
                servicePendingIntent(ACTION_PREVIOUS)
            )
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "Pausar" else "Reproducir",
                servicePendingIntent(if (playing) ACTION_PAUSE else ACTION_PLAY)
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Siguiente",
                servicePendingIntent(ACTION_NEXT)
            )
            .setStyle(style)
            .build()
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun servicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, YouTubeMediaService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager?.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Reproducción de KIVO",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Controles de reproducción de KIVO"
                    setShowBadge(false)
                }
                manager?.createNotificationChannel(channel)
            }
        }
    }

    override fun onDestroy() {
        removeOverlayWebView()
        YouTubeBackgroundPlayer.destroy()
        metadataJob?.cancel()
        stateJob?.cancel()
        scope.cancel()
        session.isActive = false
        session.release()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "YouTubeMediaService"
        private const val CHANNEL_ID = "kivo_youtube_playback"
        private const val NOTIFICATION_ID = 20481
        private const val OVERLAY_SIZE = 4
        private const val ACTION_PLAY = "com.example.kivo.YT_PLAY"
        private const val ACTION_PAUSE = "com.example.kivo.YT_PAUSE"
        private const val ACTION_NEXT = "com.example.kivo.YT_NEXT"
        private const val ACTION_PREVIOUS = "com.example.kivo.YT_PREVIOUS"
        private const val ACTION_STOP = "com.example.kivo.YT_STOP"

        fun start(context: Context) {
            val intent = Intent(context, YouTubeMediaService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
