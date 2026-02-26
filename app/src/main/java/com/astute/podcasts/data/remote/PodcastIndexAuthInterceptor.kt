package com.astute.podcasts.data.remote

import com.astute.podcasts.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import javax.inject.Inject

class PodcastIndexAuthInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = BuildConfig.PODCAST_INDEX_API_KEY
        val apiSecret = BuildConfig.PODCAST_INDEX_API_SECRET
        val timestamp = (System.currentTimeMillis() / 1000).toString()

        val authHash = sha1Hash("$apiKey$apiSecret$timestamp")

        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "AstutePodcasts/1.0")
            .addHeader("X-Auth-Key", apiKey)
            .addHeader("X-Auth-Date", timestamp)
            .addHeader("Authorization", authHash)
            .build()

        return chain.proceed(request)
    }

    private fun sha1Hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
