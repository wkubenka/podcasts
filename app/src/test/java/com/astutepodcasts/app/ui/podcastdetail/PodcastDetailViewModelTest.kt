package com.astutepodcasts.app.ui.podcastdetail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.astutepodcasts.app.domain.model.DownloadStatus
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.repository.EpisodeRepository
import com.astutepodcasts.app.domain.repository.PodcastRepository
import com.astutepodcasts.app.domain.repository.SubscriptionRepository
import com.astutepodcasts.app.testutil.MainDispatcherRule
import com.astutepodcasts.app.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val podcastId = 42L
    private val savedStateHandle = SavedStateHandle(mapOf("podcastId" to podcastId))

    private val isSubscribedFlow = MutableStateFlow(false)
    private val observeEpisodesFlow = MutableStateFlow<List<Episode>>(emptyList())

    private val podcastRepository: PodcastRepository = mockk()
    private val episodeRepository: EpisodeRepository = mockk {
        every { observeEpisodesForPodcast(podcastId) } returns observeEpisodesFlow
        coEvery { setArchived(any(), any()) } returns Unit
    }
    private val subscriptionRepository: SubscriptionRepository = mockk {
        every { isSubscribed(podcastId) } returns isSubscribedFlow
        coEvery { subscribe(any(), any()) } returns Unit
        coEvery { unsubscribe(any()) } returns Unit
    }

    private fun createViewModel(
        podcast: Any? = TestData.podcast(id = podcastId),
        apiEpisodes: List<Episode> = listOf(
            TestData.episode(id = 1, podcastId = podcastId, title = "Ep 1"),
            TestData.episode(id = 2, podcastId = podcastId, title = "Ep 2")
        ),
        localEpisodes: List<Episode> = emptyList()
    ): PodcastDetailViewModel {
        coEvery { podcastRepository.getPodcastById(podcastId) } returns (podcast as? com.astutepodcasts.app.domain.model.Podcast)
        coEvery { episodeRepository.getEpisodesForPodcast(podcastId) } returns apiEpisodes
        coEvery { episodeRepository.getLocalEpisodesForPodcast(podcastId) } returns localEpisodes
        return PodcastDetailViewModel(
            savedStateHandle,
            podcastRepository,
            episodeRepository,
            subscriptionRepository
        )
    }

    // --- Loading ---

    @Test
    fun `initial load sets podcast and episodes`() = runTest {
        val podcast = TestData.podcast(id = podcastId, title = "My Podcast")
        val episodes = listOf(
            TestData.episode(id = 1, podcastId = podcastId, title = "Ep 1"),
            TestData.episode(id = 2, podcastId = podcastId, title = "Ep 2")
        )

        val vm = createViewModel(podcast = podcast, apiEpisodes = episodes)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(podcast, state.podcast)
        assertEquals(2, state.episodes.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `load merges api episodes with local download status`() = runTest {
        val apiEpisodes = listOf(
            TestData.episode(id = 1, podcastId = podcastId, downloadStatus = DownloadStatus.NOT_DOWNLOADED),
            TestData.episode(id = 2, podcastId = podcastId, downloadStatus = DownloadStatus.NOT_DOWNLOADED)
        )
        val localEpisodes = listOf(
            TestData.episode(id = 1, podcastId = podcastId, downloadStatus = DownloadStatus.DOWNLOADED, localFilePath = "/data/ep1.mp3")
        )

        val vm = createViewModel(apiEpisodes = apiEpisodes, localEpisodes = localEpisodes)
        advanceUntilIdle()

        val state = vm.uiState.value
        val ep1 = state.episodes.find { it.id == 1L }!!
        val ep2 = state.episodes.find { it.id == 2L }!!
        assertEquals(DownloadStatus.DOWNLOADED, ep1.downloadStatus)
        assertEquals("/data/ep1.mp3", ep1.localFilePath)
        assertEquals(DownloadStatus.NOT_DOWNLOADED, ep2.downloadStatus)
    }

    @Test
    fun `load preserves playback progress from local episodes`() = runTest {
        val apiEpisodes = listOf(
            TestData.episode(
                id = 1, podcastId = podcastId,
                title = "Fresh Title",
                lastPlayedPositionMs = 0,
                lastPlayedAt = 0
            )
        )
        val localEpisodes = listOf(
            TestData.episode(
                id = 1, podcastId = podcastId,
                title = "Stale Title",
                lastPlayedPositionMs = 45000,
                lastPlayedAt = 999L,
                isArchived = true,
                downloadStatus = DownloadStatus.DOWNLOADED,
                localFilePath = "/data/ep1.mp3"
            )
        )

        val vm = createViewModel(apiEpisodes = apiEpisodes, localEpisodes = localEpisodes)
        advanceUntilIdle()

        val ep = vm.uiState.value.episodes.single()
        // Local state preserved
        assertEquals(45000L, ep.lastPlayedPositionMs)
        assertEquals(999L, ep.lastPlayedAt)
        assertTrue(ep.isArchived)
        assertEquals(DownloadStatus.DOWNLOADED, ep.downloadStatus)
        assertEquals("/data/ep1.mp3", ep.localFilePath)
        // API metadata overlaid
        assertEquals("Fresh Title", ep.title)
    }

    @Test
    fun `load overlays api metadata onto local episodes`() = runTest {
        val apiEpisodes = listOf(
            TestData.episode(
                id = 1, podcastId = podcastId,
                title = "Updated Title",
                description = "Updated description",
                audioUrl = "https://new.com/audio.mp3",
                artworkUrl = "https://new.com/art.jpg",
                publishedAt = 2000L,
                durationSeconds = 7200,
                fileSize = 100_000_000L,
                episodeNumber = 5,
                seasonNumber = 2
            )
        )
        val localEpisodes = listOf(
            TestData.episode(
                id = 1, podcastId = podcastId,
                title = "Old Title",
                description = "Old description",
                audioUrl = "https://old.com/audio.mp3",
                artworkUrl = "https://old.com/art.jpg",
                publishedAt = 1000L,
                durationSeconds = 3600,
                fileSize = 50_000_000L,
                episodeNumber = 1,
                seasonNumber = 1,
                lastPlayedPositionMs = 30000
            )
        )

        val vm = createViewModel(apiEpisodes = apiEpisodes, localEpisodes = localEpisodes)
        advanceUntilIdle()

        val ep = vm.uiState.value.episodes.single()
        assertEquals("Updated Title", ep.title)
        assertEquals("Updated description", ep.description)
        assertEquals("https://new.com/audio.mp3", ep.audioUrl)
        assertEquals("https://new.com/art.jpg", ep.artworkUrl)
        assertEquals(2000L, ep.publishedAt)
        assertEquals(7200, ep.durationSeconds)
        assertEquals(100_000_000L, ep.fileSize)
        assertEquals(5, ep.episodeNumber)
        assertEquals(2, ep.seasonNumber)
        // Local state still preserved
        assertEquals(30000L, ep.lastPlayedPositionMs)
    }

    // --- Room observer merge ---

    @Test
    fun `room observer preserves api metadata while updating local state`() = runTest {
        val apiEpisodes = listOf(
            TestData.episode(
                id = 1, podcastId = podcastId,
                title = "API Title",
                description = "API desc",
                audioUrl = "https://api.com/audio.mp3",
                lastPlayedPositionMs = 0,
                downloadStatus = DownloadStatus.NOT_DOWNLOADED
            )
        )

        val vm = createViewModel(apiEpisodes = apiEpisodes, localEpisodes = emptyList())
        advanceUntilIdle()

        // Simulate Room emitting an updated episode (e.g. download completed, playback progress saved)
        observeEpisodesFlow.value = listOf(
            TestData.episode(
                id = 1, podcastId = podcastId,
                title = "Room Title",
                description = "Room desc",
                audioUrl = "https://room.com/audio.mp3",
                downloadStatus = DownloadStatus.DOWNLOADED,
                localFilePath = "/data/ep1.mp3",
                lastPlayedPositionMs = 60000,
                lastPlayedAt = 500L
            )
        )
        advanceUntilIdle()

        val ep = vm.uiState.value.episodes.single()
        // Room local state preserved
        assertEquals(DownloadStatus.DOWNLOADED, ep.downloadStatus)
        assertEquals("/data/ep1.mp3", ep.localFilePath)
        assertEquals(60000L, ep.lastPlayedPositionMs)
        assertEquals(500L, ep.lastPlayedAt)
        // API metadata overlaid onto Room episode
        assertEquals("API Title", ep.title)
        assertEquals("API desc", ep.description)
        assertEquals("https://api.com/audio.mp3", ep.audioUrl)
    }

    // --- Subscription ---

    @Test
    fun `isSubscribed flow updates state`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSubscribed)

        isSubscribedFlow.value = true
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isSubscribed)
    }

    @Test
    fun `toggleSubscription subscribes when not subscribed`() = runTest {
        isSubscribedFlow.value = false
        val podcast = TestData.podcast(id = podcastId)
        val episodes = listOf(TestData.episode(id = 1, podcastId = podcastId))

        val vm = createViewModel(podcast = podcast, apiEpisodes = episodes)
        advanceUntilIdle()

        vm.toggleSubscription()
        advanceUntilIdle()

        coVerify { subscriptionRepository.subscribe(podcast, any()) }
    }

    @Test
    fun `toggleSubscription unsubscribes when subscribed`() = runTest {
        isSubscribedFlow.value = true
        val vm = createViewModel()
        advanceUntilIdle()

        vm.toggleSubscription()
        advanceUntilIdle()

        coVerify { subscriptionRepository.unsubscribe(podcastId) }
    }

    // --- Show archived ---

    @Test
    fun `toggleShowArchived flips showArchived`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.showArchived)

        vm.toggleShowArchived()
        assertTrue(vm.uiState.value.showArchived)

        vm.toggleShowArchived()
        assertFalse(vm.uiState.value.showArchived)
    }

    @Test
    fun `filteredEpisodes hides archived when showArchived is false`() = runTest {
        val episodes = listOf(
            TestData.episode(id = 1, podcastId = podcastId, isArchived = false, publishedAt = 200),
            TestData.episode(id = 2, podcastId = podcastId, isArchived = true, publishedAt = 100)
        )

        val vm = createViewModel(apiEpisodes = episodes)
        advanceUntilIdle()

        val filtered = vm.uiState.value.filteredEpisodes
        assertEquals(1, filtered.size)
        assertEquals(1L, filtered[0].id)
    }

    @Test
    fun `filteredEpisodes shows all when showArchived is true`() = runTest {
        val episodes = listOf(
            TestData.episode(id = 1, podcastId = podcastId, isArchived = false, publishedAt = 200),
            TestData.episode(id = 2, podcastId = podcastId, isArchived = true, publishedAt = 100)
        )

        val vm = createViewModel(apiEpisodes = episodes)
        advanceUntilIdle()

        vm.toggleShowArchived()
        val filtered = vm.uiState.value.filteredEpisodes
        assertEquals(2, filtered.size)
    }

    // --- Sort order ---

    @Test
    fun `toggleSortOrder switches between newest and oldest`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(EpisodeSortOrder.NEWEST_FIRST, vm.uiState.value.sortOrder)

        vm.toggleSortOrder()
        assertEquals(EpisodeSortOrder.OLDEST_FIRST, vm.uiState.value.sortOrder)

        vm.toggleSortOrder()
        assertEquals(EpisodeSortOrder.NEWEST_FIRST, vm.uiState.value.sortOrder)
    }

    @Test
    fun `filteredEpisodes sorted by newest first by default`() = runTest {
        val episodes = listOf(
            TestData.episode(id = 1, podcastId = podcastId, publishedAt = 100),
            TestData.episode(id = 2, podcastId = podcastId, publishedAt = 300),
            TestData.episode(id = 3, podcastId = podcastId, publishedAt = 200)
        )

        val vm = createViewModel(apiEpisodes = episodes)
        advanceUntilIdle()

        val filtered = vm.uiState.value.filteredEpisodes
        assertEquals(listOf(300L, 200L, 100L), filtered.map { it.publishedAt })
    }

    @Test
    fun `filteredEpisodes sorted by oldest first after toggle`() = runTest {
        val episodes = listOf(
            TestData.episode(id = 1, podcastId = podcastId, publishedAt = 100),
            TestData.episode(id = 2, podcastId = podcastId, publishedAt = 300),
            TestData.episode(id = 3, podcastId = podcastId, publishedAt = 200)
        )

        val vm = createViewModel(apiEpisodes = episodes)
        advanceUntilIdle()

        vm.toggleSortOrder()
        val filtered = vm.uiState.value.filteredEpisodes
        assertEquals(listOf(100L, 200L, 300L), filtered.map { it.publishedAt })
    }

    // --- Archive ---

    @Test
    fun `archiveEpisode calls repository`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.archiveEpisode(123L)
        advanceUntilIdle()

        coVerify { episodeRepository.setArchived(123L, true) }
    }

    @Test
    fun `unarchiveEpisode calls repository`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.unarchiveEpisode(456L)
        advanceUntilIdle()

        coVerify { episodeRepository.setArchived(456L, false) }
    }

    // --- Retry ---

    @Test
    fun `retry reloads data`() = runTest {
        val podcast = TestData.podcast(id = podcastId)
        val vm = createViewModel(podcast = podcast)
        advanceUntilIdle()

        vm.retry()
        advanceUntilIdle()

        coVerify(atLeast = 2) { podcastRepository.getPodcastById(podcastId) }
    }
}
