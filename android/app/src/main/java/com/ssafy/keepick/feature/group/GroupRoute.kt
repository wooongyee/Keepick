package com.ssafy.keepick.feature.group

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ssafy.keepick.R

@Composable
fun GroupRoute(
    onNavigateToTierMaker : (albumId: String) -> Unit,
) {
    val context = LocalContext.current

    val groupName = "팀 열우감"

    val displayMembers = remember(context) {
        listOf(
            DummyGroupMember(1, R.drawable.dummy_profile, "김싸피", true),
            DummyGroupMember(2, R.drawable.dummy_profile, "김싸피", true),
            DummyGroupMember(3, R.drawable.dummy_profile, "김싸피", false),
            DummyGroupMember(4, R.drawable.dummy_profile, "김싸피", true),
            DummyGroupMember(5, R.drawable.dummy_profile, "김싸피", true),
        ).map {
            DisplayGroupMember(
                id = it.id,
                profileImageUrl = it.profileImageUrl.toDrawableUri(context),
                name = it.name,
                isOnline = it.isOnline
            )
        }
    }

    // --- 각 탭에 맞는 별도의 데이터 리스트 생성 ---
    val timelineAlbums = remember(context) {
        (1..5).map {
            DummyGroupAlbum(it, R.drawable.dummy_album, "타임라인 앨범 ${it}", 24, "2025.06.29", "아빠님이 만듦")
        }.map { it.toDisplayModel(context) }
    }
    val tierAlbums = remember(context) {
        (1..2).map {
            DummyGroupAlbum(it, R.drawable.dummy_album, "티어 앨범 ${it}", 15, "2025.07.15", "엄마님이 만듦")
        }.map { it.toDisplayModel(context) }
    }
    val highlightAlbums = remember(context) {
        (1..8).map {
            DummyGroupAlbum(it, R.drawable.dummy_album, "하이라이트 앨범 ${it}", 30, "2025.08.01", "내가 만듦")
        }.map { it.toDisplayModel(context) }
    }

    GroupScreen(
        groupName = groupName,
        members = displayMembers,
        // --- 생성한 데이터 리스트들을 GroupScreen으로 전달 ---
        timelineAlbums = timelineAlbums,
        tierAlbums = tierAlbums,
        highlightAlbums = highlightAlbums,
        onTimelineAlbumClick = { /*TODO*/ },
        onTierAlbumClick = {
            onNavigateToTierMaker(it)
        },
        onHighlightAlbumClick = { /*TODO*/ },
        onAddClick = { /*TODO*/ },
        onVideoClick = { /*TODO*/ }
    )
}


// --- 원본 데이터 모델 ---
private data class DummyGroupMember(val id: Int, val profileImageUrl: Int, val name: String, val isOnline: Boolean)
private data class DummyGroupAlbum(val id: Int, val thumbnailUrl: Int, val title: String, val photoCount: Int, val date: String, val creator: String) {
    // --- UI 모델로 변환하는 확장 함수 추가 ---
    fun toDisplayModel(context: Context): DisplayGroupAlbum {
        return DisplayGroupAlbum(
            id = this.id,
            thumbnailUrl = this.thumbnailUrl.toDrawableUri(context),
            title = this.title,
            photoCount = this.photoCount,
            date = this.date,
            creator = this.creator
        )
    }
}

// --- UI에 직접 표시될, 가공이 끝난 데이터 모델 ---
data class DisplayGroupMember(val id: Int, val profileImageUrl: String, val name: String, val isOnline: Boolean)
data class DisplayGroupAlbum(val id: Int, val thumbnailUrl: String, val title: String, val photoCount: Int, val date: String, val creator: String)

private fun Int.toDrawableUri(context: Context): String {
    return Uri.parse("android.resource://${context.packageName}/$this").toString()
}
