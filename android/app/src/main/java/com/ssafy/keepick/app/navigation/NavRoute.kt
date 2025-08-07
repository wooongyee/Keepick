package com.ssafy.keepick.app.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoute(val route: String) {
    data object Login : NavRoute("login?postLoginUri={postLoginUri}") {
        const val POST_LOGIN_URI_ARG = "postLoginUri"

        fun createRoute(postLoginUri: String?): String {
            val encodedUri = Uri.encode(postLoginUri ?: "")
            return "login?postLoginUri=$encodedUri"
        }
    }

    data object Main : NavRoute("main")
    data object Invite : NavRoute("invite/{inviteCode}") {
        const val INVITE_CODE_ARG = "inviteCode"
        fun createRoute(inviteCode: String): String = "invite/$inviteCode"
    }

    data object TierAlbum : NavRoute("tierAlbum/{albumId}") {
        const val ALBUM_ID_ARG = "albumId"
        fun createRoute(albumId: String) = "tierAlbum/$albumId"
    }

    object TierMaker : NavRoute("tierMaker/{albumId}") {
        const val ALBUM_ID_ARG = "albumId"
        fun createRoute(albumId: String) = "tierMaker/$albumId"
    }
    data object TimelineAlbum : NavRoute("timelineAlbum/{albumId}") {
        const val ALBUM_ID_ARG = "albumId"
        fun createRoute(albumId: String) = "timelineAlbum/$albumId"
    }
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
