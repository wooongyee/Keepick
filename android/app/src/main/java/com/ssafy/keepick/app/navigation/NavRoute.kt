package com.ssafy.keepick.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoute(val route: String) {
    data object Login : NavRoute("login")
    data object Main : NavRoute("main")
}

sealed class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    data object Home : BottomNavItem("홈", Icons.Default.Home, "home")
    data object Group : BottomNavItem("그룹", Icons.Default.Search, "group")
    data object Gallery : BottomNavItem("갤러리", Icons.Default.DateRange, "gallery")
    data object MyPage : BottomNavItem("마이페이지", Icons.Default.AccountCircle, "mypage")
}
