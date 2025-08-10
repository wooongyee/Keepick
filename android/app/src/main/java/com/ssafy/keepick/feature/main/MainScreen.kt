package com.ssafy.keepick.feature.main

import MypageTopBar
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ssafy.keepick.app.navigation.BottomNavItem
import com.ssafy.keepick.app.navigation.NavRoute
import com.ssafy.keepick.core.ui.RequestStoragePermissionOnStart
import com.ssafy.keepick.core.ui.rememberLocalImages
import com.ssafy.keepick.feature.gallery.GalleryRoute
import com.ssafy.keepick.feature.gallery.GalleryViewModel
import com.ssafy.keepick.feature.gallery.components.AiAnalysisInfoMenu
import com.ssafy.keepick.feature.gallery.components.GalleryTopBar
import com.ssafy.keepick.feature.gallery.components.PhotoPickerBottomSheet
import com.ssafy.keepick.feature.gallery.components.TagFilterSheetContent
import com.ssafy.keepick.feature.gallery.similarphotos.SimilarPhotosRoute
import com.ssafy.keepick.feature.group.GroupRoute
import com.ssafy.keepick.feature.group.components.GroupDropdownMenu
import com.ssafy.keepick.feature.group.components.GroupTopBar
import com.ssafy.keepick.feature.group.createGroup.CreateGroupBottomSheet
import com.ssafy.keepick.feature.group.createGroup.CreateGroupStep
import com.ssafy.keepick.feature.groupchat.LocalGroupChatViewModel
import com.ssafy.keepick.feature.home.HomeRoute
import com.ssafy.keepick.feature.home.components.HomeTopBar
import com.ssafy.keepick.feature.mypage.MyPageRoute

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToLogin: () -> Unit,
    onNavigateToTierAlbum: (String) -> Unit,
    onNavigateToTimelineAlbum: (String) -> Unit,
    onNavigateToTierMaker: (String) -> Unit,
    onNavigateToImageDetail: (sharedKey: String, imageId: String, clusterId: String?) -> Unit,
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val groupChatViewModel = LocalGroupChatViewModel.current
    val currentGroupId = "team-yeol-woo-gam-id" // 실제 그룹 ID

    var isMenuExpanded by remember { mutableStateOf(false) }
    var currentGroupName by remember { mutableStateOf("키픽 공식 그룹") }
    val groups = listOf("키픽 공식 그룹", "SSAFY 13기 D207", "가족방", "스터디 그룹")

    var showCreateGroupSheet by remember { mutableStateOf(false) }
    var showPhotoPickerSheet by remember { mutableStateOf(false) }
    var showAiMenu by remember { mutableStateOf(false) }
    var showTagFilterSheet by remember { mutableStateOf(false) }

    val galleryViewModel: GalleryViewModel = hiltViewModel()
    val galleryState by galleryViewModel.container.stateFlow.collectAsState()

    val localImages = rememberLocalImages()

    val createGroupSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    val tagFilterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var createGroupStep by remember { mutableStateOf(CreateGroupStep.INPUT_INFO) }
    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.Group,
        BottomNavItem.Gallery,
        BottomNavItem.MyPage
    )

    RequestStoragePermissionOnStart()

    // 초기 그룹 이미지 로드
    LaunchedEffect(Unit) {
        galleryViewModel.loadImagesForGroup(currentGroupName)
    }

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
                        onVideoClick = {
                            groupChatViewModel.startGroupChat(currentGroupId)
                        }
                    )
                }
                BottomNavItem.MyPage.route -> {
                    MypageTopBar(
                        onLogoutClick = onNavigateToLogin, // 로그아웃 콜백 연결
                        onSettingsClick = { /* TODO: 설정 화면 이동 */ }
                    )
                }
                BottomNavItem.Gallery.route, NavRoute.SimilarPhotos.route -> {
                    GalleryTopBar(
                        groupName = currentGroupName,
                        onGroupSelectClick = { isMenuExpanded = !isMenuExpanded },
                        onAiIconClick = { showAiMenu = true },
                        onAddClick = { showPhotoPickerSheet = true }
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                bottomNavItems.forEach { screen ->
                    // '갤러리' 탭일 경우 현재 경로가 갤러리 탭 또는 유사한 사진 경로 중 하나라도 해당되면 선택된 것으로 처리
                    val isSelected = when (screen.route) {
                        BottomNavItem.Gallery.route -> {
                            currentDestination?.hierarchy?.any {
                                it.route == BottomNavItem.Gallery.route || it.route == NavRoute.SimilarPhotos.route
                            } == true
                        }
                        else -> {
                            currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        }
                    }

                    NavigationBarItem(
                        selected = isSelected, // 수정된 로직을 적용
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
                composable(route = BottomNavItem.Group.route) {
                    GroupRoute(
                        // 그룹 화면에서는 '보기' 기능만 필요
                        onNavigateToTierMaker = onNavigateToTierAlbum, // 이름이 TierAlbum인 곳으로 감
                        onNavigateToTimelineAlbum = onNavigateToTimelineAlbum
                    )
                }
                composable(
                    route = BottomNavItem.Gallery.route
                ) {
                    GalleryRoute(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        viewModel = galleryViewModel,
                        onNavigateToTierMaker = onNavigateToTierMaker, // TierMaker로 감
                        onNavigateToTimelineAlbum = onNavigateToTimelineAlbum, // TimelineAlbum으로 감
                        onNavigateToImageDetail = onNavigateToImageDetail,
                        onNavigateToSimilarPhotos = {
                            bottomNavController.navigate(NavRoute.SimilarPhotos.route)
                        },
                        onUploadRequest = { showPhotoPickerSheet = true },
                        onOpenTagFilter = { showTagFilterSheet = true }
                    )
                }

                composable(route = NavRoute.SimilarPhotos.route) {
                    SimilarPhotosRoute(
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onNavigateToImageDetail = onNavigateToImageDetail,
                        onNavigateBack = { bottomNavController.popBackStack() }
                    )
                }

                composable(route = BottomNavItem.MyPage.route) {
                    MyPageRoute(
                        onNavigateToLogin = onNavigateToLogin,
                    )
                }
            }

            GroupDropdownMenu(
                expanded = isMenuExpanded,
                groups = groups,
                onGroupSelected = { selectedGroup ->
                    currentGroupName = selectedGroup
                    isMenuExpanded = false
                    galleryViewModel.loadImagesForGroup(selectedGroup)
                },
                onDismissRequest = {
                    isMenuExpanded = false
                }
            )
            AiAnalysisInfoMenu(
                expanded = showAiMenu,
                onDismissRequest = { showAiMenu = false },
                isAnalyzing = galleryState.isAnalyzing,
                analysisProgress = galleryState.analysisProgress // progress 값을 전달
            )
        }
        if (showCreateGroupSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCreateGroupSheet = false },
                sheetState = createGroupSheetState,
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
        // 커스텀 사진 선택 바텀시트 로직
        if (showPhotoPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPhotoPickerSheet = false },
                sheetState = createGroupSheetState,
            ) {
                PhotoPickerBottomSheet(
                    localImageUris = localImages, // 로드한 이미지 목록 전달
                    onDismissRequest = { showPhotoPickerSheet = false },
                    onImagesSelected = { selectedUris ->
                        // 선택 완료 시 ViewModel에 데이터 전달
                        galleryViewModel.addImagesToGallery(selectedUris)
                        showPhotoPickerSheet = false
                    }
                )
            }
        }
        if (showTagFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTagFilterSheet = false },
                sheetState = tagFilterSheetState // 생성한 State 연결
            ) {
                TagFilterSheetContent(
                    // 2. 시트의 최대 높이를 화면의 70%로 제한하는 Modifier 전달
                    modifier = Modifier.fillMaxHeight(0.7f),
                    initialSelectedTags = galleryState.selectedTags,
                    specialAiTags = galleryState.specialAiTags,
                    personTags = galleryState.personTags,
                    generalTags = galleryState.generalTags,
                    onApply = { newSelection ->
                        galleryViewModel.onApplyFilters(newSelection)
                        showTagFilterSheet = false
                    },
                )
            }
        }
    }
}
