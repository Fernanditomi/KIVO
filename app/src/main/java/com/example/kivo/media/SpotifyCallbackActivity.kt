package com.example.kivo.media

import android.app.Activity
import android.os.Bundle
import android.util.Log

class SpotifyCallbackActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        Log.d("SpotifyCallback", "Received URI: $uri")

        if (uri != null && uri.toString().startsWith("kivo://spotify-callback")) {
            val fragment = uri.fragment
            if (fragment != null) {
                val params = fragment.split("&").associate {
                    val parts = it.split("=", limit = 2)
                    parts[0] to (parts.getOrElse(1) { "" })
                }
                val token = params["access_token"]
                if (token != null) {
                    Log.d("SpotifyCallback", "Token received successfully")
                    SpotifyPendingAuth.token = token
                } else {
                    Log.e("SpotifyCallback", "No token in redirect: $fragment")
                    SpotifyPendingAuth.error = params["error"] ?: "unknown"
                }
            } else {
                Log.e("SpotifyCallback", "No fragment in URI")
            }
        }

        finish()
    }
}

object SpotifyPendingAuth {
    var token: String? = null
    var error: String? = null

    fun consume(): String? {
        val t = token
        token = null
        error = null
        return t
    }
}
