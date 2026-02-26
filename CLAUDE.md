# Astute Podcasts

Android podcast player built with Kotlin, Jetpack Compose, and Material 3.

## Build & Run

```bash
# Build debug APK (requires Android Studio / Android SDK)
./gradlew assembleDebug

# Install on connected device or emulator
./gradlew installDebug
```

Requires Android Studio with SDK 35 installed. Open the project root in Android Studio and let it sync Gradle automatically.

## Project Structure

Single-module Android app at `app/`. All source code under `com.astute.podcasts`:

- `domain/model/` - pure Kotlin data classes (Podcast, Episode, DownloadStatus, PlaybackState)
- `domain/repository/` - repository interfaces
- `data/local/` - Room database, DAOs, entities
- `data/remote/` - Retrofit API service, DTOs, auth interceptor
- `data/repository/` - repository implementations
- `data/mapper/` - DTO/entity/domain model mappers
- `data/worker/` - WorkManager workers
- `ui/` - Compose screens organized by feature (home, search, podcastdetail, nowplaying, downloads)
- `ui/components/` - reusable composables (PodcastCard, EpisodeListItem, MiniPlayer)
- `ui/navigation/` - route definitions and NavGraph
- `ui/theme/` - Material 3 theme, colors, typography
- `playback/` - MediaSessionService, MediaController connection, PlaybackManager
- `di/` - Hilt modules

## Key Conventions

- **Architecture**: MVVM with clean architecture layers (domain, data, ui). Unidirectional data flow. ViewModels expose `StateFlow`, screens observe and call event methods.
- **DI**: Hilt throughout. `@AndroidEntryPoint` on Activity/Service, `@HiltViewModel` on ViewModels, `@HiltWorker` on WorkManager workers. Modules in `di/`.
- **Navigation**: Compose Navigation with a sealed `Screen` class defining routes. Single `NavHost` in `NavGraph.kt`.
- **Serialization**: Kotlinx Serialization (not Gson). DTOs use `@Serializable` and `@SerialName`.
- **Database**: Room with KSP. Entities in `data/local/entity/`, DAOs return `Flow<>` for reactive queries.
- **Async**: Kotlin coroutines + Flow. No RxJava.
- **Image loading**: Coil with `AsyncImage` composable.
- **Build**: Kotlin DSL Gradle files. All dependency versions in `gradle/libs.versions.toml`.

## API Credentials

Podcast Index API credentials go in `local.properties` (git-ignored):

```properties
PODCAST_INDEX_API_KEY=your_key
PODCAST_INDEX_API_SECRET=your_secret
```

These are injected via `BuildConfig` at compile time. Register at https://api.podcastindex.org/.

## Documentation

- `docs/ROADMAP.md` - development phases and what each delivers
- `docs/ARCHITECTURE.md` - technical architecture, data flow, package structure
- `docs/CURRENT_STATUS.md` - what's implemented and what's remaining
