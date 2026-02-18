# Astute Podcasts - Current Status

**All 6 phases complete. Post-phase enhancements delivered.**

The app is fully functional end-to-end: search, subscribe, play, download, archive, and offline playback all work.

## What's Done

### Phase 1: Project Scaffolding & Static UI Shell

- Gradle build system with Kotlin DSL and version catalog (`libs.versions.toml`)
- Single-activity Compose architecture with Material 3 dynamic color theming
- Bottom navigation (Home, Search, Downloads) with five screen routes
- Reusable components: `PodcastCard`, `EpisodeListItem`, `MiniPlayer`
- Domain models: `Podcast`, `Episode`, `DownloadStatus`, `PlaybackState`

### Phase 2: Podcast Index API & Search

- `PodcastIndexAuthInterceptor` computing per-request SHA-1 auth headers
- `NetworkModule` providing OkHttp, Retrofit, and `PodcastIndexApi` via Hilt
- Remote DTOs with Kotlinx Serialization (`SearchResponse`, `PodcastDto`, `EpisodeDto`, `TrendingResponse`)
- `PodcastRepository` and `EpisodeRepository` (interface + implementation)
- `SearchViewModel` with 500ms debounced search and trending podcasts
- `PodcastDetailViewModel` loading podcast metadata and episode lists

### Phase 3: Local Database & Subscriptions

- Room database (`PodcastDatabase`) with entities: `PodcastEntity`, `EpisodeEntity`, `SubscriptionEntity`
- DAOs with Flow-returning queries for reactive UI updates
- `SubscriptionRepository` for subscribe/unsubscribe with local episode caching
- `HomeViewModel` observing subscriptions and recent episodes from Room
- Subscribe/unsubscribe toggle on Podcast Detail screen
- Feed refresh on Home screen open

### Phase 4: Audio Playback

- `PlaybackService` extending `MediaSessionService` with ExoPlayer (speech audio, audio focus, headphone-unplug pause)
- `PlaybackServiceConnection` managing `MediaController` lifecycle
- `PlaybackManager` with clean API (`play`, `togglePlayPause`, `seekTo`, `skipForward`, `skipBackward`, `setPlaybackSpeed`) and `Flow<PlaybackState>` emitting every 500ms
- Full `NowPlayingScreen` with seek bar, skip controls, speed selector (0.5x-2x)
- `MiniPlayer` wired to live playback state, visible when audio is active
- Playback speed preference persisted via DataStore

### Phase 5: Episode Downloads

- `EpisodeDownloadWorker` (`@HiltWorker` CoroutineWorker) streaming audio to app-specific storage
- `DownloadRepository` orchestrating WorkManager with progress tracking
- `DownloadsScreen` and `DownloadsViewModel` showing progress, completed downloads, and storage usage
- `DownloadButton` component with visual states (idle, queued, downloading, completed, failed)
- Auto-download on play: episodes begin downloading when playback starts, seamlessly swapping to local file on completion
- Cancel and delete support

### Phase 6: Polish & Production Readiness

- Network error handling with user-facing retry actions throughout
- Empty states with helpful messages and calls to action
- Loading skeleton/shimmer effects on search results and episode lists
- Playback position persistence: saves on pause/switch, restores on resume
- "Continue listening" section on Home for partially-played episodes
- `POST_NOTIFICATIONS` runtime permission request (Android 13+)
- ProGuard/R8 rules for Retrofit, Kotlinx Serialization, and Room

### Post-Phase Enhancements

- **Direct RSS feed parsing** - Episode refresh uses `RssFeedService` + `RssFeedParser` instead of Podcast Index API, providing more reliable and up-to-date episode data
- **Native HTML descriptions** - Podcast/episode descriptions rendered with `AnnotatedString.fromHtml()` via the `HtmlText` composable
- **Archive played episodes** - Auto-archive on playback completion, manual archive/unarchive buttons, "Show archived" filter chip on podcast detail, archived episodes hidden from home feed and recently played, download cleanup on archive. Room database at version 3 (`isArchived` column added in migration 2→3).

## Database Schema

Room database version 3 with three tables:

| Table | Key columns |
|-------|-------------|
| `podcasts` | id, title, author, description, artworkUrl, feedUrl |
| `episodes` | id, podcastId (FK), title, audioUrl, downloadStatus, localFilePath, lastPlayedPositionMs, lastPlayedAt, isArchived |
| `subscriptions` | podcastId (FK), subscribedAt |

## Environment Requirements

- **Android Studio** (latest stable)
- **JDK 17** - bundled with Android Studio
- **Android SDK 35** - downloaded automatically by Android Studio on first sync
- **Podcast Index API credentials** - register at [podcastindex.org](https://api.podcastindex.org/) and add to `local.properties`
