package com.astutepodcasts.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RssFeedService @Inject constructor(
    @Named("download") private val httpClient: OkHttpClient
) {
    suspend fun fetchFeed(feedUrl: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(feedUrl)
            .header("User-Agent", "AstutePodcasts/1.0")
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch feed: HTTP ${response.code}")
        }
        response.body?.string() ?: throw Exception("Empty response body")
    }
}
