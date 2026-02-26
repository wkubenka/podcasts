# Astute Podcasts - Architecture

## Overview

Astute Podcasts is a native Android app built with Kotlin and Jetpack Compose. It follows a single-activity, single-module clean architecture with unidirectional data flow.

## Technology Stack

| Category | Technology | Purpose |
|----------|-----------|---------|
| Language | Kotlin 2.1 | Primary language, targeting JVM 17 |
| UI | Jetpack Compose + Material 3 | Declarative UI with dynamic color theming |
| Navigation | Navigation Compose | Single-activity screen-to-screen routing |
| DI | Hilt (Dagger) | Compile-time dependency injection |
| Database | Room | Local SQLite for subscriptions, episodes, downloads |
| Audio | Media3 (ExoPlayer + MediaSession) | Background playback, notifications, lock screen |
| Networking | Retrofit + OkHttp | HTTP client for Podcast Index API and RSS feeds |
| Serialization | Kotlinx Serialization | JSON parsing for API responses |
| Images | Coil | Async image loading with Compose integration |
| Background work | WorkManager | Reliable episode downloads |
| Preferences | DataStore | Key-value settings (playback speed, etc.) |

## Build Configuration

- **Min SDK**: 26 (Android 8.0) - covers 95%+ of active devices
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35
- **Gradle**: Kotlin DSL with a version catalog (`gradle/libs.versions.toml`)
- **Annotation processing**: KSP (for Hilt, Room)

## Package Structure

All source code lives under `com.astute.podcasts` in a single `app` module:

```
com.astute.podcasts/
├── PodcastApp.kt              Hilt application entry point
├── MainActivity.kt            Single activity, hosts Compose content
│
├── domain/                    Pure Kotlin, no Android dependencies
│   ├── model/                 Data classes: Podcast, Episode, PlaybackState, DownloadStatus
│   └── repository/            Repository interfaces (contracts)
│
├── data/                      Android-dependent implementations
│   ├── local/                 Room database, DAOs, entities, type converters
│   ├── remote/                Retrofit API service, DTOs, auth interceptor, RSS feed parser
│   ├── repository/            Repository implementations (orchestrate local + remote)
│   ├── mapper/                Conversion functions between DTO, entity, and domain model
│   └── worker/                WorkManager workers (episode downloads)
│
├── ui/                        Compose UI layer
│   ├── theme/                 Material 3 theme, colors, typography
│   ├── navigation/            Route definitions, NavGraph, bottom navigation bar
│   ├── components/            Reusable composables (PodcastCard, EpisodeListItem, MiniPlayer)
│   ├── home/                  Home screen + ViewModel
│   ├── search/                Search screen + ViewModel
│   ├── podcastdetail/         Podcast detail screen + ViewModel
│   ├── nowplaying/            Now Playing screen + ViewModel
│   └── downloads/             Downloads screen + ViewModel
│
├── playback/                  Audio playback layer
│   ├── PlaybackService.kt    MediaSessionService owning ExoPlayer
│   ├── PlaybackServiceConnection.kt  MediaController lifecycle
│   └── PlaybackManager.kt    Clean API for ViewModels, exposes Flow<PlaybackState>
│
└── di/                        Hilt modules
    ├── NetworkModule.kt       OkHttp, Retrofit, API service
    ├── DatabaseModule.kt      Room database, DAOs
    ├── RepositoryModule.kt    Binds repository interfaces to implementations
    └── PlaybackModule.kt     PlaybackServiceConnection, PlaybackManager
```

## Data Flow

The app uses **unidirectional data flow** (UDF). State flows downward, events flow upward:

```
Screen ──observes──> ViewModel.uiState (StateFlow)
Screen ──calls─────> ViewModel.onAction(event)
ViewModel ──calls──> Repository (domain interface)
ViewModel ──calls──> PlaybackManager (for media)
```

### Example: Searching for podcasts

1. User types in `SearchScreen`
2. `SearchScreen` calls `SearchViewModel.search(query)`
3. `SearchViewModel` calls `PodcastRepository.searchPodcasts(query)` (domain interface)
4. `PodcastRepositoryImpl` calls `PodcastIndexApi.searchByTerm(query)` (Retrofit), maps DTOs to domain models
5. `SearchViewModel` updates `_uiState: MutableStateFlow<SearchUiState>`
6. `SearchScreen` recomposes with new results

### Example: Playing an episode

1. User taps play on an `EpisodeListItem`
2. ViewModel calls `PlaybackManager.play(episode)`
3. `PlaybackManager` builds a `MediaItem` and sends it to `PlaybackService` via `MediaController`
4. `PlaybackService` plays audio via ExoPlayer as a foreground service
5. Media notification and lock screen controls appear automatically
6. `PlaybackManager` emits `Flow<PlaybackState>` (position, duration, isPlaying)
7. `NowPlayingViewModel` collects this flow, updates UI state
8. Both `MiniPlayer` and `NowPlayingScreen` recompose

## Navigation

Single-activity with Compose Navigation. The app uses a `Scaffold` with a persistent `BottomNavBar` and `MiniPlayer`:

```
BottomNavBar tabs:
  ├── Home         ──tap podcast──> PodcastDetail ──tap play──> NowPlaying
  ├── Search       ──tap result──>  PodcastDetail
  └── Downloads    ──tap episode──> NowPlaying

MiniPlayer: persistent bar above BottomNavBar when audio is active
NowPlaying: full-screen overlay / route
```

Routes are defined as a sealed class in `Screen.kt`:
- `home` - Home / Library tab
- `search` - Search tab
- `downloads` - Downloads tab
- `podcast/{podcastId}` - Podcast detail (parameterized)
- `now_playing` - Full-screen player

## API Authentication (Podcast Index)

Every request to `https://api.podcastindex.org/api/1.0/` requires these headers, computed per-request by an OkHttp interceptor:

| Header | Value |
|--------|-------|
| `User-Agent` | `AstutePodcasts/1.0` |
| `X-Auth-Key` | API key from `BuildConfig` |
| `X-Auth-Date` | Current Unix timestamp (seconds) |
| `Authorization` | SHA-1 hex digest of `apiKey + apiSecret + timestamp` |

API credentials are stored in `local.properties` (git-ignored) and injected at build time via `BuildConfig` fields.

## Episode Lifecycle

Episodes follow a lifecycle from discovery through archival:

1. **Discovery** - Episodes are fetched by parsing RSS feeds directly (`RssFeedService` + `RssFeedParser`), with podcast search/trending via the Podcast Index API
2. **Subscription** - On subscribe, episode metadata is cached locally in Room. The upsert query preserves download status and archive state across refreshes.
3. **Playback** - Playing an episode auto-downloads it. `PlaybackManager` saves position every ~10s and on pause. Position is restored on resume.
4. **Completion** - When an episode finishes, `PlaybackManager.onEpisodeFinished()` clears playback position, deletes the download, and auto-archives the episode
5. **Archival** - Archived episodes are hidden from the home feed, recently played, and the podcast detail episode list (unless "Show archived" is toggled). Users can manually archive/unarchive. Archiving cleans up associated downloads.

## Offline Strategy

- **Subscriptions and metadata**: Cached in Room on subscribe. Available offline.
- **Episode audio**: Downloaded via WorkManager to `getExternalFilesDir("downloads/")`. No storage permission required. Cleaned up on app uninstall.
- **Playback**: `PlaybackManager` checks for a local file path before streaming. Downloaded episodes play without network.
- **Download resilience**: WorkManager survives process death, respects network constraints, retries on failure with exponential backoff.
