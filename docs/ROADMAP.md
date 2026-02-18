# Astute Podcasts - Development Roadmap

A full-featured Android podcast player built incrementally across six phases. Each phase produces a functional, runnable app.

## Phase 1: Project Scaffolding & Static UI Shell **[COMPLETE]**

Establish the Android project from scratch with a full Gradle build system, Material 3 theming with dynamic color, and all five app screens rendered with hardcoded placeholder data.

**What gets built:**
- Gradle build system with Kotlin DSL and a version catalog (`libs.versions.toml`) managing all dependency versions in one place
- Single-activity architecture with Jetpack Compose as the UI framework
- Material 3 theme that uses Android 12+ dynamic color (wallpaper-based theming) with a static fallback for older devices
- Bottom navigation bar with three tabs: Home, Search, Downloads
- Five screens with placeholder content and full navigation wiring between them
- Three reusable UI components: `PodcastCard`, `EpisodeListItem`, `MiniPlayer`
- Domain model data classes: `Podcast`, `Episode`, `DownloadStatus`, `PlaybackState`
- Placeholder `PlaybackService` declared in the manifest for future use

**How to verify:** Open the project in Android Studio, run on an emulator (API 26+). All three tabs work, tapping a podcast navigates to the detail screen, and the Now Playing screen is reachable.

---

## Phase 2: Podcast Index API & Search **[COMPLETE]**

Replace placeholder data on the Search and Podcast Detail screens with real results from the Podcast Index API.

**What gets built:**
- `PodcastIndexAuthInterceptor` - an OkHttp interceptor that computes the SHA-1 authorization hash from the API key, secret, and current Unix timestamp, and adds the required headers to every request
- `NetworkModule` (Hilt) providing OkHttp, Retrofit, and the `PodcastIndexApi` Retrofit service as singletons
- Remote DTOs (`SearchResponse`, `PodcastDto`, `EpisodeDto`, `TrendingResponse`) annotated with `@Serializable` for Kotlinx Serialization
- `PodcastIndexApi` Retrofit interface with endpoints: `searchByTerm`, `getEpisodesByFeedId`, `getTrending`
- Mapper functions converting DTOs to domain models
- `PodcastRepository` and `EpisodeRepository` (interfaces in `domain/`, implementations in `data/`)
- `SearchViewModel` with 500ms debounced search input, loading/error states, and trending podcasts when the query is empty
- `PodcastDetailViewModel` loading podcast metadata and its episode list

**Prerequisites:** Register at [podcastindex.org](https://api.podcastindex.org/) and add credentials to `local.properties`:
```properties
PODCAST_INDEX_API_KEY=your_key_here
PODCAST_INDEX_API_SECRET=your_secret_here
```

**How to verify:** Type a podcast name in the Search screen and see real results. Tap a result to see its real episode list.

---

## Phase 3: Local Database & Subscriptions **[COMPLETE]**

Add a Room database so users can subscribe to podcasts and see them on the Home screen, persisted across app restarts.

**What gets built:**
- Room entities: `PodcastEntity`, `EpisodeEntity`, `SubscriptionEntity` with foreign keys and indices
- DAOs with Flow-returning queries for reactive UI updates
- `PodcastDatabase` (Room database class) and `DatabaseModule` (Hilt)
- `SubscriptionRepository` handling subscribe/unsubscribe operations, caching episode metadata locally on subscribe, and exposing subscriptions as a Flow
- `HomeViewModel` observing subscriptions and recent episodes from Room
- Subscribe/unsubscribe toggle button on the Podcast Detail screen
- Feed refresh logic that fetches the latest episodes for subscribed podcasts when the Home screen opens

**How to verify:** Subscribe to a podcast from the detail screen. Kill the app. Reopen it. The Home screen shows the subscribed podcast and its episodes.

---

## Phase 4: Audio Playback **[COMPLETE]**

Implement full audio playback with a background service, media notification, lock screen controls, and the Now Playing UI.

**What gets built:**
- `PlaybackService` extending `MediaSessionService` with an ExoPlayer instance configured for speech audio, audio focus handling, and headphone-unplug pause
- `PlaybackServiceConnection` managing the `MediaController` lifecycle (connecting UI to the background service)
- `PlaybackManager` exposing a clean API (`play`, `togglePlayPause`, `seekTo`, `skipForward`, `skipBackward`, `setPlaybackSpeed`) and a `Flow<PlaybackState>` that emits position/duration/playing status every 500ms
- `NowPlayingViewModel` collecting playback state
- Full `NowPlayingScreen`: seek bar, skip -10s / +30s, play/pause, playback speed selector (0.5x to 2x)
- `MiniPlayer` wired into the main Scaffold, visible whenever audio is active, with play/pause and tap-to-expand
- Play buttons on episode list items that trigger playback
- Playback speed preference persisted via DataStore

**How to verify:** Tap play on an episode. Audio plays. Minimize the app. Audio continues. A media notification appears with controls. Lock screen shows controls. Speed changes work.

---

## Phase 5: Episode Downloads **[COMPLETE]**

Allow downloading episodes for offline playback, with progress tracking and download management.

**What gets built:**
- `DownloadEntity` and `DownloadDao` in Room, tracking status (queued, downloading, downloaded, failed), progress percentage, and local file path
- `EpisodeDownloadWorker` (a `@HiltWorker` CoroutineWorker) that streams the audio file via OkHttp, writes it to app-specific external storage (`getExternalFilesDir`), and updates Room with progress as it downloads
- `DownloadRepository` orchestrating WorkManager: enqueuing downloads, cancelling them, deleting files, querying status as Flows
- `DownloadsViewModel` and a fully wired `DownloadsScreen` showing download progress bars, completed downloads, and storage usage
- `DownloadButton` component with visual states: idle, queued, downloading (circular progress), completed (checkmark), failed (retry)
- `PlaybackManager` updated to use the local file path when an episode has been downloaded, falling back to the streaming URL otherwise
- Cancel and delete support (kills WorkManager job, removes partial files)

**How to verify:** Tap download on an episode. See a progress indicator. Once complete, enable airplane mode. Play the episode. Audio plays from local storage.

---

## Phase 6: Polish & Production Readiness **[COMPLETE]**

Harden the app for real-world use with error handling, UX improvements, and release preparation.

**What gets built:**
- Network error handling throughout with user-facing retry actions
- Empty states with helpful messages and calls to action
- Offline detection (show cached content gracefully when offline)
- Pull-to-refresh on the Home screen to update episode feeds
- Playback position persistence: save position when pausing/switching episodes, restore on resume
- "Continue listening" section on the Home screen for partially-played episodes
- Loading shimmer/skeleton effects on search results and episode lists
- `POST_NOTIFICATIONS` runtime permission request dialog (required on Android 13+)
- ProGuard/R8 rules for Retrofit, Kotlinx Serialization, and Room
- Proper app icon (replace placeholder vector) and splash screen via the Splash Screen API
- Accessibility: content descriptions, minimum touch targets, screen reader support
- Dark mode testing and edge case fixes

**How to verify:** Use the app end-to-end: search, subscribe, play, download, go offline, come back online. No crashes, clear error messages, smooth transitions.

---

## Post-Phase Enhancements **[COMPLETE]**

Additional features and improvements built after the core phases.

**What was built:**
- **Direct RSS feed parsing** - Episode refresh switched from Podcast Index API to parsing RSS feeds directly via `RssFeedService` and `RssFeedParser`, providing more reliable and up-to-date episode data
- **Native HTML descriptions** - Podcast and episode descriptions rendered natively using `AnnotatedString.fromHtml()` instead of plain text stripping
- **Archive played episodes** - Episodes auto-archive when playback finishes, hiding them from the podcast detail feed and home screen. Manual archive/unarchive via icon buttons. "Show archived" filter chip toggle on the podcast detail screen. Archiving cleans up associated downloads. Database migration (v2→v3) adds `isArchived` column
