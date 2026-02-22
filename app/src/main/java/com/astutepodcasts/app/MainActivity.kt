package com.astutepodcasts.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.astutepodcasts.app.playback.PlaybackService
import com.astutepodcasts.app.playback.PlaybackServiceConnection
import com.astutepodcasts.app.ui.navigation.AppNavGraph
import com.astutepodcasts.app.ui.theme.AstutePodcastsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playbackServiceConnection: PlaybackServiceConnection

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Start the PlaybackService early so its MediaLibrarySession is registered
        // with the system. This allows Bluetooth AVRCP to discover the app as an
        // available media source for car head units.
        playbackServiceConnection.connect()

        enableEdgeToEdge()
        setContent {
            AstutePodcastsTheme {
                val openNowPlaying = intent?.getBooleanExtra(
                    PlaybackService.EXTRA_OPEN_NOW_PLAYING, false
                ) == true
                AppNavGraph(openNowPlaying = openNowPlaying)
            }
        }
    }
}
