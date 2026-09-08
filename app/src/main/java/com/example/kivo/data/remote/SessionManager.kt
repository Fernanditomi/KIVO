package com.example.kivo.data.remote

import android.content.Context
import com.example.kivo.data.models.User
import com.google.gson.Gson

object SessionManager {
    private const val PREFS = "kivo_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER = "user_json"

    private lateinit var appContext: Context
    private val gson = Gson()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveSession(token: String, user: User) {
        prefs().edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, gson.toJson(user))
            .apply()
    }

    fun updateCachedUser(user: User) {
        val token = getToken() ?: return
        prefs().edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, gson.toJson(user))
            .apply()
    }

    fun getToken(): String? = prefs().getString(KEY_TOKEN, null)

    fun getCachedUser(): User? {
        val json = prefs().getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        prefs().edit().clear().apply()
    }
}