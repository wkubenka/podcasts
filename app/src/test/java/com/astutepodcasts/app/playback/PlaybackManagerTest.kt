package com.astutepodcasts.app.playback

import androidx.media3.common.Player
import com.astutepodcasts.app.data.local.PlaybackPreferences
import com.astutepodcasts.app.data.local.dao.EpisodeDao
import com.astutepodcasts.app.domain.model.DownloadStatus
import com.astutepodcasts.app.domain.repository.DownloadRepository
import com.astutepodcasts.app.testutil.MainDispatcherRule
import com.astutepodcasts.app.testutil.TestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val controllerFlow = MutableStateFlow<Player?>(null)

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

    private fun createMockController(isPlaying: Boolean = true, position: Long = 45_000L): Player {
        return mockk(relaxed = true) {
            every { this@mockk.isPlaying } returns isPlaying
            every { currentPosition } returns position
            every { duration } returns 3_600_000L
        }
    }

    @Test
    fun `download swap resumes playback when episode was playing`() = runTest {
        val downloadFlow = MutableStateFlow<String?>(null)
        val episode = TestData.episode(downloadStatus = DownloadStatus.NOT_DOWNLOADED)
        val controller = createMockController(isPlaying = true, position = 45_000L)
        controllerFlow.value = controller

        coEvery { downloadRepository.observeEpisodeDownloaded(episode.id) } returns downloadFlow

        val manager = createManager()
        manager.play(episode)
        advanceUntilIdle()

        // Simulate download completing while playing
        downloadFlow.value = "/data/local/episode.mp3"
        advanceUntilIdle()

        // play() called once from play(), and once from the download swap
        verify(exactly = 2) { controller.play() }
    }

    @Test
    fun `download swap does not resume playback when episode was paused`() = runTest {
        val downloadFlow = MutableStateFlow<String?>(null)
        val episode = TestData.episode(downloadStatus = DownloadStatus.NOT_DOWNLOADED)

        // Controller starts playing, then is paused before download completes
        val controller: Player = mockk(relaxed = true) {
            every { currentPosition } returns 45_000L
            every { duration } returns 3_600_000L
            // isPlaying will be false when the download swap checks it
            every { isPlaying } returns false
        }
        controllerFlow.value = controller

        coEvery { downloadRepository.observeEpisodeDownloaded(episode.id) } returns downloadFlow

        val manager = createManager()
        manager.play(episode)
        advanceUntilIdle()

        // Simulate download completing while paused
        downloadFlow.value = "/data/local/episode.mp3"
        advanceUntilIdle()

        // play() called once from play(), but NOT from the download swap
        verify(exactly = 1) { controller.play() }
    }

    @Test
    fun `download swap preserves playback position`() = runTest {
        val downloadFlow = MutableStateFlow<String?>(null)
        val episode = TestData.episode(downloadStatus = DownloadStatus.DOWNLOADING)
        val controller = createMockController(isPlaying = false, position = 120_000L)
        controllerFlow.value = controller

        coEvery { downloadRepository.observeEpisodeDownloaded(episode.id) } returns downloadFlow

        val manager = createManager()
        manager.play(episode)
        advanceUntilIdle()

        downloadFlow.value = "/data/local/episode.mp3"
        advanceUntilIdle()

        verify { controller.setMediaItem(any(), eq(120_000L)) }
    }
}
