package com.ssafy.keepick.feature.main

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssafy.keepick.app.navigation.BottomNavItem
import com.ssafy.keepick.feature.gallery.GalleryScreen
import com.ssafy.keepick.feature.group.GroupScreen
import com.ssafy.keepick.feature.home.HomeRoute
import com.ssafy.keepick.feature.home.components.KeepickTopAppBar
import com.ssafy.keepick.feature.mypage.MypageScreen

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // Scaffold가 material3로 변경됨
@Composable
fun MainScreen(
    onNavigateToLogin: () -> Unit
) {
    val bottomNavController = rememberNavController()
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Group,
        BottomNavItem.Gallery,
        BottomNavItem.MyPage
    )

    Scaffold(
        topBar = { KeepickTopAppBar() },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White // backgroundColor -> containerColor
            ) {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            bottomNavController.navigate(screen.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(imageVector = screen.icon, contentDescription = screen.title)
                        },
                        label = {
                            Text(screen.title)
                        },
                        // 색상 설정을 material3 방식으로 변경
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color.Black,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent // 선택 시 배경 인디케이터 투명 처리
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = BottomNavItem.Home.route) {
                HomeRoute(
                    onNavigateToLogin = onNavigateToLogin
                )
            }
            composable(route = BottomNavItem.Group.route) {
                GroupScreen() // 임시 스크린
            }
            composable(route = BottomNavItem.Gallery.route) {
                GalleryScreen() // 임시 스크린
            }
            composable(route = BottomNavItem.MyPage.route) {
                MypageScreen() // 임시 스크린
            }
        }
    }
}
