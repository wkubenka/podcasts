package com.astutepodcasts.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.astutepodcasts.app.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

data class EpisodeIdAudioUrl(val id: Long, val audioUrl: String)

@Dao
interface EpisodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query(
        """
        INSERT INTO episodes (id, podcastId, title, description, audioUrl, artworkUrl, publishedAt, durationSeconds, fileSize, episodeNumber, seasonNumber, downloadStatus, localFilePath, lastPlayedPositionMs, lastPlayedAt, isArchived)
        VALUES (:id, :podcastId, :title, :description, :audioUrl, :artworkUrl, :publishedAt, :durationSeconds, :fileSize, :episodeNumber, :seasonNumber, 'NOT_DOWNLOADED', NULL, 0, 0, 0)
        ON CONFLICT(id) DO UPDATE SET
            title = excluded.title,
            description = excluded.description,
            audioUrl = excluded.audioUrl,
            artworkUrl = excluded.artworkUrl,
            publishedAt = excluded.publishedAt,
            durationSeconds = excluded.durationSeconds,
            fileSize = excluded.fileSize,
            episodeNumber = excluded.episodeNumber,
            seasonNumber = excluded.seasonNumber
        """
    )
    suspend fun upsertPreservingDownloadStatus(
        id: Long, podcastId: Long, title: String, description: String,
        audioUrl: String, artworkUrl: String?, publishedAt: Long, durationSeconds: Int,
        fileSize: Long, episodeNumber: Int?, seasonNumber: Int?
    )

    @Transaction
    suspend fun upsertAllPreservingDownloadStatus(episodes: List<EpisodeEntity>) {
        for (episode in episodes) {
            upsertPreservingDownloadStatus(
                episode.id, episode.podcastId, episode.title, episode.description,
                episode.audioUrl, episode.artworkUrl, episode.publishedAt, episode.durationSeconds,
                episode.fileSize, episode.episodeNumber, episode.seasonNumber
            )
        }
    }

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishedAt DESC")
    suspend fun getByPodcastId(podcastId: Long): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY publishedAt DESC")
    fun observeByPodcastId(podcastId: Long): Flow<List<EpisodeEntity>>

    @Query(
        """
        SELECT e.* FROM episodes e
        INNER JOIN subscriptions s ON e.podcastId = s.podcastId
        WHERE e.isArchived = 0
        ORDER BY e.publishedAt DESC
        LIMIT :limit
        """
    )
    fun getRecentSubscribedEpisodes(limit: Int = 50): Flow<List<EpisodeEntity>>

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId")
    suspend fun deleteByPodcastId(podcastId: Long)

    @Query("SELECT * FROM episodes WHERE id = :episodeId")
    suspend fun getEpisodeById(episodeId: Long): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE id = :episodeId")
    fun observeById(episodeId: Long): Flow<EpisodeEntity?>

    @Query("UPDATE episodes SET downloadStatus = :status, localFilePath = :localFilePath WHERE id = :episodeId")
    suspend fun updateDownloadStatus(episodeId: Long, status: String, localFilePath: String? = null)

    @Query("SELECT * FROM episodes WHERE downloadStatus IN ('QUEUED', 'DOWNLOADING', 'DOWNLOADED') ORDER BY publishedAt DESC")
    fun getDownloadedEpisodes(): Flow<List<EpisodeEntity>>

    @Query("UPDATE episodes SET lastPlayedPositionMs = :positionMs, lastPlayedAt = :lastPlayedAt WHERE id = :episodeId")
    suspend fun updatePlaybackPosition(episodeId: Long, positionMs: Long, lastPlayedAt: Long)

    @Query(
        """
        SELECT * FROM episodes
        WHERE lastPlayedAt > 0
            AND lastPlayedPositionMs > 0
            AND (durationSeconds = 0 OR lastPlayedPositionMs < durationSeconds * 1000)
            AND isArchived = 0
        ORDER BY lastPlayedAt DESC
        LIMIT 10
        """
    )
    fun getRecentlyPlayed(): Flow<List<EpisodeEntity>>

    @Query("SELECT id, audioUrl FROM episodes WHERE podcastId = :podcastId")
    suspend fun getEpisodeIdsByAudioUrl(podcastId: Long): List<EpisodeIdAudioUrl>

    @Query("UPDATE episodes SET isArchived = :isArchived WHERE id = :episodeId")
    suspend fun updateArchived(episodeId: Long, isArchived: Boolean)
}
