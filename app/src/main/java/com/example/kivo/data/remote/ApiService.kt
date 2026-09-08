package com.example.kivo.data.remote

import com.example.kivo.data.models.Conversation
import com.example.kivo.data.models.Message
import com.example.kivo.data.models.User
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

data class CreateConversationRequest(
    val otherId: String
)

data class SendMessageRequest(
    val text: String,
    val receiverId: String,
    val type: String = "text"
)

data class PresenceRequest(
    val isOnline: Boolean
)

data class ProfileUpdateRequest(
    val username: String,
    val displayName: String,
    val bio: String,
    val photoUrl: String?
)

data class AvailabilityResponse(
    val available: Boolean
)

data class ReadResponse(
    val updated: Int
)

data class DeviceRequest(
    val onesignalPlayerId: String,
    val platform: String = "android"
)

data class UploadImageRequest(
    val base64: String
)

data class UploadResponse(
    val url: String
)

interface ApiService {

    @GET("api/users/me")
    suspend fun getMe(): User

    @GET("api/users/check")
    suspend fun checkUsername(@Query("username") username: String): AvailabilityResponse

    @GET("api/users/search")
    suspend fun searchUsers(@Query("q") query: String): List<User>

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: String): User

    @PUT("api/users/{id}")
    suspend fun updateProfile(@Path("id") id: String, @Body body: ProfileUpdateRequest): User

    @PUT("api/users/{id}/presence")
    suspend fun setPresence(@Path("id") id: String, @Body body: PresenceRequest): User

    @GET("api/conversations")
    suspend fun getConversations(): List<Conversation>

    @POST("api/conversations")
    suspend fun createConversation(@Body body: CreateConversationRequest): Conversation

    @GET("api/conversations/{id}/messages")
    suspend fun getMessages(@Path("id") conversationId: String): List<Message>

    @POST("api/conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") conversationId: String, @Body body: SendMessageRequest): Message

    @POST("api/conversations/{id}/read")
    suspend fun markAsRead(@Path("id") conversationId: String): ReadResponse

    @POST("api/devices")
    suspend fun registerDevice(@Body body: DeviceRequest): Map<String, Any>

    @POST("api/uploads")
    suspend fun uploadImage(@Body body: UploadImageRequest): UploadResponse

    @DELETE("api/conversations/{id}/messages")
    suspend fun clearMessages(@Path("id") conversationId: String): Map<String, Any>

    @DELETE("api/conversations/{id}")
    suspend fun deleteConversation(@Path("id") conversationId: String): Map<String, Any>
}