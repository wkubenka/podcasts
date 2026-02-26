package com.astute.podcasts.data.remote

import com.astute.podcasts.data.remote.dto.SearchResponse
import com.astute.podcasts.data.remote.dto.TrendingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PodcastIndexApi {

    @GET("search/byterm")
    suspend fun searchByTerm(
        @Query("q") query: String,
        @Query("max") max: Int = 20
    ): SearchResponse

    @GET("podcasts/trending")
    suspend fun getTrending(
        @Query("max") max: Int = 20,
        @Query("lang") lang: String = "en"
    ): TrendingResponse
}
