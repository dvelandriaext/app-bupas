package co.com.coexito.bupas.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.com.coexito.bupas.core.designsystem.theme.ThemeMode
import co.com.coexito.bupas.presentation.auth.LoginRoute
import co.com.coexito.bupas.presentation.auth.RecoverPasswordRoute
import co.com.coexito.bupas.presentation.auth.SessionGuardState
import co.com.coexito.bupas.presentation.auth.SessionViewModel
import co.com.coexito.bupas.presentation.collection.FieldCollectionRoute
import co.com.coexito.bupas.presentation.orders.OrderDetailRoute
import co.com.coexito.bupas.presentation.orders.OrdersInboxRoute

@Composable
fun BupasNavHost(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeSelected: (ThemeMode) -> Unit = {},
    navController: NavHostController = rememberNavController(),
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val sessionGuardState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = BupasDestination.Login.route,
        enterTransition = BupasTransitions.enterTransition,
        exitTransition = BupasTransitions.exitTransition,
        popEnterTransition = BupasTransitions.popEnterTransition,
        popExitTransition = BupasTransitions.popExitTransition
    ) {
        composable(route = BupasDestination.Login.route) {
            LaunchedEffect(sessionGuardState) {
                if (sessionGuardState is SessionGuardState.Authenticated) {
                    navController.navigate(BupasDestination.OrdersInbox.route) {
                        popUpTo(BupasDestination.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            LoginRoute(
                themeMode = themeMode,
                onThemeModeSelected = onThemeModeSelected,
                onLoginSuccess = {
                    navController.navigate(BupasDestination.OrdersInbox.route) {
                        popUpTo(BupasDestination.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRecoverPassword = {
                    navController.navigate(BupasDestination.RecoverPassword.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = BupasDestination.RecoverPassword.route) {
            RecoverPasswordRoute(
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = BupasDestination.OrdersInbox.route) {
            LaunchedEffect(sessionGuardState) {
                if (sessionGuardState is SessionGuardState.Unauthenticated) {
                    navController.navigate(BupasDestination.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            val subtitle = when (val current = sessionGuardState) {
                is SessionGuardState.Authenticated ->
                    "${current.session.fullName} • ${current.session.cedi} (${current.session.role.displayName})"
                else -> "Rol: Conductor en Campo"
            }

            OrdersInboxRoute(
                driverSubtitle = subtitle,
                themeMode = themeMode,
                onThemeModeSelected = onThemeModeSelected,
                onNavigateToDetail = { numeroSolicitud ->
                    navController.navigate(BupasDestination.OrderDetail.createRoute(numeroSolicitud)) {
                        launchSingleTop = true
                    }
                },
                onSafeLogout = {
                    sessionViewModel.performSafeLogout {
                        navController.navigate(BupasDestination.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = BupasDestination.OrderDetail.route,
            arguments = listOf(
                navArgument("numeroSolicitud") { type = NavType.StringType }
            )
        ) {
            LaunchedEffect(sessionGuardState) {
                if (sessionGuardState is SessionGuardState.Unauthenticated) {
                    navController.navigate(BupasDestination.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            OrderDetailRoute(
                onBackClick = { navController.popBackStack() },
                onNavigateToFieldCollection = { numeroSolicitud ->
                    navController.navigate(BupasDestination.FieldCollection.createRoute(numeroSolicitud)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = BupasDestination.FieldCollection.route,
            arguments = listOf(
                navArgument("numeroSolicitud") { type = NavType.StringType }
            )
        ) {
            LaunchedEffect(sessionGuardState) {
                if (sessionGuardState is SessionGuardState.Unauthenticated) {
                    navController.navigate(BupasDestination.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            FieldCollectionRoute(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
