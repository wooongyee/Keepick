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
    object ImageDetail : NavRoute("image_detail/{sharedKey}/{imageId}?clusterId={clusterId}") {
        const val SHARED_KEY_ARG = "sharedKey"
        const val IMAGE_ID_ARG = "imageId"
        const val CLUSTER_ID_ARG = "clusterId"

        fun createRoute(sharedKey: String, imageId: String, clusterId: String?): String {
            val baseRoute = "image_detail/$sharedKey/$imageId"
            return if (clusterId != null) "$baseRoute?clusterId=$clusterId" else baseRoute
        }
    }
    data object SimilarPhotos : NavRoute("similarPhotos")


    // 하이라이트 앨범 메인 경로
    data object HighlightAlbum : NavRoute("highlightAlbum/{albumId}") {
        const val ALBUM_ID_ARG = "albumId"
        fun createRoute(albumId: String) = "highlightAlbum/$albumId"
    }
    data object EmotionDetail : NavRoute("emotionDetail/{albumId}/{albumTitle}/{emotionId}") {
        const val ALBUM_ID_ARG = "albumId"
        const val ALBUM_TITLE_ARG = "albumTitle" // title을 위한 상수 추가
        const val EMOTION_ID_ARG = "emotionId"
        // [수정] createRoute 함수에 albumTitle 파라미터 추가
        fun createRoute(albumId: String, albumTitle: String, emotionId: String) = "emotionDetail/$albumId/$albumTitle/$emotionId"
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
