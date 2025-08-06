// feature/tier/tieralbum/components/TierCardDeck.kt

package com.ssafy.keepick.feature.tier.tieralbum.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TierCardDeck(
    photos: List<DisplayGroupAlbum>,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("이 티어에는 사진이 없습니다.")
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { photos.size })

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 60.dp), // 양 옆 카드가 보이도록 패딩 조절
    ) { page ->
        val photo = photos[page]
        val pageOffset = (
            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            ).absoluteValue

        // 페이지 오프셋에 따라 카드에 시각 효과 적용
        val cardModifier = Modifier
            .fillMaxHeight()
            .aspectRatio(0.7f) // 카드 비율
            .graphicsLayer {
                // 현재 페이지가 아닌 카드들을 뒤로 보내고 작게 만듦
                translationX = size.width * pageOffset * 0.5f
                scaleY = 1f - (pageOffset * 0.2f)
                scaleX = 1f - (pageOffset * 0.2f)
                alpha = 1f - (pageOffset * 0.4f)
                // 카드가 넘어가는 듯한 회전 효과
                cameraDistance = 12 * density
                rotationY = pageOffset * -45f
            }

        PhotoCard(photo = photo, modifier = cardModifier)
    }
}

@Composable
private fun PhotoCard(
    photo: DisplayGroupAlbum,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 날짜 오버레이
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
            ) {
                Text(
                    text = photo.date,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
