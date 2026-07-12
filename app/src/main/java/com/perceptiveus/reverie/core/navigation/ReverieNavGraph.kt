package com.perceptiveus.reverie.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.perceptiveus.reverie.AppContainer
import com.perceptiveus.reverie.ReverieViewModelFactory
import com.perceptiveus.reverie.feature.home.HomeScreen
import com.perceptiveus.reverie.feature.home.HomeViewModel
import com.perceptiveus.reverie.feature.importmusic.ImportMusicScreen
import com.perceptiveus.reverie.feature.importmusic.ImportMusicViewModel
import com.perceptiveus.reverie.feature.library.LibraryScreen
import com.perceptiveus.reverie.feature.library.LibraryViewModel
import com.perceptiveus.reverie.feature.player.PlayerScreen
import com.perceptiveus.reverie.feature.player.PlayerViewModel
import com.perceptiveus.reverie.feature.premium.PremiumFeaturesScreen
import com.perceptiveus.reverie.feature.settings.SettingsScreen
import com.perceptiveus.reverie.feature.settings.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun ReverieNavGraph(
    navController: NavHostController,
    container: AppContainer,
    factory: ReverieViewModelFactory,
    modifier: Modifier = Modifier,
    startDestination: String = ReverieDestination.Home.route,
) {
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(ReverieDestination.Home.route) {
            val viewModel: HomeViewModel = viewModel(factory = factory)
            HomeScreen(
                viewModel = viewModel,
                onNavigateToImport = {
                    navController.navigate(ReverieDestination.ImportMusic.route)
                },
                onNavigateToLibrary = {
                    navController.navigate(ReverieDestination.Library.route) {
                        popUpTo(ReverieDestination.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToPremium = {
                    navController.navigate(ReverieDestination.PremiumFeatures.route)
                },
            )
        }

        composable(ReverieDestination.Library.route) {
            val viewModel: LibraryViewModel = viewModel(factory = factory)
            LibraryScreen(
                viewModel = viewModel,
                onPremiumFeatureClick = {
                    navController.navigate(ReverieDestination.PremiumFeatures.route)
                },
            )
        }

        composable(ReverieDestination.Player.route) {
            val viewModel: PlayerViewModel = viewModel(factory = factory)
            PlayerScreen(
                viewModel = viewModel,
                onNavigateToPremium = {
                    navController.navigate(ReverieDestination.PremiumFeatures.route)
                },
            )
        }

        composable(ReverieDestination.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(viewModel = viewModel)
        }

        composable(ReverieDestination.ImportMusic.route) {
            val viewModel: ImportMusicViewModel = viewModel(factory = factory)
            ImportMusicScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(ReverieDestination.PremiumFeatures.route) {
            PremiumFeaturesScreen(
                featureAccessChecker = container.featureAccessChecker,
                onNavigateBack = { navController.popBackStack() },
                onTogglePremiumForTesting = {
                    scope.launch {
                        val current = container.entitlementRepository.entitlements.value.isPremium
                        container.entitlementRepository.setPremiumForTesting(!current)
                    }
                },
            )
        }
    }
}
