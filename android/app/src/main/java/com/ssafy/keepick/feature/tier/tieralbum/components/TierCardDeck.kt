package com.ssafy.keepick.feature.tier.tieralbum.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
import kotlin.math.absoluteValue
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TierCardDeck(
    photos: List<DisplayGroupAlbum>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("이 티어에는 사진이 없습니다.")
        }
        return
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 60.dp),
    ) { page ->
        val photo = photos[page]
        val pageOffset = (
            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            )

        val pageOffsetAbs = pageOffset.absoluteValue
        // 페이지가 멀어질수록 작아짐 (최소 75% 크기)
        val scale = 1f - (pageOffsetAbs * 0.25f)
        // 페이지가 멀어질수록 투명해짐 (최소 50% 투명도)
        val alpha = 1f - (pageOffsetAbs * 0.5f)
        // 좌우로 밀 때 Y축 기준 회전 (비스듬한 효과)
        val rotationY = pageOffset * -45f
        // 뒤로 갈 때 왼쪽 아래로 빠지는 듯한 효과 (곡선 이동)

        val cardModifier = Modifier
            .aspectRatio(0.7f) // 카드 비율
            .graphicsLayer {
                val calculatedTranslationX = pageOffset * (size.width / 4)
                val calculatedTranslationY = pageOffsetAbs * 40f * sin(pageOffsetAbs * 3.14f)

                this.translationX = calculatedTranslationX
                this.translationY = calculatedTranslationY
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
                this.cameraDistance = 12 * density
                this.rotationY = rotationY
            }
            // 렌더링 이슈 해결: graphicsLayer 변환 후 clip을 적용하여 깨짐 방지
            .clip(RoundedCornerShape(16.dp))

        PhotoCard(photo = photo, modifier = cardModifier)
    }
}

@Composable
private fun PhotoCard(
    photo: DisplayGroupAlbum,
    modifier: Modifier = Modifier
) {
    // Card의 기본 clip을 사용하지 않고, 상위 Modifier에서 제어
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
