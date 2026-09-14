package com.example.kivo.data.remote

import com.example.kivo.data.models.SpotifySearchResponse
import com.example.kivo.data.models.SpotifyNewReleasesResponse
import com.example.kivo.data.models.SpotifyTokenResponse
import retrofit2.Response
import retrofit2.http.*

interface SpotifyApiService {

    @POST("api/token")
    @FormUrlEncoded
    suspend fun getToken(
        @Field("grant_type") grantType: String = "client_credentials",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): Response<SpotifyTokenResponse>

    @GET("v1/search")
    suspend fun search(
        @Header("Authorization") auth: String,
        @Query("q") query: String,
        @Query("type") type: String,
        @Query("market") market: String
    ): Response<SpotifySearchResponse>

    @GET("v1/browse/new-releases")
    suspend fun getNewReleases(
        @Header("Authorization") auth: String,
        @Query("country") country: String,
        @Query("limit") limit: String,
        @Query("offset") offset: String
    ): Response<SpotifyNewReleasesResponse>

    @GET("v1/albums/{id}/tracks")
    suspend fun getAlbumTracks(
        @Header("Authorization") auth: String,
        @Path("id") albumId: String,
        @Query("limit") limit: String,
        @Query("offset") offset: String
    ): Response<SpotifySearchResponse>
}
