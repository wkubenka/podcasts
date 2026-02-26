package com.astute.podcasts.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackServiceConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    fun connect() {
        if (_controller.value != null) return

        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val browserFuture = MediaBrowser.Builder(context, sessionToken).buildAsync()
        browserFuture.addListener(
            { _controller.value = browserFuture.get() },
            MoreExecutors.directExecutor()
        )
    }

    fun disconnect() {
        _controller.value?.release()
        _controller.value = null
    }
}
