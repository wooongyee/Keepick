package com.ssafy.keepick.app.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * 딥링크 URI를 분석하고, 현재 상태에 맞춰 적절한 화면으로 이동시키는 핵심 로직.
 *
 * @param uriString 앱 실행 시 전달받은 초기 딥링크 URI.
 * @param isLoggedIn 현재 사용자의 로그인 여부.
 * @param isFromLoginScreen 이 함수가 로그인 성공 직후에 호출되었는지 여부.
 *                          이 플래그에 따라 백스택 처리 방식이 달라집니다.
 */
internal fun NavController.handleInitialDeepLink(
    uriString: String?,
    isLoggedIn: Boolean,
    isFromLoginScreen: Boolean
) {
    // 처리할 URI가 없으면, 로그인 성공 후의 경우에만 메인으로 이동시킵니다.
    if (uriString == null) {
        if (isFromLoginScreen) navigateToMain()
        return
    }

    // URI 파싱 및 유효성 검사
    val uri = Uri.parse(uriString)
    if (uri.scheme != "keepick" || uri.host != "invite" || uri.lastPathSegment == null) {
        if (isFromLoginScreen) navigateToMain() // 처리 불가능한 딥링크 시 메인으로
        return
    }

    // 로그인 상태일 때만 내비게이션을 수행합니다.
    if (!isLoggedIn) return

    val inviteCode = uri.lastPathSegment!!
    val internalInviteRoute = NavRoute.Invite.createRoute(inviteCode)

    if (isFromLoginScreen) {
        navigateToMain() // 먼저 메인으로 이동하여 백스택 기반을 다지고
        navigate(internalInviteRoute) // 그 다음 초대 화면으로 이동합니다.
    } else {
        // 이미 로그인된 상태에서 온 경우: 현재 화면을 닫고 초대 화면으로 바로 이동합니다.
        navigate(internalInviteRoute) {
            // 이 경우 시작점은 Main이므로, Main 화면을 백스택에서 제거합니다.
            popUpTo(NavRoute.Main.route) { inclusive = true }
        }
    }
}

/**
 * 백스택을 모두 비우고 메인 화면으로 이동합니다.
 */
fun NavController.navigateToMain() {
    navigate(NavRoute.Main.route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

/**
 * 백스택을 모두 비우고 로그인 화면으로 이동합니다.
 */
fun NavController.navigateToLogin() {
    navigate(NavRoute.Login.createRoute(null)) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = true
        }
        launchSingleTop = true
    }
}
