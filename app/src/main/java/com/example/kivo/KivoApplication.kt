package com.example.kivo

import android.app.Application
import com.clerk.api.Clerk
import com.example.kivo.data.local.ChatSettingsStore
import com.example.kivo.data.notifications.NotificationCenter
import com.example.kivo.data.remote.NetworkMonitor
import com.example.kivo.data.remote.SessionManager
import com.onesignal.OneSignal

class KivoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        NetworkMonitor.init(this)
        ChatSettingsStore.init(this)
        NotificationCenter.init(this)

        val appId = BuildConfig.ONESIGNAL_APP_ID
        if (appId.isNotBlank()) {
            OneSignal.initWithContext(this, appId)
        }

        val clerkPublishableKey = BuildConfig.CLERK_PUBLISHABLE_KEY
        if (clerkPublishableKey.isNotBlank()) {
            Clerk.initialize(this, clerkPublishableKey)
        }
    }
}