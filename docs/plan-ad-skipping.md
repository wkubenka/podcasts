# Plan: Automatic Ad Skipping

## Problem Statement

Implement automatic ad skipping in the Astute Podcasts player. Key constraints:

- **Ad locations are unknown** — no pre-existing timestamp data from podcast feeds
- **Ads are often host-read** — the same voice delivers both content and ads, so simple voice-fingerprinting won't work
- **Runs on Android** — processing budget, battery, and memory are limited

## Current Architecture Summary

- Playback via **Media3 / ExoPlayer** (`PlaybackService`, `PlaybackManager`)
- Reactive state via `StateFlow<PlaybackState>` (position polled every 500ms)
- Episodes can be **streamed or downloaded** (local file path available post-download)
- No audio analysis libraries currently included
- ExoPlayer configured with `AUDIO_CONTENT_TYPE_SPEECH`
- Position polling and persistence already in place

---

## Options

### Option A: Crowdsourced Timestamp Database (SponsorBlock Model)

**How it works:**
Users manually mark ad start/end timestamps. These are submitted to a shared database. Other listeners of the same episode automatically skip those segments.

**Architecture:**
1. Add a backend service (or use an existing one) to store `(episodeGuid, startMs, endMs, category)` tuples
2. On episode play, fetch known ad segments for that episode
3. During playback, auto-seek past segments when `currentPositionMs` enters an ad range
4. Add UI for users to mark/report ad segments (long-press on progress bar, "mark ad start/end")

**Integration points:**
- `PlaybackManager` position polling (already 500ms) — check against ad segments and `seekTo()` past them
- New `AdSegment` domain model and Room table for caching
- New network layer to sync segments with the backend
- UI overlay on `NowPlayingScreen` progress bar showing ad regions

**Pros:**
- Most proven approach (SponsorBlock has millions of users on YouTube)
- 100% accurate once segments are submitted — works perfectly for host-read ads
- Zero on-device processing cost
- Works for both streaming and downloaded episodes

**Cons:**
- Requires critical mass of users to be useful (cold start problem)
- Needs a backend service to host and serve segment data
- First listener of any episode gets no benefit
- Relies on user effort to mark segments
- Potential legal/ToS concerns with podcast creators and ad networks

**Complexity:** Medium (backend + client changes)

---

### Option B: On-Device ML Audio Classification

**How it works:**
Run a trained ML model (TensorFlow Lite / ONNX) on the device that classifies short audio windows (e.g., 5-10 second chunks) as "content" or "ad."

**Architecture:**
1. Add TensorFlow Lite dependency
2. Extract audio features (mel spectrograms, MFCCs) from the ExoPlayer audio pipeline
3. Feed features into a classifier model at regular intervals
4. When consecutive windows are classified as "ad," trigger a skip

**Integration points:**
- Custom `AudioProcessor` attached to ExoPlayer's audio pipeline for real-time feature extraction
- Background coroutine running inference on extracted features
- New `AdDetectionManager` coordinating with `PlaybackManager`
- Model file bundled in `assets/` or downloaded on first launch

**Pros:**
- Fully offline — no network dependency, no backend
- Works for the first listener of any episode
- Can improve over time with better models

**Cons:**
- Host-read ads are extremely hard to distinguish from content via audio features alone — the voice, cadence, and recording environment are identical
- Requires training data (thousands of labeled ad segments across many podcasts)
- Battery and CPU cost on mobile
- Model accuracy is likely to be low for host-read ads specifically (this is the core problem)
- Large model size impacts APK size
- Latency between detection and skip creates jarring UX

**Complexity:** High (ML pipeline, training data, audio processing)
**Feasibility for host-read ads:** Low

---

### Option C: Speech-to-Text + NLP Keyword/Semantic Classification

**How it works:**
Transcribe the audio (on-device or via API), then analyze the text to detect ad-specific language patterns like "brought to you by," "use code," "check out," discount URLs, etc.

**Sub-options:**

#### C1: On-Device STT + Rule-Based Detection
- Use Android's on-device speech recognition or Whisper (via whisper.cpp / ONNX)
- Apply regex/keyword rules to detect ad language
- Low cost per episode, fully offline

#### C2: On-Device STT + Local NLP Classifier
- Transcribe on-device
- Run a small text classifier (fine-tuned DistilBERT or similar) on transcript segments
- Better at catching varied ad language than pure regex

#### C3: Cloud STT + LLM Classification
- Send audio to a cloud STT service (Google, Whisper API, Deepgram)
- Send transcript segments to an LLM to classify as ad vs. content
- Most accurate, but expensive and requires network

**Architecture (common):**
1. Pre-process episodes (during download or as a background job)
2. Chunk audio into ~30-60 second windows
3. Transcribe each chunk
4. Classify transcript text as ad or content
5. Store detected ad segment timestamps in Room
6. During playback, auto-skip stored segments (same as Option A's playback logic)

**Integration points:**
- New `TranscriptionWorker` (WorkManager) for background processing
- New `AdClassifier` component (rule-based, ML, or API-based)
- `AdSegmentDao` to cache results per episode
- `PlaybackManager` reads cached segments and skips during playback

**Pros:**
- **Best approach for host-read ads** — ad language has distinctive patterns regardless of voice
- Can work ahead of time (pre-process downloaded episodes)
- Rule-based version (C1) is simple to implement and iterate on
- Semantic analysis catches ads that audio-only analysis misses

**Cons:**
- On-device STT is CPU/battery intensive (Whisper small model: ~5-15 min to transcribe a 1-hr episode on modern phones)
- Cloud STT has per-minute costs ($0.006-$0.01/min typical)
- Real-time transcription during streaming is difficult — better suited to downloaded episodes
- Keyword rules need maintenance as ad language evolves
- False positives when hosts discuss sponsors in actual content context
- Transcript errors degrade classification accuracy

**Complexity:** Medium-High
**Feasibility for host-read ads:** High (the strongest option for this constraint)

---

### Option D: Silence & Acoustic Transition Detection

**How it works:**
Many podcasts insert brief silence, jingles, or music beds before and after ad reads. Detect these acoustic boundaries to identify ad segments.

**Architecture:**
1. Analyze audio amplitude/energy levels to find silence gaps (< -40dB for > 0.5s)
2. Detect music/jingle segments via spectral analysis
3. Use segment boundaries to hypothesize ad regions
4. Optionally combine with duration heuristics (most ad reads are 30-90 seconds)

**Integration points:**
- Custom `AudioProcessor` in ExoPlayer pipeline for amplitude monitoring
- `SilenceDetector` analyzing audio frames
- Heuristic engine to group silence-bounded segments

**Pros:**
- Lightweight computation
- Works in real-time during streaming
- No transcription needed

**Cons:**
- Many host-read ads have no acoustic boundary at all — the host transitions seamlessly from content to ad and back
- High false-positive rate (natural pauses, topic transitions)
- Completely fails for the specific constraint of seamless host-read ads
- Only useful as a supplementary signal, not standalone

**Complexity:** Low-Medium
**Feasibility for host-read ads:** Very Low (most don't have acoustic boundaries)

---

### Option E: Hybrid Approach

**How it works:**
Combine multiple signals to maximize accuracy:

1. **Crowdsourced data (Option A)** as the primary source when available
2. **STT + NLP classification (Option C)** for episodes without crowdsourced data
3. **Silence detection (Option D)** as a supplementary boundary-finding signal to improve NLP segment boundaries

**Architecture:**
1. On episode play, check for crowdsourced segments first
2. If none exist and episode is downloaded, run background transcription + classification
3. Use silence detection to refine segment boundaries
4. Cache all results locally
5. Allow users to confirm/correct detected segments (feeds back into crowdsourced DB)

**Integration points:**
- All integration points from Options A, C, and D
- `AdSkipOrchestrator` that prioritizes data sources
- Feedback loop: user corrections improve the system for everyone

**Pros:**
- Best overall accuracy
- Graceful degradation (always has some signal)
- Users improve the system over time
- Works for both new and popular episodes

**Cons:**
- Highest implementation complexity
- Largest dependency footprint
- Most code to maintain
- Longer development timeline

**Complexity:** High
**Feasibility for host-read ads:** High

---

### Option F: Third-Party Ad Detection API

**How it works:**
Use an existing third-party service that specializes in podcast ad detection. Services in this space include Podkite, Podsights analysis, or newer ML-based APIs.

**Architecture:**
1. Send episode audio URL or file to a third-party API
2. Receive back timestamped ad segments
3. Cache results and skip during playback

**Pros:**
- Offloads the hard ML/NLP problem to specialists
- Potentially high accuracy
- Minimal client-side complexity

**Cons:**
- Ongoing API costs per episode
- Privacy concerns (sending listening data to third party)
- Dependency on third-party availability and pricing
- Limited control over accuracy and behavior
- Very few public APIs exist for this specific use case today

**Complexity:** Low (client-side) / dependency on third-party
**Feasibility for host-read ads:** Depends on the service

---

## Comparison Matrix

| Criteria                        | A: Crowdsourced | B: Audio ML | C: STT + NLP | D: Silence | E: Hybrid | F: 3rd Party API |
|---------------------------------|:-:|:-:|:-:|:-:|:-:|:-:|
| Host-read ad detection          | High* | Low | **High** | Very Low | **High** | Variable |
| Works without network           | No | Yes | C1/C2: Yes | Yes | Partial | No |
| Works on first listen           | No | Yes | Yes | Yes | Yes | Yes |
| Battery/CPU impact              | None | High | Medium-High | Low | Medium | None |
| Implementation complexity       | Medium | High | Medium | Low | High | Low |
| Ongoing cost                    | Backend hosting | None | C3: API costs | None | Backend + optional API | Per-episode API |
| Cold start problem              | Yes | No | No | No | Mitigated | No |
| Accuracy for host-read ads      | 100%* | ~30-50% | ~70-85% | ~20-30% | ~80-90% | ~60-80% |

*When segments are available from other users

---

## Recommendation

Given the constraint that **ads are host-read** (same voice as content), the options split into two tiers:

### Tier 1 — Viable for host-read ads:
- **Option C (STT + NLP)** — the only approach that attacks the problem where host-read ads are actually distinguishable: their *language*, not their *sound*
- **Option A (Crowdsourced)** — perfect accuracy when data exists, but dependent on user base
- **Option E (Hybrid)** — combines both, best long-term solution

### Tier 2 — Poor fit for host-read ads:
- **Option B (Audio ML)** — host-read ads sound identical to content
- **Option D (Silence detection)** — host-read ads often have no acoustic boundary
- **Option F (Third-party API)** — limited availability, variable quality

### Suggested phased approach:

**Phase 1 — Start with C1 (On-Device STT + Rule-Based Detection):**
- Add Whisper (via `whisper.cpp` Android bindings) for on-device transcription of downloaded episodes
- Implement keyword/pattern-based ad detection on transcripts
- Build the skip infrastructure in `PlaybackManager` (reusable across all approaches)
- Build UI for showing detected ad segments on the progress bar and allowing user corrections
- This gives a working, fully offline solution with no backend dependency

**Phase 2 — Add Crowdsourced Layer (Option A):**
- Let users submit corrected/confirmed ad segments to a shared backend
- Fetch crowdsourced segments on episode play (preferred over local detection when available)
- This solves the accuracy gap in the NLP approach

**Phase 3 — Refine with Hybrid Signals (Option E):**
- Add silence detection to improve segment boundary precision
- Upgrade text classifier from rules to a small ML model if needed
- Add confidence scores and user-facing controls (sensitivity slider, auto-skip vs. notify)

---

## Key Files That Would Change

| Component | Files |
|-----------|-------|
| Playback skip logic | `PlaybackManager.kt` |
| Ad segment model | New: `domain/model/AdSegment.kt` |
| Ad segment persistence | New: `data/local/entity/AdSegmentEntity.kt`, `data/local/dao/AdSegmentDao.kt` |
| Database migration | `AppDatabase.kt` |
| Transcription worker | New: `data/worker/TranscriptionWorker.kt` |
| Ad detection logic | New: `adskip/AdDetector.kt`, `adskip/KeywordRules.kt` |
| Skip orchestrator | New: `adskip/AdSkipManager.kt` |
| Now Playing UI | `NowPlayingScreen.kt` (segment markers on progress bar, skip controls) |
| Settings | New ad-skip preferences (enable/disable, sensitivity) |
| Dependencies | `libs.versions.toml`, `build.gradle.kts` (Whisper, optional TFLite) |
