package com.example.kivo.media

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.example.kivo.data.remote.YouTubeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reproduce audio de YouTube mediante un WebView oculto en segundo plano.
 * YouTube bloquea la extracción directa de streams (UMP/PO tokens), así que
 * usamos el reproductor real de YouTube dentro de un WebView invisible y lo
 * controlamos vía JavaScript. Los estados se exponen a través de StateFlows
 * para que la barra mini player refleje reproducción/progreso.
 */
object YouTubeBackgroundPlayer {

    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pollerStarted = false

    /**
     * Intención de reproducir del usuario. Es independiente del estado observado
     * ([_isPlaying]) porque YouTube pausa el video al pasar a segundo plano; el
     * poller usa esta bandera para reanudar aunque el estado observado sea "pausado".
     */
    private var intendedPlaying = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentVideoId = MutableStateFlow<String?>(null)
    val currentVideoId = _currentVideoId.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private val _title = MutableStateFlow<String?>(null)
    val title = _title.asStateFlow()

    private val _artist = MutableStateFlow<String?>(null)
    val artist = _artist.asStateFlow()

    private val _thumbnail = MutableStateFlow<String?>(null)
    val thumbnail = _thumbnail.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds = _durationSeconds.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled = _isShuffleEnabled.asStateFlow()

    /** Cola de reproducción (resultados de búsqueda) compartida con el servicio media. */
    private var queue: List<YouTubeResult> = emptyList()
    private var queueIndex = -1

    /** inner bridge necesario para recibir eventos del JS. */
    private class JSBridge {
        @JavascriptInterface
        fun onState(playing: Boolean, current: Double, duration: Double, ready: Boolean) {
            YouTubeBackgroundPlayer.applyState(playing, current, duration, ready)
        }
    }

    @JvmStatic
    fun applyState(playing: Boolean, current: Double, duration: Double, ready: Boolean) {
        _isPlaying.value = playing
        _positionMs.value = (current * 1000).toLong()
        _durationMs.value = (duration * 1000).toLong()
        _isReady.value = ready
    }

    fun attach(view: WebView) {
        val previous = webView
        if (previous != null && previous !== view) {
            try {
                (previous.parent as? android.view.ViewGroup)?.removeView(previous)
            } catch (_: Exception) {
            }
            try {
                previous.destroy()
            } catch (e: Exception) {
                Log.e("YouTubeBackgroundPlayer", "old webview destroy error: ${e.message}")
            }
        }
        webView = view
        try {
            view.settings.javaScriptEnabled = true
            view.settings.domStorageEnabled = true
            view.settings.mediaPlaybackRequiresUserGesture = false
            view.settings.loadWithOverviewMode = true
            view.settings.useWideViewPort = true
            view.settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            view.webChromeClient = android.webkit.WebChromeClient()
            view.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageStarted(v: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    v?.evaluateJavascript(visibilityOverrideJs, null)
                }

                override fun onPageFinished(v: WebView?, url: String?) {
                    v?.evaluateJavascript(visibilityOverrideJs, null)
                }
            }
            view.addJavascriptInterface(JSBridge(), "AndroidYT")
        } catch (e: Exception) {
            Log.e("YouTubeBackgroundPlayer", "attach error: ${e.message}")
        }
        _currentVideoId.value?.let { reload() }
    }

    fun restart(view: WebView) {
        attach(view)
    }

    fun detach() {
        webView = null
    }

    /** Inicia la reproducción del video. Carga la página móvil de YouTube. */
    fun play(
        videoId: String,
        title: String = "",
        artist: String = "",
        thumbnail: String? = null,
        durationSeconds: Int = 0
    ) {
        intendedPlaying = true
        _currentVideoId.value = videoId
        _title.value = title
        _artist.value = artist
        _thumbnail.value = thumbnail
        _durationSeconds.value = durationSeconds
        reload()
    }

    /** Recarga el video actual (p. ej. al recrearse la vista) sin perder metadatos. */
    fun reload() {
        val videoId = _currentVideoId.value ?: return
        _isPlaying.value = intendedPlaying
        _positionMs.value = 0L
        _durationMs.value = 0L
        _isReady.value = false
        startPoller()
        val wv = webView ?: run { Log.e("YouTubeBackgroundPlayer", "WebView not attached"); return }
        wv.post {
            try {
                wv.loadUrl("https://m.youtube.com/watch?v=$videoId&autoplay=1")
            } catch (e: Exception) {
                Log.e("YouTubeBackgroundPlayer", "loadUrl error: ${e.message}")
            }
        }
    }

    fun pause() {
        intendedPlaying = false
        _isPlaying.value = false
        evalJs("(function(){var v=document.querySelector('video');if(v)v.pause();})()")
    }

    fun resume() {
        intendedPlaying = true
        _isPlaying.value = true
        evalJs("(function(){var v=document.querySelector('video');if(v)v.play();})()")
    }

    fun toggle() {
        if (intendedPlaying) pause() else resume()
    }

    fun seekTo(positionMs: Long) {
        val seconds = positionMs.coerceAtLeast(0L) / 1000.0
        evalJs("(function(){var v=document.querySelector('video');if(v)v.currentTime=$seconds;})()")
    }

    fun seekForward(millis: Long = 10000L) {
        seekTo(_positionMs.value + millis)
    }

    fun seekBackward(millis: Long = 10000L) {
        seekTo(_positionMs.value - millis)
    }

    fun stop() {
        evalJs("(function(){var v=document.querySelector('video');if(v){v.pause();try{v.src='';}catch(e){}}})()")
        intendedPlaying = false
        _isPlaying.value = false
        _positionMs.value = 0L
        _durationMs.value = 0L
        _isReady.value = false
        _currentVideoId.value = null
        _title.value = null
        _artist.value = null
        _thumbnail.value = null
        _durationSeconds.value = 0
        queue = emptyList()
        queueIndex = -1
        stopPoller()
    }

    /** Define la cola de reproducción y arranca en [startIndex]. */
    fun setQueue(videos: List<YouTubeResult>, startIndex: Int) {
        queue = videos
        queueIndex = startIndex.coerceIn(0, (videos.size - 1).coerceAtLeast(0))
    }

    fun playFromQueue(index: Int) {
        if (index !in queue.indices) return
        queueIndex = index
        val video = queue[index]
        play(
            videoId = video.videoId,
            title = video.title,
            artist = video.channelName,
            thumbnail = video.thumbnailUrl,
            durationSeconds = video.lengthSeconds
        )
    }

    fun next() {
        if (queueIndex !in queue.indices) return
        val target = if (_isShuffleEnabled.value) {
            if (queue.size <= 1) return
            var randomIndex: Int
            do {
                randomIndex = queue.indices.random()
            } while (randomIndex == queueIndex)
            randomIndex
        } else {
            if (queueIndex >= queue.lastIndex) return
            queueIndex + 1
        }
        playFromQueue(target)
    }

    fun previous() {
        if (queueIndex !in queue.indices) return
        val target = if (queueIndex <= 0) 0 else queueIndex - 1
        if (target == queueIndex && queueIndex == 0) {
            seekTo(0L)
            return
        }
        playFromQueue(target)
    }

    fun setShuffle(enabled: Boolean) {
        _isShuffleEnabled.value = enabled
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun destroy() {
        stop()
        webView?.destroy()
        webView = null
    }

    private fun evalJs(script: String) {
        val wv = webView ?: return
        wv.post {
            try {
                wv.evaluateJavascript(script, null)
            } catch (e: Exception) {
                Log.e("YouTubeBackgroundPlayer", "evalJs error: ${e.message}")
            }
        }
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            evalJs(buildPollJs(intendedPlaying))
            mainHandler.postDelayed(this, 1000)
        }
    }

    private fun startPoller() {
        if (pollerStarted) return
        pollerStarted = true
        mainHandler.postDelayed(pollRunnable, 1000)
    }

    private fun stopPoller() {
        pollerStarted = false
        mainHandler.removeCallbacks(pollRunnable)
    }

    /**
     * Script de sondeo. Fuerza volumen si YouTube dejó el autoplay muteado y,
     * si nuestra intención es reproducir, reintenta play() (el autoplay del
     * WebView puede quedar bloqueado).
     */
    /**
     * Fuerza la Page Visibility API a "visible". YouTube pausa/reinicia su
     * reproductor cuando detecta que la pestaña está oculta (p. ej. al pasar la
     * app a segundo plano), lo que detendría el audio.
     */
    private val visibilityOverrideJs = """
        (function(){
            try {
                Object.defineProperty(document, 'hidden', {get: function(){return false;}, configurable: true});
                Object.defineProperty(document, 'visibilityState', {get: function(){return 'visible';}, configurable: true});
                if (!window.__kivoVisHooked) {
                    window.__kivoVisHooked = true;
                    var stop = function(e){ e.stopImmediatePropagation(); };
                    window.addEventListener('visibilitychange', stop, true);
                    document.addEventListener('visibilitychange', stop, true);
                    window.addEventListener('blur', stop, true);
                }
            } catch(e) {}
        })();
    """.trimIndent()

    private fun buildPollJs(shouldPlay: Boolean): String = """
        (function(){
            try {
                $visibilityOverrideJs
                var v = document.querySelector('video');
                if (!v) { AndroidYT.onState(false, 0, 0, false); return; }
                if (v.muted) { v.muted = false; v.volume = 1; }
                if ($shouldPlay && v.paused) { var p = v.play(); if (p && p.catch) p.catch(function(){}); }
                AndroidYT.onState(!v.paused, v.currentTime, v.duration || 0, true);
            } catch(e) { AndroidYT.onState(false, 0, 0, false); }
        })();
    """.trimIndent()
}