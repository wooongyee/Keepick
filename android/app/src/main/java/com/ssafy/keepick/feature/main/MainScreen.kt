package com.ssafy.keepick.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssafy.keepick.app.navigation.BottomNavItem
import com.ssafy.keepick.core.ui.RequestStoragePermissionOnStart
import com.ssafy.keepick.feature.gallery.GalleryScreen
import com.ssafy.keepick.feature.group.GroupRoute
import com.ssafy.keepick.feature.group.components.GroupDropdownMenu
import com.ssafy.keepick.feature.group.components.GroupTopBar
import com.ssafy.keepick.feature.group.createGroup.CreateGroupBottomSheet
import com.ssafy.keepick.feature.group.createGroup.CreateGroupStep
import com.ssafy.keepick.feature.home.HomeRoute
import com.ssafy.keepick.feature.home.components.HomeTopBar
import com.ssafy.keepick.feature.mypage.MypageScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToLogin: () -> Unit,
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var isMenuExpanded by remember { mutableStateOf(false) }
    var currentGroupName by remember { mutableStateOf("키픽 공식 그룹") }
    val groups = listOf("키픽 공식 그룹", "SSAFY 13기 D207", "가족방", "스터디 그룹")

    var showCreateGroupSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    var createGroupStep by remember { mutableStateOf(CreateGroupStep.INPUT_INFO) }
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Group,
        BottomNavItem.Gallery,
        BottomNavItem.MyPage
    )

    RequestStoragePermissionOnStart()

    Scaffold(
        topBar = {
            when (currentDestination?.route) {
                BottomNavItem.Home.route -> {
                    HomeTopBar(onNotificationClick = { /* TODO: 알림 화면으로 이동 */ })
                }

                BottomNavItem.Group.route -> {
                    GroupTopBar(
                        groupName = currentGroupName,
                        onBackClick = { /* 최상위 화면에서는 보통 사용하지 않음 */ },
                        onGroupSelectClick = { isMenuExpanded = !isMenuExpanded },
                        onAddClick = {
                            createGroupStep = CreateGroupStep.INPUT_INFO
                            showCreateGroupSheet = true
                        },
                        onVideoClick = { /* TODO: 영상통화 */ }
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
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
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = Color.Black,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentDestination?.route == BottomNavItem.Group.route) {
                FloatingActionButton(onClick = { /* TODO: 카메라/비디오 기능 실행 */ }) {
                    Icon(imageVector = Icons.Default.Videocam, contentDescription = "영상통화")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(route = BottomNavItem.Home.route) { HomeRoute() }
                composable(route = BottomNavItem.Group.route) { GroupRoute() }
                composable(route = BottomNavItem.Gallery.route) { GalleryScreen() }
                composable(route = BottomNavItem.MyPage.route) { MypageScreen() }
            }

            GroupDropdownMenu(
                expanded = isMenuExpanded,
                groups = groups,
                onGroupSelected = { selectedGroup ->
                    currentGroupName = selectedGroup
                    isMenuExpanded = false
                },
                onDismissRequest = {
                    isMenuExpanded = false
                }
            )
        }
        if (showCreateGroupSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCreateGroupSheet = false },
                sheetState = sheetState,
                dragHandle = null // 2. 기본 드래그 핸들을 완전히 제거합니다.
            ) {
                CreateGroupBottomSheet(
                    currentStep = createGroupStep,
                    onDismissRequest = { showCreateGroupSheet = false },
                    onNavigateToPreviousStep = { // 3. 뒤로가기 콜백 연결
                        createGroupStep = CreateGroupStep.INPUT_INFO
                    },
                    onNavigateToNextStep = {
                        createGroupStep = CreateGroupStep.INVITE_FRIENDS
                    },
                    onCreateGroup = {
                        showCreateGroupSheet = false
                        // TODO: 실제 그룹 생성 API 호출 로직
                    }
                )
            }
        }
    }
}
