# Astute Podcasts - Current Status

**Phase 1 complete. Phases 2-6 not started.**

## What's Done

Phase 1 delivers a fully navigable UI prototype with Material 3 theming and placeholder data. All screens are built and wired together. The project is ready to open in Android Studio, build, and run.

### Build System (complete)

All Gradle configuration is in place with Kotlin DSL:

- `settings.gradle.kts` - project named "AstutePodcasts", includes `:app` module
- `build.gradle.kts` (root) - declares all plugins without applying them
- `app/build.gradle.kts` - app module config with compileSdk 35, minSdk 26, all dependencies, BuildConfig fields for Podcast Index API credentials
- `gradle/libs.versions.toml` - version catalog managing 20+ library versions
- `gradle/wrapper/gradle-wrapper.properties` - Gradle 8.11.1
- `gradle.properties` - JVM args, AndroidX, non-transitive R class

Dependencies declared but not yet used in code: Room, Retrofit, OkHttp, Kotlinx Serialization, WorkManager, DataStore, Media3. These are ready for Phases 2-5.

### App Entry Points (complete)

- `PodcastApp.kt` - `@HiltAndroidApp` application class
- `MainActivity.kt` - `@AndroidEntryPoint` single activity with edge-to-edge, sets Compose content with `AstutePodcastsTheme` wrapping `AppNavGraph`
- `AndroidManifest.xml` - declares activity, `PlaybackService` (foreground media), and permissions (INTERNET, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, POST_NOTIFICATIONS)

### Theme (complete)

Material 3 with dynamic color support:

- `Theme.kt` - `AstutePodcastsTheme` composable that uses `dynamicLightColorScheme`/`dynamicDarkColorScheme` on Android 12+ and falls back to a static purple scheme on older devices
- `Color.kt` - static fallback color tokens (Purple40/80, PurpleGrey, Pink)
- `Type.kt` - typography scale from `headlineLarge` down to `labelSmall`

### Navigation (complete)

- `Screen.kt` - sealed class with five routes: `Home`, `Search`, `Downloads`, `PodcastDetail` (parameterized with `podcastId: Long`), `NowPlaying`
- `NavGraph.kt` - `AppNavGraph` composable with:
  - `Scaffold` containing a `NavigationBar` (bottom bar) with three tabs
  - `NavHost` with all five routes wired up
  - Bottom bar hidden on detail/player screens
  - Placeholder `MiniPlayer` (hidden by default, wired for Phase 4)

### Screens (complete, placeholder data)

All screens render with hardcoded sample data. No ViewModels yet (those come in Phases 2-3).

| Screen | File | Placeholder Content |
|--------|------|-------------------|
| Home | `ui/home/HomeScreen.kt` | Horizontal row of 4 podcast cards, vertical list of 4 recent episodes, empty state composable |
| Search | `ui/search/SearchScreen.kt` | Material 3 `SearchBar`, list of 5 trending podcasts as `SearchResultItem` rows |
| Podcast Detail | `ui/podcastdetail/PodcastDetailScreen.kt` | Header (artwork, title, author, description, subscribe button), list of 5 episodes |
| Now Playing | `ui/nowplaying/NowPlayingScreen.kt` | Large artwork, episode/podcast title, seek slider, skip -10s/+30s buttons, play/pause, speed selector cycling 0.5x-2x |
| Downloads | `ui/downloads/DownloadsScreen.kt` | 3 sample downloads (2 complete, 1 in progress), storage usage display, empty state composable |

### Shared Components (complete)

| Component | File | Description |
|-----------|------|-------------|
| `PodcastCard` | `ui/components/PodcastCard.kt` | 120dp-wide card with artwork (Coil `AsyncImage`), title. Used in Home's horizontal row. |
| `EpisodeListItem` | `ui/components/EpisodeListItem.kt` | Row with artwork thumbnail, title, date/duration metadata, download button, play button. Used in Home and Podcast Detail. |
| `MiniPlayer` | `ui/components/MiniPlayer.kt` | Compact bar with progress indicator, artwork, episode/podcast title, play/pause. Intended to sit above the bottom nav bar. |

### Domain Models (complete)

| Model | File | Fields |
|-------|------|--------|
| `Podcast` | `domain/model/Podcast.kt` | id, title, author, description, artworkUrl, feedUrl, language, episodeCount, lastUpdateTime |
| `Episode` | `domain/model/Episode.kt` | id, podcastId, title, description, audioUrl, artworkUrl, publishedAt, durationSeconds, fileSize, episodeNumber, seasonNumber, downloadStatus, localFilePath |
| `DownloadStatus` | `domain/model/DownloadStatus.kt` | Enum: NOT_DOWNLOADED, QUEUED, DOWNLOADING, DOWNLOADED, FAILED |
| `PlaybackState` | `domain/model/PlaybackState.kt` | currentEpisode, isPlaying, currentPositionMs, durationMs, playbackSpeed, isBuffering |

### Other Files

- `playback/PlaybackService.kt` - Skeleton `MediaSessionService` (no ExoPlayer initialization yet, just the manifest-required class)
- `app/proguard-rules.pro` - R8 keep rules for Kotlinx Serialization and Retrofit
- `.gitignore` - ignores build artifacts, `.idea/`, `local.properties`, `.DS_Store`
- App icon resources - vector drawable launcher icons (purple circle with play triangle)

## What's Not Done Yet

| Item | Phase |
|------|-------|
| Podcast Index API integration | 2 |
| ViewModels for Search and Podcast Detail | 2 |
| Room database (entities, DAOs, database class) | 3 |
| Subscription persistence | 3 |
| HomeViewModel with real data | 3 |
| ExoPlayer initialization in PlaybackService | 4 |
| PlaybackServiceConnection and PlaybackManager | 4 |
| NowPlayingViewModel with real playback state | 4 |
| MiniPlayer wired to actual playback | 4 |
| EpisodeDownloadWorker | 5 |
| Download repository and management | 5 |
| DownloadsViewModel with real data | 5 |
| Error handling, loading states, shimmer effects | 6 |
| Playback position persistence | 6 |
| Runtime permission requests | 6 |
| Proper app icon and splash screen | 6 |

## Environment Requirements

- **Android Studio** (latest stable) - required for building; not currently installed on the dev machine
- **JDK 17** - bundled with Android Studio
- **Android SDK 35** - downloaded automatically by Android Studio on first sync
- **Podcast Index API credentials** - needed before starting Phase 2; register at [podcastindex.org](https://api.podcastindex.org/)
