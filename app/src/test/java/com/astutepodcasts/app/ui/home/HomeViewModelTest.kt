package com.astutepodcasts.app.ui.home

import app.cash.turbine.test
import com.astutepodcasts.app.domain.model.Episode
import com.astutepodcasts.app.domain.model.Podcast
import com.astutepodcasts.app.domain.repository.EpisodeRepository
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val subscribedPodcastsFlow = MutableStateFlow<List<Podcast>>(emptyList())
    private val recentEpisodesFlow = MutableStateFlow<List<Episode>>(emptyList())
    private val continueListeningFlow = MutableStateFlow<List<Episode>>(emptyList())

    private val subscriptionRepository: SubscriptionRepository = mockk {
        every { getSubscribedPodcasts() } returns subscribedPodcastsFlow
        every { getRecentEpisodes() } returns recentEpisodesFlow
        coEvery { refreshFeeds() } returns Unit
    }

    private val episodeRepository: EpisodeRepository = mockk {
        every { getRecentlyPlayedEpisodes() } returns continueListeningFlow
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(subscriptionRepository, episodeRepository)
    }

    @Test
    fun `RECENT_EPISODES sort orders podcasts by episode recency`() = runTest {
        val podcastA = TestData.podcast(id = 1, title = "AAA Podcast")
        val podcastB = TestData.podcast(id = 2, title = "BBB Podcast")
        val podcastC = TestData.podcast(id = 3, title = "CCC Podcast")

        // Episodes order: B's episode is most recent, then A's
        val episodes = listOf(
            TestData.episode(podcastId = 2, publishedAt = 2000),
            TestData.episode(podcastId = 1, publishedAt = 1000)
        )

        subscribedPodcastsFlow.value = listOf(podcastA, podcastB, podcastC)
        recentEpisodesFlow.value = episodes

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            // B first (most recent episode), then A, then C (no episodes, appended at end)
            assertEquals(listOf(podcastB, podcastA, podcastC), state.subscribedPodcasts)
            assertEquals(PodcastSortOrder.RECENT_EPISODES, state.sortOrder)
        }
    }

    @Test
    fun `ALPHABETICAL sort orders podcasts by title`() = runTest {
        val podcastZ = TestData.podcast(id = 1, title = "Zebra Pod")
        val podcastA = TestData.podcast(id = 2, title = "Alpha Pod")
        val podcastM = TestData.podcast(id = 3, title = "middle Pod")

        subscribedPodcastsFlow.value = listOf(podcastZ, podcastA, podcastM)
        recentEpisodesFlow.value = emptyList()

        val vm = createViewModel()
        advanceUntilIdle()

        vm.setSortOrder(PodcastSortOrder.ALPHABETICAL)
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(
                listOf(podcastA, podcastM, podcastZ),
                state.subscribedPodcasts
            )
            assertEquals(PodcastSortOrder.ALPHABETICAL, state.sortOrder)
        }
    }

    @Test
    fun `setSortOrder updates sort in state`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.setSortOrder(PodcastSortOrder.ALPHABETICAL)
        advanceUntilIdle()

        vm.uiState.test {
            assertEquals(PodcastSortOrder.ALPHABETICAL, awaitItem().sortOrder)
        }
    }

    @Test
    fun `refresh error sets error message`() = runTest {
        coEvery { subscriptionRepository.refreshFeeds() } throws RuntimeException("Refresh failed")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNotNull(state.error)
            assertFalse(state.isRefreshing)
        }
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        coEvery { subscriptionRepository.refreshFeeds() } throws RuntimeException("error")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.clearError()
        advanceUntilIdle()

        vm.uiState.test {
            assertNull(awaitItem().error)
        }
    }

    @Test
    fun `continue listening passes through from repository`() = runTest {
        val episodes = listOf(
            TestData.episode(id = 1, title = "Ep 1", lastPlayedPositionMs = 5000),
            TestData.episode(id = 2, title = "Ep 2", lastPlayedPositionMs = 10000)
        )
        continueListeningFlow.value = episodes

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            assertEquals(episodes, awaitItem().continueListening)
        }
    }

    @Test
    fun `successful refresh completes without error`() = runTest {
        coEvery { subscriptionRepository.refreshFeeds() } returns Unit

        val vm = createViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.error)
            assertFalse(state.isRefreshing)
        }
    }
}
