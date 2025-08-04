package com.ssafy.keepick.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ssafy.keepick.feature.login.LoginRoute
import com.ssafy.keepick.feature.main.MainScreen

@Composable
fun KeepickNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoute.Main.route
    ) {
        composable(route = NavRoute.Login.route) {
            LoginRoute(
                onNavigateToHome = {
                    navController.navigate(NavRoute.Main.route) {
                        popUpTo(NavRoute.Login.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(route = NavRoute.Main.route) {
            MainScreen(
                onNavigateToLogin = {
                    navController.navigate(NavRoute.Login.route) {
                        popUpTo(NavRoute.Main.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
