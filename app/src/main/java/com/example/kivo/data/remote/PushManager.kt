package com.example.kivo.data.remote

import com.example.kivo.BuildConfig
import com.onesignal.OneSignal

object PushManager {

    fun onUserLoggedIn(userId: String) {
        if (BuildConfig.ONESIGNAL_APP_ID.isBlank()) return
        runCatching {
            OneSignal.login(userId)
        }
    }

    fun onUserLoggedOut() {
        if (BuildConfig.ONESIGNAL_APP_ID.isBlank()) return
        runCatching {
            OneSignal.logout()
        }
    }
}