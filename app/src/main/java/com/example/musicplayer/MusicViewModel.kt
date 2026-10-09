package com.example.musicplayer

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class Section(val title: String, val songs: List<Song>, val isArtist: Boolean = false)

private class SectionDef(val title: String, val term: String, val isArtist: Boolean)

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    // Home rows. Edit this list to add or change artists.
    // (iTunes allows roughly 20 requests per minute, so keep this list under about 10.)
    private val sectionDefs = listOf(
        SectionDef("Taylor Swift", "Taylor Swift", true),
        SectionDef("The Weeknd", "The Weeknd", true),
        SectionDef("Ed Sheeran", "Ed Sheeran", true),
        SectionDef("Billie Eilish", "Billie Eilish", true),
        SectionDef("Dua Lipa", "Dua Lipa", true),
        SectionDef("Coldplay", "Coldplay", true),
        SectionDef("Harry Styles", "Harry Styles", true),
        SectionDef("Bruno Mars", "Bruno Mars", true)
    )

    var sections by mutableStateOf<List<Section>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var searchResults by mutableStateOf<List<Song>>(emptyList())
        private set
    var searching by mutableStateOf(false)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set
    var currentSong by mutableStateOf<Song?>(null)
        private set
    var position by mutableStateOf(0L)
        private set
    var duration by mutableStateOf(0L)
        private set

    private var queue: List<Song> = emptyList()
    private var controller: MediaController? = null
    private var searchJob: Job? = null

    init {
        loadSongs()
        connectToService()
        startProgressUpdates()
    }

    // Refreshes the playback position twice a second for the progress bar.
    private fun startProgressUpdates() {
        viewModelScope.launch {
            while (true) {
                controller?.let { c ->
                    position = c.currentPosition.coerceAtLeast(0L)
                    val d = c.duration
                    duration = if (d > 0) d else 0L
                }
                delay(500)
            }
        }
    }

    private suspend fun fetch(
        term: String,
        limit: Int = 15,
        artistFilter: String? = null
    ): List<Song> {
        val response = Api.service.search(term = term, limit = limit)
        val all = response.results
            .filter { !it.previewUrl.isNullOrBlank() }
            .map {
                Song(
                    id = it.trackId,
                    title = it.trackName ?: "Unknown",
                    artist = it.artistName ?: "Unknown",
                    artworkUrl = it.artworkUrl100?.replace("100x100", "600x600") ?: "",
                    audioUrl = it.previewUrl!!
                )
            }
            .distinctBy { it.id }
        if (artistFilter == null) return all
        val only = all.filter { it.artist.contains(artistFilter, ignoreCase = true) }
        return if (only.isNotEmpty()) only else all
    }

    private fun mixSongs(lists: List<List<Song>>, max: Int): List<Song> {
        val out = mutableListOf<Song>()
        var i = 0
        while (out.size < max && lists.any { i < it.size }) {
            for (l in lists) {
                if (i < l.size && out.size < max) out.add(l[i])
            }
            i++
        }
        return out.distinctBy { it.id }
    }

    fun loadSongs() {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val result = sectionDefs
                    .map { def ->
                        async {
                            try {
                                val songs = fetch(
                                    term = def.term,
                                    limit = if (def.isArtist) 30 else 20,
                                    artistFilter = if (def.isArtist) def.term else null
                                ).take(15)
                                Section(def.title, songs, def.isArtist)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Section(def.title, emptyList(), def.isArtist)
                            }
                        }
                    }
                    .awaitAll()
                    .filter { it.songs.isNotEmpty() }
                // "Trending Now" = a mix of one song from each artist in turn
                val trending = mixSongs(result.map { it.songs }, 20)
                sections = if (trending.isEmpty()) result
                else listOf(Section("Trending Now", trending, false)) + result
                if (result.isEmpty()) {
                    error = "Check your internet connection, or wait a minute and try again."
                }
            } catch (e: Exception) {
                error = e.message ?: "Check your internet connection"
            }
            loading = false
        }
    }

    // Called on every keystroke: waits for a short pause, then searches.
    fun onQueryChange(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.length < 2) {
            searchResults = emptyList()
            searchError = null
            searching = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(450)
            runSearch(q)
        }
    }

    // Called when the user taps the keyboard search key, a genre, or an artist.
    fun search(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.isEmpty()) {
            searchResults = emptyList()
            searchError = null
            searching = false
            return
        }
        searchJob = viewModelScope.launch { runSearch(q) }
    }

    private suspend fun runSearch(q: String) {
        searching = true
        searchError = null
        try {
            val r = fetch(q, 30)
            searchResults = r
            if (r.isEmpty()) searchError = "No results for \"$q\""
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            searchError = e.message ?: "Check your internet connection"
        }
        searching = false
    }

    private fun connectToService() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            isPlaying = c.isPlaying
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    currentSong = queue.find { it.id.toString() == item?.mediaId } ?: currentSong
                }
            })
        }, ContextCompat.getMainExecutor(context))
    }

    fun play(list: List<Song>, index: Int) {
        val c = controller ?: return
        if (index !in list.indices) return
        queue = list
        val items = list.map { s ->
            MediaItem.Builder()
                .setMediaId(s.id.toString())
                .setUri(s.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setArtworkUri(Uri.parse(s.artworkUrl))
                        .build()
                )
                .build()
        }
        currentSong = list[index]
        c.setMediaItems(items, index, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() { controller?.seekToNextMediaItem() }

    fun previous() { controller?.seekToPreviousMediaItem() }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        position = ms
    }

    override fun onCleared() {
        controller?.release()
        super.onCleared()
    }
}
