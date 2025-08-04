package com.ssafy.keepick.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ssafy.keepick.feature.home.HomeRoute
import com.ssafy.keepick.feature.login.LoginRoute


@Composable
fun KeepickNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoute.Login.route
    ) {
        composable(route = NavRoute.Login.route) {
            LoginRoute(
                onNavigateToHome = {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(route = NavRoute.Home.route) {
            HomeRoute(
                onNavigateToLogin = {
                    navController.navigate(NavRoute.Login.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
