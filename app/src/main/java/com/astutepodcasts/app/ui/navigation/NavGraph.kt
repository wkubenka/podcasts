package com.astutepodcasts.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.astutepodcasts.app.ui.MainViewModel
import com.astutepodcasts.app.ui.components.MiniPlayer
import com.astutepodcasts.app.ui.downloads.DownloadsScreen
import com.astutepodcasts.app.ui.home.HomeScreen
import com.astutepodcasts.app.ui.nowplaying.NowPlayingScreen
import com.astutepodcasts.app.ui.podcastdetail.PodcastDetailScreen
import com.astutepodcasts.app.ui.search.SearchScreen

@Composable
fun AppNavGraph(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val playbackState by mainViewModel.playbackState.collectAsStateWithLifecycle()
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
                        }
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
                        onEpisodePlayClick = { }
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
                        }
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
