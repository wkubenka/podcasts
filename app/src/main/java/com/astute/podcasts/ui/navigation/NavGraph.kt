package com.astute.podcasts.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.astute.podcasts.ui.MainViewModel
import com.astute.podcasts.ui.components.MiniPlayer
import com.astute.podcasts.ui.downloads.DownloadsScreen
import com.astute.podcasts.ui.home.HomeScreen
import com.astute.podcasts.ui.nowplaying.NowPlayingScreen
import com.astute.podcasts.ui.podcastdetail.PodcastDetailScreen
import com.astute.podcasts.ui.search.SearchScreen

@Composable
fun AppNavGraph(
    openNowPlaying: Boolean = false,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no action needed on result */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val navController = rememberNavController()

    LaunchedEffect(openNowPlaying) {
        if (openNowPlaying) {
            navController.navigate(Screen.NowPlaying.route) {
                launchSingleTop = true
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val playbackState by mainViewModel.playbackState.collectAsStateWithLifecycle()
    val downloadProgress by mainViewModel.downloadProgress.collectAsStateWithLifecycle()
    val showMiniPlayer = playbackState.currentEpisode != null
        && currentRoute != Screen.NowPlaying.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.Downloads.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon
                                    else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onPodcastClick = { podcastId ->
                            navController.navigate(Screen.PodcastDetail.createRoute(podcastId))
                        },
                        onEpisodePlayClick = { episode ->
                            mainViewModel.play(episode)
                        },
                        onEpisodeDownloadClick = { episode ->
                            mainViewModel.downloadEpisode(episode)
                        },
                        onCancelDownloadClick = { episodeId ->
                            mainViewModel.cancelDownload(episodeId)
                        },
                        onDeleteDownloadClick = { episodeId ->
                            mainViewModel.deleteDownload(episodeId)
                        },
                        downloadProgressMap = downloadProgress
                    )
                }
                composable(Screen.Search.route) {
                    SearchScreen(
                        onPodcastClick = { podcastId ->
                            navController.navigate(Screen.PodcastDetail.createRoute(podcastId))
                        }
                    )
                }
                composable(Screen.Downloads.route) {
                    DownloadsScreen(
                        onEpisodePlayClick = { episode ->
                            mainViewModel.play(episode)
                        }
                    )
                }
                composable(
                    route = Screen.PodcastDetail.route,
                    arguments = listOf(navArgument("podcastId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val podcastId = backStackEntry.arguments?.getLong("podcastId") ?: 0L
                    PodcastDetailScreen(
                        podcastId = podcastId,
                        onBackClick = { navController.popBackStack() },
                        onEpisodePlayClick = { episode ->
                            mainViewModel.play(episode)
                        },
                        onEpisodeDownloadClick = { episode ->
                            mainViewModel.downloadEpisode(episode)
                        },
                        onCancelDownloadClick = { episodeId ->
                            mainViewModel.cancelDownload(episodeId)
                        },
                        onDeleteDownloadClick = { episodeId ->
                            mainViewModel.deleteDownload(episodeId)
                        },
                        downloadProgressMap = downloadProgress
                    )
                }
                composable(Screen.NowPlaying.route) {
                    NowPlayingScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            if (showMiniPlayer) {
                val episode = playbackState.currentEpisode!!
                val progress = if (playbackState.durationMs > 0) {
                    playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()
                } else {
                    0f
                }
                MiniPlayer(
                    episodeTitle = episode.title,
                    podcastTitle = "",
                    artworkUrl = episode.artworkUrl,
                    isPlaying = playbackState.isPlaying,
                    isBuffering = playbackState.isBuffering,
                    progress = progress,
                    onSkipBackwardClick = { mainViewModel.skipBackward() },
                    onPlayPauseClick = { mainViewModel.togglePlayPause() },
                    onSkipForwardClick = { mainViewModel.skipForward() },
                    onClick = { navController.navigate(Screen.NowPlaying.route) }
                )
            }
        }
    }
}
