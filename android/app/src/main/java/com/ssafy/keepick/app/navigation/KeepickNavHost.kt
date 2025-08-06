package com.ssafy.keepick.app.navigation

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.ssafy.keepick.core.util.AppConstants
import com.ssafy.keepick.feature.invite.InviteRoute
import com.ssafy.keepick.feature.login.LoginRoute
import com.ssafy.keepick.feature.main.MainScreen
import com.ssafy.keepick.feature.tier.tieralbum.TierAlbumRoute
import com.ssafy.keepick.feature.tier.tiermaker.TierMakerRoute


@Composable
fun KeepickNavHost(
    isLoggedIn: Boolean = false,
    initialDeepLinkUri: String?
) {
    val navController = rememberNavController()

    // 시작점은 오직 '로그인 상태'에만 의존하여 결정됩니다.
    val startDestination = if (isLoggedIn) NavRoute.Main.route else NavRoute.Login.route

    Log.d("NavHost", "KeepickNavHost: isLoggedIn=$isLoggedIn, initialDeepLinkUri=$initialDeepLinkUri")
    LaunchedEffect(isLoggedIn, initialDeepLinkUri) {
        if (isLoggedIn && initialDeepLinkUri != null) {
            // 로그인 상태에서 딥링크 진입 시, 수동 파싱 후 이동
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

    NavHost(
        navController = navController,
        startDestination = startDestination
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
                onNavigateToTierAlbum = { albumId ->
                    navController.navigate(NavRoute.TierAlbum.createRoute(albumId))
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
    }
}

