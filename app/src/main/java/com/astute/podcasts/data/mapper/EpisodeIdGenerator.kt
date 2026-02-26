package com.astute.podcasts.data.mapper

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeIdGenerator @Inject constructor() {

    fun generateId(audioUrl: String): Long {
        val digest = MessageDigest.getInstance("SHA-256").digest(audioUrl.toByteArray())
        // Read first 8 bytes as a Long, mask to positive
        var id = 0L
        for (i in 0 until 8) {
            id = (id shl 8) or (digest[i].toLong() and 0xFF)
        }
        return id and Long.MAX_VALUE
    }
}
