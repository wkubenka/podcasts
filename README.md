# Astute Podcasts

A native Android podcast player built with Kotlin and Jetpack Compose. Search and discover podcasts, subscribe to your favorites, stream or download episodes for offline listening, and enjoy background playback with full media controls.

## Features

- **Discover** - Search podcasts and browse trending shows via the [Podcast Index](https://podcastindex.org/) open directory
- **Subscribe** - Build your library and stay up to date with new episodes via direct RSS feed parsing
- **Listen** - Background audio playback with notification and lock screen controls, adjustable playback speed (0.5x-2x), skip forward/back, and playback position persistence
- **Download** - Save episodes for offline listening with auto-download on play, progress tracking, and download management
- **Archive** - Finished episodes auto-archive to keep your feed focused on unplayed content, with manual archive/unarchive and a toggle to show hidden episodes
- **Material You** - Dynamic color theming on Android 12+ with full dark mode support

## Screenshots

*Coming soon*

## Tech Stack

| | |
|---|---|
| **Language** | Kotlin 2.1 |
| **UI** | Jetpack Compose + Material Design 3 |
| **Architecture** | Single-activity MVVM with clean architecture layers |
| **DI** | Hilt |
| **Database** | Room |
| **Audio** | Media3 / ExoPlayer |
| **Networking** | Retrofit + OkHttp |
| **API** | [Podcast Index](https://api.podcastindex.org/) + direct RSS feed parsing |
| **Images** | Coil |
| **Downloads** | WorkManager |

**Min SDK**: 26 (Android 8.0) &nbsp;|&nbsp; **Target SDK**: 35 (Android 15)

## Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable)
- Android SDK 35 (downloaded automatically on first sync)
- A [Podcast Index](https://api.podcastindex.org/) API key and secret (free)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/astute-podcasts.git
   cd astute-podcasts
   ```

2. Add your Podcast Index API credentials to `local.properties`:
   ```properties
   PODCAST_INDEX_API_KEY=your_key_here
   PODCAST_INDEX_API_SECRET=your_secret_here
   ```

3. Open the project in Android Studio and let Gradle sync.

4. Run the app on an emulator or connected device:
   ```bash
   ./gradlew installDebug
   ```

## Project Structure

```
app/src/main/java/com/astutepodcasts/app/
├── domain/          Pure Kotlin models and repository interfaces
├── data/
│   ├── local/       Room database, DAOs, entities
│   ├── remote/      Retrofit API, DTOs, auth interceptor
│   ├── repository/  Repository implementations
│   └── worker/      WorkManager download workers
├── ui/
│   ├── home/        Subscribed podcasts and recent episodes
│   ├── search/      Podcast search and trending
│   ├── podcastdetail/  Podcast info and episode list
│   ├── nowplaying/  Full player with seek, speed, and skip controls
│   ├── downloads/   Download management
│   ├── components/  Shared composables (PodcastCard, EpisodeListItem, MiniPlayer)
│   ├── navigation/  Routes and bottom navigation
│   └── theme/       Material 3 dynamic color theme
├── playback/        Media3 service, controller, and playback manager
└── di/              Hilt dependency injection modules
```

## Architecture

The app follows a clean architecture pattern with unidirectional data flow:

```
UI (Compose)  ──observes──>  ViewModel (StateFlow)
                             ViewModel  ──calls──>  Repository (interface)
                                                    Repository  ──uses──>  Room + Retrofit
```

Audio playback runs in a `MediaSessionService` with ExoPlayer, providing background audio, media notifications, and lock screen controls. The playback layer exposes a `Flow<PlaybackState>` that the UI observes reactively.

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full technical design.

## Documentation

| Document | Description |
|----------|-------------|
| [docs/ROADMAP.md](docs/ROADMAP.md) | Development phases and what each delivers |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Technical architecture, data flow, and package structure |
| [docs/CURRENT_STATUS.md](docs/CURRENT_STATUS.md) | What's implemented and what's remaining |

## License

*TBD*
