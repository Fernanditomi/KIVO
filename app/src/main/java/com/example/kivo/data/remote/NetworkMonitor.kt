package com.example.kivo.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkMonitor {
    private const val PREFS = "kivo_network"
    private const val KEY_BASE = "preferred_base"

    private val _online = MutableStateFlow(true)
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private lateinit var appContext: Context
    private var cm: ConnectivityManager? = null
    private var callback: ConnectivityManager.NetworkCallback? = null

    private var bases: List<String> = emptyList()
    @Volatile
    private var preferredIndex = 0

    fun init(context: Context) {
        appContext = context.applicationContext
        bases = listOfNotNull(
            com.example.kivo.BuildConfig.KIVO_BASE_URL,
            com.example.kivo.BuildConfig.KIVO_BASE_URL_ALT.takeIf { it.isNotBlank() }
        )
        val saved = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BASE, null)
        val idx = saved?.let { s -> bases.indexOf(s) } ?: -1
        preferredIndex = if (idx >= 0) idx else 0

        cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { _online.value = true }
            override fun onLost(network: Network) {
                _online.value = cm?.activeNetwork != null
            }
            override fun onUnavailable() {
                _online.value = cm?.activeNetwork != null
            }
        }
        callback = cb
        cm?.registerDefaultNetworkCallback(cb)
        _online.value = hasInternet()
    }

    fun hasInternet(): Boolean {
        val manager = cm ?: return true
        val caps = manager.getNetworkCapabilities(manager.activeNetwork) ?: return true
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun baseUrls(): List<String> = bases

    fun preferredBase(): String =
        bases.getOrElse(preferredIndex) { com.example.kivo.BuildConfig.KIVO_BASE_URL }

    fun markFailure(baseUrl: String) {
        val idx = bases.indexOf(baseUrl)
        if (idx >= 0 && bases.size > 1) {
            preferredIndex = (idx + 1) % bases.size
        }
    }

    fun markSuccess(baseUrl: String) {
        val idx = bases.indexOf(baseUrl)
        if (idx >= 0) {
            preferredIndex = idx
            appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_BASE, baseUrl).apply()
        }
    }
}