package com.example.kivo.data.models

import com.google.gson.annotations.SerializedName

data class SpotifySearchResponse(
    val tracks: SpotifyTracksResult?
)

data class SpotifyTracksResult(
    val href: String,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int,
    val items: List<SpotifyTrack>
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val uri: String,
    @SerializedName("duration_ms") val durationMs: Int,
    val explicit: Boolean,
    val popularity: Int,
    @SerializedName("preview_url") val previewUrl: String?,
    val artists: List<SpotifyArtist>,
    val album: SpotifyAlbum
)

data class SpotifyArtist(
    val id: String,
    val name: String,
    val uri: String
)

data class SpotifyAlbum(
    val id: String,
    val name: String,
    @SerializedName("release_date") val releaseDate: String,
    val images: List<SpotifyImage>,
    @SerializedName("total_tracks") val totalTracks: Int
)

data class SpotifyImage(
    val url: String,
    val height: Int,
    val width: Int
)

data class SpotifyNewReleasesResponse(
    val albums: SpotifyAlbumsResult?
)

data class SpotifyAlbumsResult(
    val href: String,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int,
    val items: List<SpotifyAlbumFull>
)

data class SpotifyAlbumFull(
    val id: String,
    val name: String,
    @SerializedName("release_date") val releaseDate: String,
    val images: List<SpotifyImage>,
    @SerializedName("total_tracks") val totalTracks: Int,
    val artists: List<SpotifyArtist>,
    val tracks: SpotifyAlbumTracks?
)

data class SpotifyAlbumTracks(
    val items: List<SpotifyTrack>
)

data class SpotifyTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int
)
