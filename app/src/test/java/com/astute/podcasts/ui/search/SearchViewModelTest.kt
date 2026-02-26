package com.astute.podcasts.ui.search

import app.cash.turbine.test
import com.astute.podcasts.domain.repository.PodcastRepository
import com.astute.podcasts.testutil.MainDispatcherRule
import com.astute.podcasts.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val podcastRepository: PodcastRepository = mockk()

    private fun createViewModel(): SearchViewModel {
        return SearchViewModel(podcastRepository)
    }

    @Test
    fun `initial state loads trending podcasts`() = runTest {
        val trending = listOf(TestData.podcast(id = 1), TestData.podcast(id = 2))
        coEvery { podcastRepository.getTrendingPodcasts() } returns trending

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(trending, state.results)
        assertFalse(state.isLoading)
        assertTrue(state.isShowingTrending)
        assertNull(state.error)
    }

    @Test
    fun `trending error sets error message`() = runTest {
        coEvery { podcastRepository.getTrendingPodcasts() } throws RuntimeException("Network error")

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.error)
        assertFalse(state.isLoading)
        assertTrue(state.results.isEmpty())
    }

    @Test
    fun `onQueryChange updates query in state`() = runTest {
        coEvery { podcastRepository.getTrendingPodcasts() } returns emptyList()

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("kotlin")
        assertEquals("kotlin", vm.uiState.value.query)
    }

    @Test
    fun `retry with query performs search`() = runTest {
        val trending = listOf(TestData.podcast(id = 1))
        val searchResults = listOf(TestData.podcast(id = 10, title = "Search Result"))
        coEvery { podcastRepository.getTrendingPodcasts() } returns trending
        coEvery { podcastRepository.searchPodcasts("kotlin") } returns searchResults

        val vm = createViewModel()
        advanceUntilIdle()

        // Set query directly in state for retry
        vm.onQueryChange("kotlin")
        vm.retry()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(searchResults, state.results)
        assertFalse(state.isShowingTrending)
        coVerify { podcastRepository.searchPodcasts("kotlin") }
    }

    @Test
    fun `retry with blank query loads trending`() = runTest {
        val trending = listOf(TestData.podcast(id = 1))
        coEvery { podcastRepository.getTrendingPodcasts() } returns trending

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("")
        vm.retry()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isShowingTrending)
        coVerify(atLeast = 2) { podcastRepository.getTrendingPodcasts() }
    }

    @Test
    fun `search error sets error message`() = runTest {
        coEvery { podcastRepository.getTrendingPodcasts() } returns emptyList()
        coEvery { podcastRepository.searchPodcasts(any()) } throws RuntimeException("Search failed")

        val vm = createViewModel()
        advanceUntilIdle()

        vm.onQueryChange("test")
        vm.retry()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.error)
        assertFalse(state.isLoading)
    }
}
