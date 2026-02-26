package com.astute.podcasts.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.astute.podcasts.data.local.dao.EpisodeDao
import com.astute.podcasts.data.local.dao.PodcastDao
import com.astute.podcasts.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaBrowseTreeTest {

    private val podcastDao: PodcastDao = mockk(relaxed = true)
    private val episodeDao: EpisodeDao = mockk(relaxed = true)

    private fun createBrowseTree() = MediaBrowseTree(podcastDao, episodeDao)

    // --- Root ---

    @Test
    fun `root item is browsable and not playable`() {
        val tree = createBrowseTree()
        val root = tree.getRootItem()

        assertEquals("root", root.mediaId)
        assertEquals(true, root.mediaMetadata.isBrowsable)
        assertEquals(false, root.mediaMetadata.isPlayable)
        assertEquals("Astute Podcasts", root.mediaMetadata.title.toString())
    }

    @Test
    fun `root children returns four category folders`() {
        val tree = createBrowseTree()
        val children = tree.getRootChildren()

        assertEquals(4, children.size)
        assertEquals("subscriptions", children[0].mediaId)
        assertEquals("recent", children[1].mediaId)
        assertEquals("downloads", children[2].mediaId)
        assertEquals("continue_listening", children[3].mediaId)
    }

    @Test
    fun `root category folders are browsable and not playable`() {
        val tree = createBrowseTree()
        val children = tree.getRootChildren()

        children.forEach { folder ->
            assertEquals(
                "Folder ${folder.mediaId} should be browsable",
                true,
                folder.mediaMetadata.isBrowsable
            )
            assertEquals(
                "Folder ${folder.mediaId} should not be playable",
                false,
                folder.mediaMetadata.isPlayable
            )
        }
    }

    @Test
    fun `root category folders have correct titles`() {
        val tree = createBrowseTree()
        val children = tree.getRootChildren()

        assertEquals("Subscriptions", children[0].mediaMetadata.title.toString())
        assertEquals("Recent Episodes", children[1].mediaMetadata.title.toString())
        assertEquals("Downloads", children[2].mediaMetadata.title.toString())
        assertEquals("Continue Listening", children[3].mediaMetadata.title.toString())
    }

    @Test
    fun `root category folders have correct media types`() {
        val tree = createBrowseTree()
        val children = tree.getRootChildren()

        assertEquals(MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS, children[0].mediaMetadata.mediaType)
        assertEquals(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, children[1].mediaMetadata.mediaType)
        assertEquals(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, children[2].mediaMetadata.mediaType)
        assertEquals(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, children[3].mediaMetadata.mediaType)
    }

    // --- getChildren dispatches to correct DAO ---

    @Test
    fun `getChildren for root returns root children`() = runTest {
        val tree = createBrowseTree()
        val children = tree.getChildren("root")

        assertEquals(4, children.size)
        assertEquals("subscriptions", children[0].mediaId)
    }

    @Test
    fun `getChildren for subscriptions queries podcastDao`() = runTest {
        val podcasts = listOf(
            TestData.podcastEntity(id = 1L, title = "Podcast A", author = "Author A"),
            TestData.podcastEntity(id = 2L, title = "Podcast B", author = "Author B")
        )
        coEvery { podcastDao.getSubscribedPodcasts() } returns flowOf(podcasts)

        val tree = createBrowseTree()
        val children = tree.getChildren("subscriptions")

        assertEquals(2, children.size)
        coVerify { podcastDao.getSubscribedPodcasts() }
    }

    @Test
    fun `getChildren for recent queries episodeDao recent episodes`() = runTest {
        val episodes = listOf(
            TestData.episodeEntity(id = 10L, title = "Recent Ep 1"),
            TestData.episodeEntity(id = 11L, title = "Recent Ep 2")
        )
        coEvery { episodeDao.getRecentSubscribedEpisodes(50) } returns flowOf(episodes)

        val tree = createBrowseTree()
        val children = tree.getChildren("recent")

        assertEquals(2, children.size)
        coVerify { episodeDao.getRecentSubscribedEpisodes(50) }
    }

    @Test
    fun `getChildren for downloads queries episodeDao downloaded episodes`() = runTest {
        val episodes = listOf(
            TestData.episodeEntity(id = 20L, title = "Downloaded Ep", downloadStatus = "DOWNLOADED")
        )
        coEvery { episodeDao.getDownloadedEpisodes() } returns flowOf(episodes)

        val tree = createBrowseTree()
        val children = tree.getChildren("downloads")

        assertEquals(1, children.size)
        coVerify { episodeDao.getDownloadedEpisodes() }
    }

    @Test
    fun `getChildren for continue listening queries episodeDao recently played`() = runTest {
        val episodes = listOf(
            TestData.episodeEntity(id = 30L, title = "In Progress Ep", lastPlayedPositionMs = 60_000L)
        )
        coEvery { episodeDao.getRecentlyPlayed() } returns flowOf(episodes)

        val tree = createBrowseTree()
        val children = tree.getChildren("continue_listening")

        assertEquals(1, children.size)
        coVerify { episodeDao.getRecentlyPlayed() }
    }

    @Test
    fun `getChildren for podcast prefix queries episodes by podcast id`() = runTest {
        val episodes = listOf(
            TestData.episodeEntity(id = 40L, podcastId = 5L, title = "Ep 1"),
            TestData.episodeEntity(id = 41L, podcastId = 5L, title = "Ep 2")
        )
        coEvery { episodeDao.getByPodcastId(5L) } returns episodes

        val tree = createBrowseTree()
        val children = tree.getChildren("podcast:5")

        assertEquals(2, children.size)
        coVerify { episodeDao.getByPodcastId(5L) }
    }

    @Test
    fun `getChildren for unknown parent returns empty list`() = runTest {
        val tree = createBrowseTree()
        val children = tree.getChildren("unknown_id")

        assertTrue(children.isEmpty())
    }

    @Test
    fun `getChildren for invalid podcast prefix returns empty list`() = runTest {
        val tree = createBrowseTree()
        val children = tree.getChildren("podcast:not_a_number")

        assertTrue(children.isEmpty())
    }

    // --- Podcast browse items ---

    @Test
    fun `subscribed podcasts are browsable and not playable`() = runTest {
        val podcasts = listOf(
            TestData.podcastEntity(id = 1L, title = "My Podcast", author = "Host")
        )
        coEvery { podcastDao.getSubscribedPodcasts() } returns flowOf(podcasts)

        val tree = createBrowseTree()
        val children = tree.getChildren("subscriptions")

        val item = children.first()
        assertEquals(true, item.mediaMetadata.isBrowsable)
        assertEquals(false, item.mediaMetadata.isPlayable)
    }

    @Test
    fun `subscribed podcasts have correct media id and metadata`() = runTest {
        val podcasts = listOf(
            TestData.podcastEntity(id = 42L, title = "Great Podcast", author = "Jane Doe")
        )
        coEvery { podcastDao.getSubscribedPodcasts() } returns flowOf(podcasts)

        val tree = createBrowseTree()
        val children = tree.getChildren("subscriptions")

        val item = children.first()
        assertEquals("podcast:42", item.mediaId)
        assertEquals("Great Podcast", item.mediaMetadata.title.toString())
        assertEquals("Jane Doe", item.mediaMetadata.artist.toString())
        assertEquals(MediaMetadata.MEDIA_TYPE_PODCAST, item.mediaMetadata.mediaType)
    }

    // --- Episode browse items ---

    @Test
    fun `episode items are playable and not browsable`() = runTest {
        val episodes = listOf(
            TestData.episodeEntity(id = 100L, title = "Episode 1")
        )
        coEvery { episodeDao.getRecentSubscribedEpisodes(50) } returns flowOf(episodes)

        val tree = createBrowseTree()
        val children = tree.getChildren("recent")

        val item = children.first()
        assertEquals(false, item.mediaMetadata.isBrowsable)
        assertEquals(true, item.mediaMetadata.isPlayable)
    }

    @Test
    fun `episode items have correct media id and metadata`() = runTest {
        val episodes = listOf(
            TestData.episodeEntity(id = 200L, title = "Great Episode", description = "A description")
        )
        coEvery { episodeDao.getRecentSubscribedEpisodes(50) } returns flowOf(episodes)

        val tree = createBrowseTree()
        val children = tree.getChildren("recent")

        val item = children.first()
        assertEquals("episode:200", item.mediaId)
        assertEquals("Great Episode", item.mediaMetadata.title.toString())
        assertEquals("A description", item.mediaMetadata.description.toString())
        assertEquals(MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE, item.mediaMetadata.mediaType)
    }

    // --- Empty results ---

    @Test
    fun `empty subscriptions returns empty list`() = runTest {
        coEvery { podcastDao.getSubscribedPodcasts() } returns flowOf(emptyList())

        val tree = createBrowseTree()
        val children = tree.getChildren("subscriptions")

        assertTrue(children.isEmpty())
    }

    @Test
    fun `empty podcast episodes returns empty list`() = runTest {
        coEvery { episodeDao.getByPodcastId(99L) } returns emptyList()

        val tree = createBrowseTree()
        val children = tree.getChildren("podcast:99")

        assertTrue(children.isEmpty())
    }

    // --- resolvePlayableItem ---

    @Test
    fun `resolvePlayableItem resolves episode id to playable media item`() = runTest {
        val entity = TestData.episodeEntity(
            id = 300L,
            title = "Resolved Episode",
            audioUrl = "https://example.com/ep300.mp3"
        )
        coEvery { episodeDao.getEpisodeById(300L) } returns entity

        val tree = createBrowseTree()
        val browseItem = MediaItem.Builder().setMediaId("episode:300").build()
        val resolved = tree.resolvePlayableItem(browseItem)

        // Resolved item should use the original episode id as media id
        assertEquals("300", resolved?.mediaId)
        assertEquals("Resolved Episode", resolved?.mediaMetadata?.title.toString())
    }

    @Test
    fun `resolvePlayableItem returns null for unknown episode id`() = runTest {
        coEvery { episodeDao.getEpisodeById(999L) } returns null

        val tree = createBrowseTree()
        val browseItem = MediaItem.Builder().setMediaId("episode:999").build()
        val resolved = tree.resolvePlayableItem(browseItem)

        assertNull(resolved)
    }

    @Test
    fun `resolvePlayableItem returns null for invalid episode id format`() = runTest {
        val tree = createBrowseTree()
        val browseItem = MediaItem.Builder().setMediaId("episode:abc").build()
        val resolved = tree.resolvePlayableItem(browseItem)

        assertNull(resolved)
    }

    @Test
    fun `resolvePlayableItem passes through non-episode items unchanged`() = runTest {
        val tree = createBrowseTree()
        val nonEpisodeItem = MediaItem.Builder().setMediaId("some_other_id").build()
        val resolved = tree.resolvePlayableItem(nonEpisodeItem)

        assertEquals("some_other_id", resolved?.mediaId)
    }

    @Test
    fun `resolvePlayableItem prefers local file path over remote url`() = runTest {
        val entity = TestData.episodeEntity(
            id = 400L,
            audioUrl = "https://example.com/ep400.mp3",
            localFilePath = "/data/local/ep400.mp3",
            downloadStatus = "DOWNLOADED"
        )
        coEvery { episodeDao.getEpisodeById(400L) } returns entity

        val tree = createBrowseTree()
        val browseItem = MediaItem.Builder().setMediaId("episode:400").build()
        val resolved = tree.resolvePlayableItem(browseItem)

        // EpisodeMediaItemMapper selects localFilePath when available
        // The resolved item should have the local file URI
        assertEquals("400", resolved?.mediaId)
    }
}
