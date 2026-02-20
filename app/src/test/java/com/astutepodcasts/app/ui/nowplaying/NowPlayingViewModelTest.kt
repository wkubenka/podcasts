package com.astutepodcasts.app.ui.nowplaying

import com.astutepodcasts.app.domain.model.PlaybackState
import com.astutepodcasts.app.playback.PlaybackManager
import com.astutepodcasts.app.testutil.TestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingViewModelTest {

    private val playbackStateFlow = MutableStateFlow(PlaybackState())

    private val playbackManager: PlaybackManager = mockk(relaxed = true) {
        every { playbackState } returns playbackStateFlow
    }

    private fun createViewModel() = NowPlayingViewModel(playbackManager)

    // --- Playback state ---

    @Test
    fun `playbackState exposes manager state`() {
        val vm = createViewModel()
        assertEquals(PlaybackState(), vm.playbackState.value)
    }

    @Test
    fun `playbackState includes current episode with description`() {
        val episode = TestData.episode(
            title = "Test Episode",
            description = "<p>Episode <b>show notes</b> with HTML</p>"
        )
        playbackStateFlow.value = PlaybackState(currentEpisode = episode, isPlaying = true)

        val vm = createViewModel()

        val state = vm.playbackState.value
        assertEquals(episode, state.currentEpisode)
        assertEquals("<p>Episode <b>show notes</b> with HTML</p>", state.currentEpisode?.description)
        assertTrue(state.isPlaying)
    }

    @Test
    fun `playbackState with null episode has no description`() {
        playbackStateFlow.value = PlaybackState(currentEpisode = null)

        val vm = createViewModel()

        assertNull(vm.playbackState.value.currentEpisode)
    }

    @Test
    fun `playbackState with blank description`() {
        val episode = TestData.episode(description = "")
        playbackStateFlow.value = PlaybackState(currentEpisode = episode)

        val vm = createViewModel()

        assertEquals("", vm.playbackState.value.currentEpisode?.description)
    }

    // --- Delegation ---

    @Test
    fun `togglePlayPause delegates to playback manager`() {
        val vm = createViewModel()
        vm.togglePlayPause()
        verify { playbackManager.togglePlayPause() }
    }

    @Test
    fun `seekTo delegates to playback manager`() {
        val vm = createViewModel()
        vm.seekTo(30000L)
        verify { playbackManager.seekTo(30000L) }
    }

    @Test
    fun `skipBackward delegates to playback manager`() {
        val vm = createViewModel()
        vm.skipBackward()
        verify { playbackManager.skipBackward() }
    }

    @Test
    fun `skipForward delegates to playback manager`() {
        val vm = createViewModel()
        vm.skipForward()
        verify { playbackManager.skipForward() }
    }

    @Test
    fun `setPlaybackSpeed delegates to playback manager`() {
        val vm = createViewModel()
        vm.setPlaybackSpeed(1.5f)
        verify { playbackManager.setPlaybackSpeed(1.5f) }
    }
}
