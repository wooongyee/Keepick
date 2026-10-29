package com.ssafy.keepick.feature.tier.tieralbum

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ssafy.keepick.R
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
import com.ssafy.keepick.feature.tier.tiermaker.Tier

private fun Int.toDrawableUri(context: Context): String {
    return Uri.parse("android.resource://${context.packageName}/$this").toString()
}

@Composable
fun TierAlbumRoute(
    tierAlbumId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit, // TierMaker로 이동
) {
    val context = LocalContext.current

    // 실제로는 tierAlbumId를 사용해 ViewModel 등에서 데이터를 로드합니다.
    // 여기서는 기존 TierMaker의 더미 데이터를 활용하여 Mock 데이터를 생성합니다.
    val dummyTiers = remember {
        val photos = (1..25).map {
            DisplayGroupAlbum(
                id = it,
                thumbnailUrl = when {
                    it % 3 == 0 -> R.drawable.dummy_group.toDrawableUri(context)
                    it % 3 == 1 -> R.drawable.dummy_profile.toDrawableUri(context)
                    else -> R.drawable.dummy_album.toDrawableUri(context)
                },
                title = "추억 사진 $it",
                photoCount = 0,
                date = "2025.07.${(it % 30) + 1}",
                creator = "User ${it % 5}"
            )
        }
        listOf(
            Tier("S", "S", photos.slice(0..4)),
            Tier("A", "A", photos.slice(5..9)),
            Tier("B", "B", photos.slice(10..15)),
            Tier("C", "C", photos.slice(16..19)),
            Tier("D", "D", emptyList()), // 비어있는 티어 테스트
            Tier("unranked", "미분류", photos.slice(20..24))
        )
    }

    var tiers by remember { mutableStateOf(dummyTiers) }
    var selectedTierId by remember { mutableStateOf("S") }

    val selectedTier = tiers.find { it.id == selectedTierId }

    TierAlbumScreen(
        albumTitle = "제주도 여름휴가",
        allTiers = tiers.filter { it.id != "unranked" }, // 미분류는 제외하고 표시
        selectedTier = selectedTier,
        onTierSelected = { newTierId ->
            selectedTierId = newTierId
        },
        onNavigateBack = onNavigateBack,
        onEditClick = { onNavigateToEdit(tierAlbumId) },
        onCopyClick = { /* TODO: 앨범 복사 로직 */ }
    )
}
