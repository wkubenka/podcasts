package com.astutepodcasts.app.data.remote

import com.astutepodcasts.app.domain.model.Episode
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssFeedParser @Inject constructor() {

    fun parse(xml: String, podcastId: Long, podcastArtworkUrl: String?): List<Episode> {
        val episodes = mutableListOf<Episode>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var insideItem = false
        var title = ""
        var description = ""
        var audioUrl = ""
        var fileSize = 0L
        var imageUrl: String? = null
        var pubDate = 0L
        var durationSeconds = 0
        var episodeNumber: Int? = null
        var seasonNumber: Int? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName.equals("item", ignoreCase = true)) {
                        insideItem = true
                        title = ""
                        description = ""
                        audioUrl = ""
                        fileSize = 0L
                        imageUrl = null
                        pubDate = 0L
                        durationSeconds = 0
                        episodeNumber = null
                        seasonNumber = null
                    } else if (insideItem) {
                        when {
                            tagName.equals("title", ignoreCase = true) -> {
                                title = parser.nextText().orEmpty().trim()
                            }
                            tagName.equals("description", ignoreCase = true) -> {
                                description = parser.nextText().orEmpty().trim()
                            }
                            tagName.equals("itunes:summary", ignoreCase = true) -> {
                                if (description.isBlank()) {
                                    description = parser.nextText().orEmpty().trim()
                                }
                            }
                            tagName.equals("enclosure", ignoreCase = true) -> {
                                val url = parser.getAttributeValue(null, "url")
                                if (!url.isNullOrBlank()) audioUrl = url.trim()
                                val length = parser.getAttributeValue(null, "length")
                                fileSize = length?.toLongOrNull() ?: 0L
                            }
                            tagName.equals("itunes:image", ignoreCase = true) -> {
                                val href = parser.getAttributeValue(null, "href")
                                if (!href.isNullOrBlank()) imageUrl = href.trim()
                            }
                            tagName.equals("pubDate", ignoreCase = true) -> {
                                pubDate = parseRfc2822Date(parser.nextText().orEmpty().trim())
                            }
                            tagName.equals("itunes:duration", ignoreCase = true) -> {
                                durationSeconds = parseDuration(parser.nextText().orEmpty().trim())
                            }
                            tagName.equals("itunes:episode", ignoreCase = true) -> {
                                episodeNumber = parser.nextText().orEmpty().trim().toIntOrNull()
                            }
                            tagName.equals("itunes:season", ignoreCase = true) -> {
                                seasonNumber = parser.nextText().orEmpty().trim().toIntOrNull()
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName.equals("item", ignoreCase = true) && insideItem) {
                        insideItem = false
                        if (audioUrl.isNotBlank()) {
                            episodes.add(
                                Episode(
                                    id = 0,
                                    podcastId = podcastId,
                                    title = title,
                                    description = stripHtml(description),
                                    audioUrl = audioUrl,
                                    artworkUrl = imageUrl ?: podcastArtworkUrl,
                                    publishedAt = pubDate,
                                    durationSeconds = durationSeconds,
                                    fileSize = fileSize,
                                    episodeNumber = episodeNumber,
                                    seasonNumber = seasonNumber
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return episodes
    }

    private fun parseRfc2822Date(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        val formats = arrayOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm Z"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(dateStr)?.time?.div(1000) ?: continue
            } catch (_: Exception) {
                // try next format
            }
        }
        return 0L
    }

    private fun parseDuration(value: String): Int {
        if (value.isBlank()) return 0
        // Pure seconds
        value.toIntOrNull()?.let { return it }
        // HH:MM:SS or MM:SS
        val parts = value.split(":")
        return when (parts.size) {
            3 -> {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                val s = parts[2].toIntOrNull() ?: 0
                h * 3600 + m * 60 + s
            }
            2 -> {
                val m = parts[0].toIntOrNull() ?: 0
                val s = parts[1].toIntOrNull() ?: 0
                m * 60 + s
            }
            else -> 0
        }
    }

    private fun stripHtml(html: String): String {
        return html.replace(Regex("<[^>]*>"), "").trim()
    }
}
