package com.astutepodcasts.app.ui.components

import com.astutepodcasts.app.testutil.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeListItemKtTest {

    // --- formatDuration tests ---

    @Test
    fun `formatDuration shows minutes only when under one hour`() {
        assertEquals("45m", formatDuration(45 * 60))
    }

    @Test
    fun `formatDuration shows hours and minutes when one hour or more`() {
        assertEquals("1h 30m", formatDuration(90 * 60))
    }

    @Test
    fun `formatDuration shows 0m for zero seconds`() {
        assertEquals("0m", formatDuration(0))
    }

    @Test
    fun `formatDuration truncates partial minutes`() {
        assertEquals("1m", formatDuration(119))
    }

    @Test
    fun `formatDuration shows exact hours with 0 remaining minutes`() {
        assertEquals("2h 0m", formatDuration(7200))
    }

    // --- formatEpisodeMetadata: total duration (not in progress) ---

    @Test
    fun `metadata shows total duration when episode has not been played`() {
        val ep = TestData.episode(durationSeconds = 3600, lastPlayedPositionMs = 0)
        val metadata = formatEpisodeMetadata(ep)
        assertTrue("Expected total duration '1h 0m' but got: $metadata", metadata.endsWith("1h 0m"))
    }

    @Test
    fun `metadata shows total duration when position equals duration`() {
        val ep = TestData.episode(durationSeconds = 3600, lastPlayedPositionMs = 3600_000)
        val metadata = formatEpisodeMetadata(ep)
        assertTrue("Expected total duration but got: $metadata", metadata.endsWith("1h 0m"))
    }

    @Test
    fun `metadata shows total duration when position exceeds duration`() {
        val ep = TestData.episode(durationSeconds = 3600, lastPlayedPositionMs = 4000_000)
        val metadata = formatEpisodeMetadata(ep)
        assertTrue("Expected total duration but got: $metadata", metadata.endsWith("1h 0m"))
    }

    @Test
    fun `metadata shows total duration when duration is zero`() {
        val ep = TestData.episode(durationSeconds = 0, lastPlayedPositionMs = 5000)
        val metadata = formatEpisodeMetadata(ep)
        assertTrue("Expected total duration but got: $metadata", metadata.endsWith("0m"))
    }

    // --- formatEpisodeMetadata: remaining time (in progress) ---

    @Test
    fun `metadata shows remaining time for in-progress episode`() {
        // 1h episode, 30m played -> 30m left
        val ep = TestData.episode(durationSeconds = 3600, lastPlayedPositionMs = 1800_000)
        val metadata = formatEpisodeMetadata(ep)
        assertTrue("Expected '30m left' but got: $metadata", metadata.endsWith("30m left"))
    }

    @Test
    fun `metadata shows remaining time with hours for long episode`() {
        // 2h episode, 15m played -> 1h 45m left
        val ep = TestData.episode(durationSeconds = 7200, lastPlayedPositionMs = 900_000)
        val metadata = formatEpisodeMetadata(ep)
        assertTrue("Expected '1h 45m left' but got: $metadata", metadata.endsWith("1h 45m left"))
    }

    @Test
    fun `metadata shows remaining time near start of episode`() {
        // 1h episode, 1ms played -> ~59m left (truncated from 59m 59s)
        val ep = TestData.episode(durationSeconds = 3600, lastPlayedPositionMs = 1)
        val metadata = formatEpisodeMetadata(ep)
        assertTrue("Expected 'left' suffix but got: $metadata", metadata.endsWith("left"))
        assertTrue("Expected remaining close to full duration but got: $metadata", metadata.contains("59m left"))
    }

    @Test
    fun `metadata shows remaining time near end of episode`() {
        // 1h episode, 59m 30s played -> 0m left
        val ep = TestData.episode(durationSeconds = 3600, lastPlayedPositionMs = 3570_000)
        val metadata = formatEpisodeMetadata(ep)
        assertTrue("Expected '0m left' but got: $metadata", metadata.endsWith("0m left"))
    }

    @Test
    fun `metadata contains date and separator`() {
        val ep = TestData.episode(durationSeconds = 3600, lastPlayedPositionMs = 1800_000)
        val metadata = formatEpisodeMetadata(ep)
        assertTrue("Expected '\u2022' separator but got: $metadata", metadata.contains("\u2022"))
    }

    // --- Edge cases for remaining time boundary ---

    @Test
    fun `metadata shows remaining when position is 1ms`() {
        // Smallest positive position should still trigger "left" display
        val ep = TestData.episode(durationSeconds = 600, lastPlayedPositionMs = 1)
        val metadata = formatEpisodeMetadata(ep)
        assertTrue("Expected 'left' for minimal progress but got: $metadata", metadata.endsWith("left"))
    }

    @Test
    fun `metadata shows total duration when position is exactly at boundary`() {
        // position == duration * 1000 should NOT show "left"
        val ep = TestData.episode(durationSeconds = 600, lastPlayedPositionMs = 600_000)
        val metadata = formatEpisodeMetadata(ep)
        assertTrue("Expected total duration '10m' but got: $metadata", metadata.endsWith("10m"))
    }

    @Test
    fun `metadata shows remaining for position just under duration`() {
        // position == duration * 1000 - 1 should show "left"
        val ep = TestData.episode(durationSeconds = 600, lastPlayedPositionMs = 599_999)
        val metadata = formatEpisodeMetadata(ep)
        assertTrue("Expected 'left' for near-complete but got: $metadata", metadata.endsWith("left"))
    }
}
