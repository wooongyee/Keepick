package com.ssafy.keepick.app.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.ssafy.keepick.core.util.AppConstants
import com.ssafy.keepick.feature.groupchat.GroupChatScreen
import com.ssafy.keepick.feature.groupchat.GroupChatSideEffect
import com.ssafy.keepick.feature.groupchat.GroupChatViewModel
import com.ssafy.keepick.feature.groupchat.LocalGroupChatViewModel
import com.ssafy.keepick.feature.invite.InviteRoute
import com.ssafy.keepick.feature.login.LoginRoute
import com.ssafy.keepick.feature.main.MainScreen
import com.ssafy.keepick.feature.tier.tieralbum.TierAlbumRoute
import com.ssafy.keepick.feature.tier.tiermaker.TierMakerRoute
import com.ssafy.keepick.feature.timeline.TimelineAlbumRoute


@Composable
fun KeepickNavHost(
    isLoggedIn: Boolean = false,
    initialDeepLinkUri: String?
) {
    val navController = rememberNavController()
    val groupChatViewModel: GroupChatViewModel = hiltViewModel()

    val startDestination = if (isLoggedIn) NavRoute.Main.route else NavRoute.Login.route

    Log.d("NavHost", "KeepickNavHost: isLoggedIn=$isLoggedIn, initialDeepLinkUri=$initialDeepLinkUri")
    LaunchedEffect(isLoggedIn, initialDeepLinkUri) {
        if (isLoggedIn && initialDeepLinkUri != null) {
            val uri = Uri.parse(initialDeepLinkUri)
            if (uri.host == "invite" && uri.lastPathSegment != null) {
                val inviteCode = uri.lastPathSegment!!
                val internalRoute = NavRoute.Invite.createRoute(inviteCode)

                navController.navigate(internalRoute) {
                    popUpTo(NavRoute.Main.route) { inclusive = true }
                }
            }
        }
    }
    // TODO: GroupChatViewModel SideEffect 처리 (예: Toast 메시지 표시)
    LaunchedEffect(groupChatViewModel.container.sideEffectFlow) {
        groupChatViewModel.container.sideEffectFlow.collect { sideEffect ->
            when (sideEffect) {
                is GroupChatSideEffect.ShowToast -> {
                    // context.showToast(sideEffect.message)
                }
            }
        }
    }

    CompositionLocalProvider(LocalGroupChatViewModel provides groupChatViewModel) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination =
//                    NavRoute.Main.route
                    startDestination
            ) {
                composable(NavRoute.Login.route) {
                    LoginRoute(
                        onNavigateToHome = { // 로그인 성공 시
                            navController.handleInitialDeepLink(
                                uriString = initialDeepLinkUri,
                                isLoggedIn = true,
                                isFromLoginScreen = true
                            )
                        }
                    )
                }

                composable(NavRoute.Main.route) {
                    MainScreen(
                        onNavigateToLogin = { navController.navigate(NavRoute.Login.route) },
                        // 앨범 보기용 콜백
                        onNavigateToTierAlbum = { albumId ->
                            navController.navigate(NavRoute.TierAlbum.createRoute(albumId))
                        },
                        // 앨범 보기 및 생성용 콜백 (재사용)
                        onNavigateToTimelineAlbum = { albumId ->
                            navController.navigate(NavRoute.TimelineAlbum.createRoute(albumId))
                        },
                        // 앨범 생성용 콜백
                        onNavigateToTierMaker = { albumId ->
                            navController.navigate(NavRoute.TierMaker.createRoute(albumId))
                        }
                    )
                }

                composable(
                    route = NavRoute.Invite.route,
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "${AppConstants.FRONTEND_URL}/invite/{inviteCode}" },
                        navDeepLink { uriPattern = "keepick://invite/{inviteCode}" }
                    )
                ) { backStackEntry ->
                    val inviteCode = backStackEntry.arguments?.getString(NavRoute.Invite.INVITE_CODE_ARG)
                    InviteRoute(
                        inviteCode = inviteCode,
                        onNavigateToMain = {
                            navController.navigate(NavRoute.Main.route) {
                                popUpTo(NavRoute.Invite.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(route = NavRoute.TierAlbum.route) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getString(NavRoute.TierAlbum.ALBUM_ID_ARG)
                    TierAlbumRoute(
                        tierAlbumId = albumId ?: "unknown",
                        onNavigateBack = { navController.popBackStack() },
                        // 수정하기 버튼 클릭 시 TierMaker로 이동
                        onNavigateToEdit = { editAlbumId ->
                            navController.navigate(NavRoute.TierMaker.createRoute(editAlbumId))
                        }
                    )
                }

                composable(route = NavRoute.TierMaker.route) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getString(NavRoute.TierMaker.ALBUM_ID_ARG)
                    TierMakerRoute(
                        albumId = albumId ?: "unknown",
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(route = NavRoute.TimelineAlbum.route) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getString(NavRoute.TimelineAlbum.ALBUM_ID_ARG)
                    TimelineAlbumRoute(
                        timelineAlbumId = albumId ?: "unknown",
                        onNavigateBack = { navController.popBackStack() },
                    )
                }

            }
            GroupChatScreen()
        }
    }
}

