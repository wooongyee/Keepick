package com.ssafy.keepick.feature.home

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.R
import com.ssafy.keepick.core.designsystem.theme.KeepickTheme
import com.ssafy.keepick.feature.home.components.AlbumItem
import com.ssafy.keepick.feature.home.components.GroupItem

@Composable
fun HomeScreen() {
    val context = LocalContext.current

    val displayGroups = remember(context) {
        listOf(
            DummyGroup(1, R.drawable.dummy_group, "가족 모임", "9명 - 엄마, 아빠, 동생 ...", listOf(R.drawable.dummy_profile, R.drawable.dummy_profile, R.drawable.dummy_profile)),
            DummyGroup(2, R.drawable.dummy_group, "Ssafy 동기들", "15명 - 김싸피, 이싸피 ...", listOf(R.drawable.dummy_profile, R.drawable.dummy_profile, R.drawable.dummy_profile)),
            DummyGroup(3, R.drawable.dummy_group, "디자인 스터디", "5명 - 박디자인, 최디자인 ...", listOf(R.drawable.dummy_profile, R.drawable.dummy_profile, R.drawable.dummy_profile))
        ).map {
            DisplayGroup(
                id = it.id,
                imageUrl = it.imageUrl.toDrawableUri(context),
                name = it.name,
                detail = it.detail,
                memberImageUrls = it.memberImageUrls.map { resId -> resId.toDrawableUri(context) }
            )
        }
    }

    val displayAlbums = remember(context) {
        (1..10).map {
            DummyAlbum(it, R.drawable.dummy_album, "제주도 여행 ${it}", it * 3)
        }.map {
            DisplayAlbum(
                id = it.id,
                thumbnailUrl = it.thumbnailUrl.toDrawableUri(context),
                title = it.title,
                photoCount = it.photoCount
            )
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("내가 속한 그룹", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(
            items = displayGroups,
            key = { group -> "group_${group.id}" },
            span = { GridItemSpan(maxLineSpan) }
        ) { group ->
            GroupItem(
                groupImageUrl = group.imageUrl,
                groupName = group.name,
                groupDetail = group.detail,
                memberImageUrls = group.memberImageUrls
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("내 그룹 앨범들", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(
            items = displayAlbums,
            key = { album -> "album_${album.id}" }
        ) { album ->
            AlbumItem(
                thumbnailUrl = album.thumbnailUrl,
                albumTitle = album.title,
                photoCount = album.photoCount
            )
        }
    }
}


@Preview(showBackground = true, name = "홈 화면 미리보기")
@Composable
fun HomeScreenPreview() {
    KeepickTheme {
        HomeScreen()
    }
}

// --- 원본 데이터 모델 ---
private data class DummyGroup(val id: Int, val imageUrl: Int, val name: String, val detail: String, val memberImageUrls: List<Int>)
private data class DummyAlbum(val id: Int, val thumbnailUrl: Int, val title: String, val photoCount: Int)

// --- UI에 직접 표시될, 가공이 끝난 데이터 모델 ---
private data class DisplayGroup(val id: Int, val imageUrl: String, val name: String, val detail: String, val memberImageUrls: List<String>)
private data class DisplayAlbum(val id: Int, val thumbnailUrl: String, val title: String, val photoCount: Int)

private fun Int.toDrawableUri(context: Context): String {
    return Uri.parse("android.resource://${context.packageName}/$this").toString()
}
