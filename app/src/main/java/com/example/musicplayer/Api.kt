package com.example.musicplayer

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class ItunesResponse(val results: List<ItunesTrack>)

data class ItunesTrack(
    val trackId: Long,
    val trackName: String?,
    val artistName: String?,
    val artworkUrl100: String?,
    val previewUrl: String?
)

interface ItunesService {
    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 25
    ): ItunesResponse
}

object Api {
    val service: ItunesService by lazy {
        Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ItunesService::class.java)
    }
}
