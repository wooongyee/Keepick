package com.ssafy.keepick.feature.group

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.core.designsystem.theme.KeepickTheme
import com.ssafy.keepick.feature.group.components.GroupAlbumItem
import com.ssafy.keepick.feature.group.components.GroupAlbumTabView
import com.ssafy.keepick.feature.group.components.GroupMemberItem

@OptIn(ExperimentalFoundationApi::class) // stickyHeader를 사용하기 위해 추가
@Composable
fun GroupScreen(
    groupName: String,
    members: List<DisplayGroupMember>,
    timelineAlbums: List<DisplayGroupAlbum>,
    tierAlbums: List<DisplayGroupAlbum>,
    highlightAlbums: List<DisplayGroupAlbum>,
    onAddClick: () -> Unit,
    onVideoClick: () -> Unit,
) {
    // 탭 상태를 관리할 변수
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("타임라인 앨범", "티어 앨범", "하이라이트 앨범")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 그룹 정보 (상단 고정되지 않는 부분)
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "그룹 정보",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = members,
                        key = { member -> "member_${member.id}" }
                    ) { member ->
                        GroupMemberItem(
                            imageUrl = member.profileImageUrl,
                            name = member.name,
                            isOnline = member.isOnline
                        )
                    }
                }
            }
        }

        // 커스텀 탭 (스크롤 시 상단에 고정됨)
        stickyHeader {
            GroupAlbumTabView(
                selectedTabIndex = selectedTabIndex,
                tabs = tabTitles,
                onTabSelected = { newIndex -> selectedTabIndex = newIndex }
            )
        }
// --- selectedTabIndex에 따라 다른 리스트를 렌더링 ---
        when (selectedTabIndex) {
            0 -> items(
                items = timelineAlbums,
                key = { album -> "timeline_${album.id}" } // 고유한 키를 위해 접두사 추가
            ) { album ->
                GroupAlbumItem(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    thumbnailUrl = album.thumbnailUrl,
                    title = album.title,
                    photoCount = album.photoCount,
                    date = album.date,
                    creator = album.creator
                )
            }
            1 -> items(
                items = tierAlbums,
                key = { album -> "tier_${album.id}" }
            ) { album ->
                GroupAlbumItem(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    thumbnailUrl = album.thumbnailUrl,
                    title = album.title,
                    photoCount = album.photoCount,
                    date = album.date,
                    creator = album.creator
                )
            }
            2 -> items(
                items = highlightAlbums,
                key = { album -> "highlight_${album.id}" }
            ) { album ->
                GroupAlbumItem(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    thumbnailUrl = album.thumbnailUrl,
                    title = album.title,
                    photoCount = album.photoCount,
                    date = album.date,
                    creator = album.creator
                )
            }
        }
    }
}

// Preview도 수정된 파라미터에 맞게 업데이트
@Preview(showBackground = true, name = "그룹 화면 미리보기")
@Composable
private fun GroupScreenPreview() {
    KeepickTheme {
        val members = listOf(DisplayGroupMember(1, "", "김싸피", true))
        val albums = listOf(DisplayGroupAlbum(1, "", "미리보기용 앨범", 10, "2025.01.01", "미리보기"))
        GroupScreen(
            groupName = "팀 열우감",
            members = members,
            timelineAlbums = albums, // Preview에서는 동일 데이터 재사용 가능
            tierAlbums = emptyList(),
            highlightAlbums = albums.take(1),
            onAddClick = {},
            onVideoClick = {}
        )
    }
}
