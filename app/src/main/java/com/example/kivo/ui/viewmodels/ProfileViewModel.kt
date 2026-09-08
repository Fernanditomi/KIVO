package com.example.kivo.ui.viewmodels

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kivo.data.models.User
import com.example.kivo.data.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val prefs = application.getSharedPreferences("kivo_profile", Context.MODE_PRIVATE)

    // Valores iniciales genÃ©ricos
    private val _name = MutableStateFlow(prefs.getString("name", "Tu Nombre") ?: "Tu Nombre")
    val name = _name.asStateFlow()

    private val _username = MutableStateFlow(prefs.getString("username", "usuario") ?: "usuario")
    val username = _username.asStateFlow()

    private val _description = MutableStateFlow(prefs.getString("description", "") ?: "")
    val description = _description.asStateFlow()
    
    private val _pronouns = MutableStateFlow(prefs.getString("pronouns", "") ?: "")
    val pronouns = _pronouns.asStateFlow()

    private val _profileImageUri = MutableStateFlow(prefs.getString("profile_image", null))
    val profileImageUri = _profileImageUri.asStateFlow()

    private val _following = MutableStateFlow(prefs.getString("following", "0") ?: "0")
    val following = _following.asStateFlow()

    private val _followers = MutableStateFlow(prefs.getString("followers", "0") ?: "0")
    val followers = _followers.asStateFlow()

    private val _likes = MutableStateFlow(prefs.getString("likes", "0") ?: "0")
    val likes = _likes.asStateFlow()

    private var currentPassword = ""

    fun updateName(newName: String) {
        _name.value = newName
        prefs.edit().putString("name", newName).apply()
        syncProfile()
    }

    fun updateUsername(newUsername: String) {
        _username.value = newUsername
        prefs.edit().putString("username", newUsername).apply()
        syncProfile()
    }

    fun updateDescription(newDesc: String) {
        _description.value = newDesc
        prefs.edit().putString("description", newDesc).apply()
        syncProfile()
    }

    fun updatePronouns(newPronouns: String) {
        _pronouns.value = newPronouns
        prefs.edit().putString("pronouns", newPronouns).apply()
        syncProfile()
    }
    
    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = AuthRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val user = AuthRepository.getUserProfile(uid)
            if (user != null) {
                _name.value = user.displayName
                _username.value = user.username
                _description.value = user.bio
                _profileImageUri.value = user.photoUrl
                currentPassword = user.password
                
                // Update prefs to keep sync
                prefs.edit().apply {
                    putString("name", user.displayName)
                    putString("username", user.username)
                    putString("description", user.bio)
                    putString("profile_image", user.photoUrl)
                }.apply()
            }
        }
    }

    private fun syncProfile() {
        val uid = AuthRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            val user = User(
                userId = uid,
                username = _username.value,
                usernameLowercase = _username.value.lowercase().trim(),
                displayName = _name.value,
                photoUrl = _profileImageUri.value,
                bio = _description.value,
                password = currentPassword
            )
            AuthRepository.updateProfile(user)
        }
    }

    /**
     * Guarda la imagen de perfil permanentemente copiÃ¡ndola al almacenamiento interno de la app.
     * Esto evita que se pierda el acceso al cerrar la aplicaciÃ³n (URI permission loss).
     */
    fun updateProfileImage(uri: String) {
        try {
            val sourceUri = Uri.parse(uri)
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            
            // Creamos un archivo local permanente
            val file = File(context.filesDir, "profile_picture.jpg")
            val outputStream = FileOutputStream(file)
            
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            // Guardamos la ruta del archivo local, no la URI temporal de la galerÃ­a
            val localPath = file.absolutePath
            _profileImageUri.value = localPath
            prefs.edit().putString("profile_image", localPath).apply()
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: si falla la copia, intentamos guardar la URI original (aunque sea temporal)
            _profileImageUri.value = uri
            prefs.edit().putString("profile_image", uri).apply()
        }
    }

    fun updateProfilePhotoUrl(url: String) {
        _profileImageUri.value = url
        prefs.edit().putString("profile_image", url).apply()
        syncProfile()
    }

    fun updateFollowing(value: String) {
        _following.value = value
        prefs.edit().putString("following", value).apply()
    }

    fun updateFollowers(value: String) {
        _followers.value = value
        prefs.edit().putString("followers", value).apply()
    }

    fun updateLikes(value: String) {
        _likes.value = value
        prefs.edit().putString("likes", value).apply()
    }
}

