package com.example.kivo.data.remote

import com.google.gson.JsonParser
import retrofit2.HttpException

fun Throwable.toApiMessage(): String {
    if (this is HttpException) {
        val body = response()?.errorBody()?.string()
        if (body != null) {
            val message = runCatching {
                JsonParser.parseString(body).asJsonObject.get("error")?.asString
            }.getOrNull()
            if (!message.isNullOrBlank()) return message
        }
        val code = code()
        return when (code) {
            401 -> "Credenciales incorrectas"
            409 -> "Ese correo o usuario ya esta registrado"
            in 500..599 -> "Error del servidor"
            else -> "Error $code"
        }
    }
    return message ?: "Error de conexion con el servidor"
}