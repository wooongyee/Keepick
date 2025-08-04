package com.ssafy.keepick.app.navigation

// AppNavRoute.kt
sealed class NavRoute(val route: String) {
    data object Login : NavRoute("login")
    data object Home : NavRoute("home")
}
