package com.example.musicplayer

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUrl: String = "",
    val audioUrl: String = ""
)
