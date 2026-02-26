package com.astute.podcasts.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.astute.podcasts.data.local.PlaybackPreferences
import com.astute.podcasts.data.local.dao.EpisodeDao
import com.astute.podcasts.domain.model.DownloadStatus
import com.astute.podcasts.domain.repository.DownloadRepository
import com.astute.podcasts.testutil.MainDispatcherRule
import com.astute.podcasts.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PlaybackManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkObject(EpisodeMediaItemMapper)
        every { EpisodeMediaItemMapper.toMediaItem(any()) } returns mockk()
    }

    @After
    fun tearDown() {
        unmockkObject(EpisodeMediaItemMapper)
    }

    private val controllerFlow = MutableStateFlow<MediaController?>(null)

    private val connection: PlaybackServiceConnection = mockk(relaxed = true) {
        every { controller } returns controllerFlow
    }

    private val playbackPreferences: PlaybackPreferences = mockk(relaxed = true) {
        every { playbackSpeed } returns flowOf(1.0f)
    }

    private val downloadRepository: DownloadRepository = mockk(relaxed = true)
    private val episodeDao: EpisodeDao = mockk(relaxed = true)

    private fun createManager() = PlaybackManager(
        connection, playbackPreferences, downloadRepository, episodeDao
    )

    private fun createMockController(isPlaying: Boolean = true, position: Long = 45_000L): MediaController {
        return mockk(relaxed = true) {
            every { this@mockk.isPlaying } returns isPlaying
            every { currentPosition } returns position
            every { duration } returns 3_600_000L
        }
    }

    /**
     * Creates a controller that captures the Player.Listener for callback testing.
     */
    private fun createListenerCapturingController(
        isPlaying: Boolean = true,
        position: Long = 45_000L
    ): Pair<MediaController, () -> Player.Listener> {
        val listenerSlot = slot<Player.Listener>()
        val controller: MediaController = mockk(relaxed = true) {
            every { addListener(capture(listenerSlot)) } returns Unit
            every { this@mockk.isPlaying } returns isPlaying
            every { currentPosition } returns position
            every { duration } returns 3_600_000L
        }
        return controller to { listenerSlot.captured }
    }

    // ── Download swap ────────────────────────────────────────────────

    @Test
    fun `download swap resumes playback when episode was playing`() {
        val downloadFlow = MutableStateFlow<String?>(null)
        val episode = TestData.episode(downloadStatus = DownloadStatus.NOT_DOWNLOADED)
        val controller = createMockController(isPlaying = true, position = 45_000L)
        controllerFlow.value = controller

        coEvery { downloadRepository.observeEpisodeDownloaded(episode.id) } returns downloadFlow

        val manager = createManager()
        manager.play(episode)

        // Simulate download completing while playing
        downloadFlow.value = "/data/local/episode.mp3"

        // play() called once from play(), and once from the download swap
        verify(exactly = 2) { controller.play() }
    }

    @Test
    fun `download swap does not resume playback when episode was paused`() {
        val downloadFlow = MutableStateFlow<String?>(null)
        val episode = TestData.episode(downloadStatus = DownloadStatus.NOT_DOWNLOADED)

        // Controller starts playing, then is paused before download completes
        val controller: MediaController = mockk(relaxed = true) {
            every { currentPosition } returns 45_000L
            every { duration } returns 3_600_000L
            // isPlaying will be false when the download swap checks it
            every { isPlaying } returns false
        }
        controllerFlow.value = controller

        coEvery { downloadRepository.observeEpisodeDownloaded(episode.id) } returns downloadFlow

        val manager = createManager()
        manager.play(episode)

        // Simulate download completing while paused
        downloadFlow.value = "/data/local/episode.mp3"

        // play() called once from play(), but NOT from the download swap
        verify(exactly = 1) { controller.play() }
    }

    @Test
    fun `download swap preserves playback position`() {
        val downloadFlow = MutableStateFlow<String?>(null)
        val episode = TestData.episode(downloadStatus = DownloadStatus.DOWNLOADING)
        val controller = createMockController(isPlaying = false, position = 120_000L)
        controllerFlow.value = controller

        coEvery { downloadRepository.observeEpisodeDownloaded(episode.id) } returns downloadFlow

        val manager = createManager()
        manager.play(episode)

        downloadFlow.value = "/data/local/episode.mp3"

        verify { controller.setMediaItem(any(), eq(120_000L)) }
    }

    @Test
    fun `download swap updates episode in playback state`() {
        val downloadFlow = MutableStateFlow<String?>(null)
        val episode = TestData.episode(downloadStatus = DownloadStatus.NOT_DOWNLOADED)
        val controller = createMockController(isPlaying = true, position = 45_000L)
        controllerFlow.value = controller

        coEvery { downloadRepository.observeEpisodeDownloaded(episode.id) } returns downloadFlow

        val manager = createManager()
        manager.play(episode)

        downloadFlow.value = "/data/local/episode.mp3"

        val currentEpisode = manager.playbackState.value.currentEpisode
        assertEquals(DownloadStatus.DOWNLOADED, currentEpisode?.downloadStatus)
        assertEquals("/data/local/episode.mp3", currentEpisode?.localFilePath)
    }

    @Test
    fun `download swap ignored when different episode is playing`() {
        val downloadFlow = MutableStateFlow<String?>(null)
        val episode1 = TestData.episode(id = 10L, downloadStatus = DownloadStatus.NOT_DOWNLOADED)
        val episode2 = TestData.episode(id = 20L, downloadStatus = DownloadStatus.DOWNLOADED)
        val controller = createMockController(isPlaying = true, position = 0L)
        controllerFlow.value = controller

        coEvery { downloadRepository.observeEpisodeDownloaded(episode1.id) } returns downloadFlow

        val manager = createManager()
        manager.play(episode1)

        // Switch to a different episode before download completes
        manager.play(episode2)

        // Now episode1's download completes
        downloadFlow.value = "/data/local/episode1.mp3"

        // State should reflect episode2, not episode1
        assertEquals(20L, manager.playbackState.value.currentEpisode?.id)
    }

    // ── Resume playback position ─────────────────────────────────────

    @Test
    fun `play resumes from saved position`() {
        val episode = TestData.episode(lastPlayedPositionMs = 60_000L)
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(episode)

        verify { controller.setMediaItem(any(), eq(60_000L)) }
    }

    @Test
    fun `play starts from beginning when no saved position`() {
        val episode = TestData.episode(lastPlayedPositionMs = 0L)
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(episode)

        verify { controller.setMediaItem(any(), eq(0L)) }
    }

    @Test
    fun `play clamps negative saved position to zero`() {
        val episode = TestData.episode(lastPlayedPositionMs = -1L)
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(episode)

        verify { controller.setMediaItem(any(), eq(0L)) }
    }

    @Test
    fun `play calls prepare and play after setting media item`() {
        val episode = TestData.episode()
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(episode)

        verify { controller.prepare() }
        verify { controller.play() }
    }

    // ── Download trigger behavior ────────────────────────────────────

    @Test
    fun `play triggers download for NOT_DOWNLOADED episode`() {
        val episode = TestData.episode(downloadStatus = DownloadStatus.NOT_DOWNLOADED)
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(episode)

        coVerify { downloadRepository.downloadEpisode(episode) }
    }

    @Test
    fun `play triggers download for FAILED episode`() {
        val episode = TestData.episode(downloadStatus = DownloadStatus.FAILED)
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(episode)

        coVerify { downloadRepository.downloadEpisode(episode) }
    }

    @Test
    fun `play does not trigger download for DOWNLOADED episode`() {
        val episode = TestData.episode(downloadStatus = DownloadStatus.DOWNLOADED)
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(episode)

        coVerify(exactly = 0) { downloadRepository.downloadEpisode(any()) }
    }

    @Test
    fun `play observes download for DOWNLOADING episode without triggering new download`() {
        val episode = TestData.episode(downloadStatus = DownloadStatus.DOWNLOADING)
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(episode)

        coVerify(exactly = 0) { downloadRepository.downloadEpisode(any()) }
        coVerify { downloadRepository.observeEpisodeDownloaded(episode.id) }
    }

    @Test
    fun `play observes download for QUEUED episode without triggering new download`() {
        val episode = TestData.episode(downloadStatus = DownloadStatus.QUEUED)
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(episode)

        coVerify(exactly = 0) { downloadRepository.downloadEpisode(any()) }
        coVerify { downloadRepository.observeEpisodeDownloaded(episode.id) }
    }

    // ── Episode finished lifecycle ───────────────────────────────────

    @Test
    fun `episode finished clears playback state`() {
        val (controller, getListener) = createListenerCapturingController()
        controllerFlow.value = controller

        val episode = TestData.episode(downloadStatus = DownloadStatus.DOWNLOADED)
        val manager = createManager()
        manager.play(episode)

        getListener().onPlaybackStateChanged(Player.STATE_ENDED)

        val state = manager.playbackState.value
        assertNull(state.currentEpisode)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `episode finished preserves playback speed in reset state`() {
        val (controller, getListener) = createListenerCapturingController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.setPlaybackSpeed(1.5f)
        manager.play(TestData.episode(downloadStatus = DownloadStatus.DOWNLOADED))

        getListener().onPlaybackStateChanged(Player.STATE_ENDED)

        assertEquals(1.5f, manager.playbackState.value.playbackSpeed)
    }

    @Test
    fun `episode finished stops player and clears media items`() {
        val (controller, getListener) = createListenerCapturingController()
        controllerFlow.value = controller

        val episode = TestData.episode(downloadStatus = DownloadStatus.DOWNLOADED)
        val manager = createManager()
        manager.play(episode)

        getListener().onPlaybackStateChanged(Player.STATE_ENDED)

        verify { controller.stop() }
        verify { controller.clearMediaItems() }
    }

    @Test
    fun `episode finished auto-archives episode`() {
        val (controller, getListener) = createListenerCapturingController()
        controllerFlow.value = controller

        val episode = TestData.episode(id = 42L, downloadStatus = DownloadStatus.DOWNLOADED)
        val manager = createManager()
        manager.play(episode)

        getListener().onPlaybackStateChanged(Player.STATE_ENDED)

        Thread.sleep(200)
        coVerify { episodeDao.updateArchived(42L, true) }
    }

    @Test
    fun `episode finished clears saved position`() {
        val (controller, getListener) = createListenerCapturingController()
        controllerFlow.value = controller

        val episode = TestData.episode(id = 42L, downloadStatus = DownloadStatus.DOWNLOADED)
        val manager = createManager()
        manager.play(episode)

        getListener().onPlaybackStateChanged(Player.STATE_ENDED)

        Thread.sleep(200)
        coVerify { episodeDao.updatePlaybackPosition(42L, 0, 0) }
    }

    @Test
    fun `episode finished deletes download for downloaded episode`() {
        val (controller, getListener) = createListenerCapturingController()
        controllerFlow.value = controller

        val episode = TestData.episode(id = 42L, downloadStatus = DownloadStatus.DOWNLOADED)
        val manager = createManager()
        manager.play(episode)

        getListener().onPlaybackStateChanged(Player.STATE_ENDED)

        Thread.sleep(200)
        coVerify { downloadRepository.deleteDownload(42L) }
    }

    @Test
    fun `episode finished does not delete for NOT_DOWNLOADED episode`() {
        val (controller, getListener) = createListenerCapturingController()
        controllerFlow.value = controller

        val episode = TestData.episode(id = 42L, downloadStatus = DownloadStatus.NOT_DOWNLOADED)
        val manager = createManager()
        manager.play(episode)

        getListener().onPlaybackStateChanged(Player.STATE_ENDED)

        Thread.sleep(200)
        coVerify(exactly = 0) { downloadRepository.deleteDownload(any()) }
    }

    // ── Player listener state updates ────────────────────────────────

    @Test
    fun `isPlaying state updated from player callback`() {
        val (controller, getListener) = createListenerCapturingController()
        controllerFlow.value = controller

        val manager = createManager()

        getListener().onIsPlayingChanged(true)
        assertTrue(manager.playbackState.value.isPlaying)

        getListener().onIsPlayingChanged(false)
        assertFalse(manager.playbackState.value.isPlaying)
    }

    @Test
    fun `buffering state updated from player callback`() {
        val (controller, getListener) = createListenerCapturingController()
        controllerFlow.value = controller

        val manager = createManager()

        getListener().onPlaybackStateChanged(Player.STATE_BUFFERING)
        assertTrue(manager.playbackState.value.isBuffering)

        getListener().onPlaybackStateChanged(Player.STATE_READY)
        assertFalse(manager.playbackState.value.isBuffering)
    }

    @Test
    fun `null media item transition clears episode and playing state`() {
        val (controller, getListener) = createListenerCapturingController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(TestData.episode())

        getListener().onMediaItemTransition(null, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED)

        assertNull(manager.playbackState.value.currentEpisode)
        assertFalse(manager.playbackState.value.isPlaying)
    }

    // ── Position save ────────────────────────────────────────────────

    @Test
    fun `position saved when playback pauses`() {
        val (controller, getListener) = createListenerCapturingController(position = 90_000L)
        controllerFlow.value = controller

        val episode = TestData.episode(id = 55L)
        val manager = createManager()
        manager.play(episode)

        getListener().onIsPlayingChanged(false)

        Thread.sleep(200)
        coVerify { episodeDao.updatePlaybackPosition(55L, 90_000L, any()) }
    }

    @Test
    fun `play saves previous episode position before switching`() {
        val controller = createMockController(position = 120_000L)
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(TestData.episode(id = 10L))

        // Switch to different episode
        manager.play(TestData.episode(id = 20L))

        Thread.sleep(200)
        coVerify { episodeDao.updatePlaybackPosition(10L, 120_000L, any()) }
    }

    // ── Toggle play/pause ────────────────────────────────────────────

    @Test
    fun `togglePlayPause pauses when playing`() {
        val controller = createMockController(isPlaying = true)
        controllerFlow.value = controller

        val manager = createManager()
        manager.togglePlayPause()

        verify { controller.pause() }
    }

    @Test
    fun `togglePlayPause plays when paused`() {
        val controller = createMockController(isPlaying = false)
        controllerFlow.value = controller

        val manager = createManager()
        manager.togglePlayPause()

        verify { controller.play() }
    }

    @Test
    fun `togglePlayPause does nothing when no controller`() {
        // controllerFlow.value is null by default
        val manager = createManager()
        manager.togglePlayPause()
        // No exception thrown
    }

    // ── Seek ─────────────────────────────────────────────────────────

    @Test
    fun `seekTo delegates to controller`() {
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.seekTo(90_000L)

        verify { controller.seekTo(90_000L) }
    }

    @Test
    fun `skipBackward seeks back 10 seconds`() {
        val controller = createMockController(position = 45_000L)
        controllerFlow.value = controller

        val manager = createManager()
        manager.skipBackward()

        verify { controller.seekTo(35_000L) }
    }

    @Test
    fun `skipBackward clamps to zero`() {
        val controller = createMockController(position = 5_000L)
        controllerFlow.value = controller

        val manager = createManager()
        manager.skipBackward()

        verify { controller.seekTo(0L) }
    }

    @Test
    fun `skipForward seeks forward 30 seconds`() {
        val controller = createMockController(position = 45_000L)
        controllerFlow.value = controller

        val manager = createManager()
        manager.skipForward()

        verify { controller.seekTo(75_000L) }
    }

    @Test
    fun `skipForward clamps to duration`() {
        val controller: MediaController = mockk(relaxed = true) {
            every { isPlaying } returns true
            every { currentPosition } returns 3_590_000L
            every { duration } returns 3_600_000L
        }
        controllerFlow.value = controller

        val manager = createManager()
        manager.skipForward()

        verify { controller.seekTo(3_600_000L) }
    }

    // ── Playback speed ───────────────────────────────────────────────

    @Test
    fun `initial playback speed loaded from preferences`() {
        every { playbackPreferences.playbackSpeed } returns flowOf(1.75f)

        val manager = createManager()

        assertEquals(1.75f, manager.playbackState.value.playbackSpeed)
    }

    @Test
    fun `setPlaybackSpeed updates state`() {
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.setPlaybackSpeed(1.5f)

        assertEquals(1.5f, manager.playbackState.value.playbackSpeed)
    }

    @Test
    fun `setPlaybackSpeed persists to preferences`() {
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.setPlaybackSpeed(2.0f)

        coVerify { playbackPreferences.setPlaybackSpeed(2.0f) }
    }

    @Test
    fun `setPlaybackSpeed applies to controller`() {
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.setPlaybackSpeed(1.5f)

        verify { controller.playbackParameters = any() }
    }

    @Test
    fun `play applies current playback speed to controller`() {
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.setPlaybackSpeed(1.5f)
        manager.play(TestData.episode())

        // playbackParameters set from both setPlaybackSpeed and play
        verify(atLeast = 2) { controller.playbackParameters = any() }
    }

    // ── Play sets up state correctly ─────────────────────────────────

    @Test
    fun `play sets current episode in state`() {
        val controller = createMockController()
        controllerFlow.value = controller

        val episode = TestData.episode(id = 99L, title = "My Episode")
        val manager = createManager()
        manager.play(episode)

        assertEquals(episode, manager.playbackState.value.currentEpisode)
    }

    @Test
    fun `play sets buffering state`() {
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(TestData.episode())

        assertTrue(manager.playbackState.value.isBuffering)
    }

    @Test
    fun `play connects to service`() {
        val controller = createMockController()
        controllerFlow.value = controller

        val manager = createManager()
        manager.play(TestData.episode())

        verify { connection.connect() }
    }
}