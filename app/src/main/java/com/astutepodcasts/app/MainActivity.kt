package com.astutepodcasts.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.astutepodcasts.app.playback.PlaybackService
import com.astutepodcasts.app.ui.navigation.AppNavGraph
import com.astutepodcasts.app.ui.theme.AstutePodcastsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
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
