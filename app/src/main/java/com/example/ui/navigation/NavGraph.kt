package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.ControlEditorScreen
import com.example.ui.screens.DeviceBenchmarkScreen
import com.example.ui.screens.DownloadManagerScreen
import com.example.ui.screens.GameRuntimeScreen
import com.example.ui.screens.GameSettingsScreen
import com.example.ui.screens.GamingEnvironmentSetupScreen
import com.example.ui.screens.ImportWorkflowDialog
import com.example.ui.screens.LibraryScreen
import com.example.viewmodel.MainViewModel

object Destinations {
    const val GAMING_SETUP = "gaming_setup"
    const val DOWNLOAD_MANAGER = "download_manager"
    const val LIBRARY = "library"
    const val GAME_SETTINGS = "game_settings/{gameId}"
    const val CONTROL_EDITOR = "control_editor/{gameId}"
    const val GAME_RUNTIME = "game_runtime/{gameId}"
    const val DEVICE_BENCHMARK = "device_benchmark"

    fun gameSettings(gameId: String) = "game_settings/$gameId"
    fun controlEditor(gameId: String) = "control_editor/$gameId"
    fun gameRuntime(gameId: String) = "game_runtime/$gameId"
}

@Composable
fun PulsePcNavGraph(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val envState by mainViewModel.gamingEnvironmentState.collectAsState()

    // Determine initial destination: First-launch goes to dedicated Gaming Environment Setup
    val initialDestination = if (envState.isFirstLaunch && !envState.isEnvironmentReady) {
        Destinations.GAMING_SETUP
    } else {
        Destinations.LIBRARY
    }

    NavHost(
        navController = navController,
        startDestination = initialDestination,
        modifier = modifier
    ) {
        composable(Destinations.GAMING_SETUP) {
            GamingEnvironmentSetupScreen(
                viewModel = mainViewModel,
                onSetupComplete = {
                    navController.navigate(Destinations.LIBRARY) {
                        popUpTo(Destinations.GAMING_SETUP) { inclusive = true }
                    }
                },
                onContinueInBackground = {
                    navController.navigate(Destinations.LIBRARY) {
                        popUpTo(Destinations.GAMING_SETUP) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.DOWNLOAD_MANAGER) {
            DownloadManagerScreen(
                viewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.LIBRARY) {
            LibraryScreen(
                viewModel = mainViewModel,
                onNavigateToGameSettings = { gameId ->
                    navController.navigate(Destinations.gameSettings(gameId))
                },
                onNavigateToPlayer = { gameId ->
                    navController.navigate(Destinations.gameRuntime(gameId))
                },
                onNavigateToDeviceBenchmark = {
                    navController.navigate(Destinations.DEVICE_BENCHMARK)
                },
                onNavigateToDownloadManager = {
                    navController.navigate(Destinations.DOWNLOAD_MANAGER)
                },
                onNavigateToGamingSetup = {
                    navController.navigate(Destinations.GAMING_SETUP)
                }
            )

            // Setup workflow dialog overlay when importing games
            ImportWorkflowDialog(
                viewModel = mainViewModel,
                onLaunchGame = { gameId ->
                    navController.navigate(Destinations.gameRuntime(gameId))
                }
            )
        }

        composable(
            route = Destinations.GAME_SETTINGS,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            GameSettingsScreen(
                gameId = gameId,
                viewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToControlEditor = { id ->
                    navController.navigate(Destinations.controlEditor(id))
                },
                onLaunchGame = { id ->
                    navController.navigate(Destinations.gameRuntime(id))
                }
            )
        }

        composable(
            route = Destinations.CONTROL_EDITOR,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            ControlEditorScreen(
                gameId = gameId,
                viewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Destinations.GAME_RUNTIME,
            arguments = listOf(navArgument("gameId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId") ?: ""
            GameRuntimeScreen(
                gameId = gameId,
                onExitGame = { navController.popBackStack() }
            )
        }

        composable(Destinations.DEVICE_BENCHMARK) {
            DeviceBenchmarkScreen(
                viewModel = mainViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
