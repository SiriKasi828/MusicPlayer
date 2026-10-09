# Music Player App

A Spotify-style music player for Android, built with **Kotlin** and **Jetpack Compose** for the Android Development recruitment task.

## Level completed

**Level 3**: UI and navigation, audio playback, public API integration, and background playback.

## Features

**Home**
- Time-based greeting ("Good morning / afternoon / evening")
- Featured song banner and a grid of quick picks
- "Popular artists" row with round artist photos
- "Best of ..." rows for Taylor Swift, The Weeknd, Ed Sheeran, Billie Eilish, Dua Lipa, Coldplay, Harry Styles and Bruno Mars
- "Trending Now" row that mixes songs from all of these artists
- Loading and error states with a Retry button

**Search**
- Live suggestions while typing (waits for a short pause, then searches)
- Artists row that only shows artists matching the typed text, with their songs listed first
- Genre tiles (Pop, Rock, Hip-Hop, Dance, Chill, Indie, R&B, Country) when the box is empty
- Clear (X) button

**Now Playing**
- Large cover art, song name and artist
- Play / Pause, Next and Previous buttons
- Seek bar with elapsed time and remaining time (drag to jump)
- Back button to return

**Playback**
- Mini player bar above the bottom tabs; tap it to open Now Playing
- Background playback using a Media3 `MediaSessionService`: music keeps playing when the app is minimized or the screen is off
- Notification and lock-screen controls
- Songs in a row or search result play as a queue (Next / Previous move through it)

## Screenshots

Add your screenshots to a `screenshots` folder in the repo and link them here:

```
![Home](screenshots/home.png)
![Search](screenshots/search.png)
![Now Playing](screenshots/now_playing.png)
```

## Tech stack

| Area | Library |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Navigation | Navigation Compose (bottom tabs + player screen) |
| Audio | Media3 ExoPlayer, MediaSession, MediaSessionService |
| Networking | Retrofit + Gson |
| Images | Coil |
| State | ViewModel + Compose state, Kotlin coroutines |

## API

Songs, artists, artwork and audio previews come from the public **iTunes Search API**
(`https://itunes.apple.com/search`). No API key is needed.

Notes:
- The API provides **30-second preview clips**, so each track plays for up to 30 seconds.
- The API allows roughly 20 requests per minute. If Home shows an error after many quick reloads, wait a minute and tap Retry.

## How to run

1. Clone the repo: `git clone https://github.com/SiriKasi828/MusicPlayer.git`
2. Open the project in **Android Studio** and let Gradle sync.
3. Run on an emulator or an Android phone (Min SDK 24, internet required).
