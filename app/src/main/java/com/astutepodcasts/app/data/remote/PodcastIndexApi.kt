package com.astutepodcasts.app.data.remote

import com.astutepodcasts.app.data.remote.dto.EpisodesResponse
import com.astutepodcasts.app.data.remote.dto.SearchResponse
import com.astutepodcasts.app.data.remote.dto.TrendingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PodcastIndexApi {

    @GET("search/byterm")
    suspend fun searchByTerm(
        @Query("q") query: String,
        @Query("max") max: Int = 20
    ): SearchResponse

    @GET("episodes/byfeedid")
    suspend fun getEpisodesByFeedId(
        @Query("id") feedId: Long,
        @Query("max") max: Int = 100
    ): EpisodesResponse

    @GET("podcasts/trending")
    suspend fun getTrending(
        @Query("max") max: Int = 20,
        @Query("lang") lang: String = "en"
    ): TrendingResponse
}
