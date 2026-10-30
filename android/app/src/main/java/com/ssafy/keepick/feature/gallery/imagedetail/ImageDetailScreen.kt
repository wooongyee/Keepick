// In file: ImageDetailScreen.kt
package com.ssafy.keepick.feature.gallery.imagedetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.keepick.core.ui.detailScreenTransform
import com.ssafy.keepick.feature.gallery.model.GalleryImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ImageDetailScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedVisibilityScope,
    state: ImageDetailState,
    onNavigateBack: () -> Unit,
    onPageChanged: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.isLoading || state.initialPage == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            val pagerState = rememberPagerState(initialPage = state.initialPage) { state.allImages.size }
            val thumbnailListState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            var isReadyForInteraction by remember { mutableStateOf(false) }
            val density = LocalDensity.current

            // ▼ --- 핵심 수정 1: '초기화'와 '상호작용' 로직을 분리 --- ▼

            // 1. 초기화 전용 LaunchedEffect: 단 한 번 실행되어 UI를 준비시킵니다.
            LaunchedEffect(Unit) {
                // Pager가 초기 페이지에 완전히 자리 잡을 때까지 대기
                snapshotFlow { pagerState.isScrollInProgress }
                    .filter { !it }
                    .first()

                // 썸네일 목록을 애니메이션 없이 즉시 중앙으로 배치
                val viewportWidth = thumbnailListState.layoutInfo.viewportSize.width
                if (viewportWidth > 0) {
                    val thumbnailSizePx = with(density) { 64.dp.toPx() }
                    val centerOffset = (viewportWidth / 2 - thumbnailSizePx / 2).toInt()
                    thumbnailListState.scrollToItem(pagerState.currentPage, -centerOffset)
                }

                // 모든 준비가 끝나면 상호작용을 허용
                isReadyForInteraction = true
            }

            // 2. 상호작용 전용 LaunchedEffect: 초기화 완료 후 모든 실시간 상호작용을 처리합니다.
            LaunchedEffect(isReadyForInteraction) {
                if (!isReadyForInteraction) return@LaunchedEffect

                // الف. 페이지 변경 시 ViewModel에 상태 업데이트 요청
                snapshotFlow { pagerState.currentPage }
                    .distinctUntilChanged()
                    .collect { page ->
                        onPageChanged(page)
                    }
            }

            LaunchedEffect(isReadyForInteraction) {
                if (!isReadyForInteraction) return@LaunchedEffect

                // ب. Pager 스크롤과 썸네일 스크롤을 실시간으로 동기화
                snapshotFlow { pagerState.currentPage to pagerState.currentPageOffsetFraction }
                    .collect { (page, offset) ->
                        val viewportWidth = thumbnailListState.layoutInfo.viewportSize.width
                        if (viewportWidth > 0) {
                            val thumbnailSizePx = with(density) { 64.dp.toPx() }
                            val spacingPx = with(density) { 8.dp.toPx() }
                            val itemWidthPx = thumbnailSizePx + spacingPx
                            val centerOffset = (viewportWidth / 2 - thumbnailSizePx / 2).toInt()
                            val scrollAmount = (offset * itemWidthPx).roundToInt()
                            thumbnailListState.scrollToItem(page, -centerOffset + scrollAmount)
                        }
                    }
            }

            with(sharedTransitionScope) {
                // ▼ 핵심 수정 2: ViewModel이 내려준 state를 사용하여 동적 키 생성
                val dynamicSharedKey by remember(state.sharedKey, state.currentVisibleImageId) {
                    derivedStateOf {
                        val keyPrefix = state.sharedKey.substringBeforeLast('_', "")
                        val currentImageId = state.currentVisibleImageId // ViewModel이 제공하는 ID 사용

                        if (!keyPrefix.isNullOrEmpty() && !currentImageId.isNullOrEmpty()) {
                            "${keyPrefix}_${currentImageId}"
                        } else {
                            state.sharedKey // Fallback
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .sharedElement(
                            rememberSharedContentState(key = dynamicSharedKey),
                            animatedVisibilityScope = animatedContentScope,
                            boundsTransform = detailScreenTransform,
                        )
                    ,
                    userScrollEnabled = isReadyForInteraction
                ) { pageIndex ->
                    val image = state.allImages[pageIndex]
                    AsyncImage(
                        model = image.imageUrl,
                        contentDescription = "상세 이미지",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    // ▼ 복구: 썸네일 스크롤 중에도 뒤로가기 방지
                    if (!pagerState.isScrollInProgress && !thumbnailListState.isScrollInProgress) {
                        onNavigateBack()
                    }
                }) {
                    Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
                }

                state.clusterTitle?.let { title ->
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = state.allImages.isNotEmpty(),
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyRow(
                    state = thumbnailListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    userScrollEnabled = isReadyForInteraction
                ) {
                    itemsIndexed(state.allImages, key = { _, item -> item.id }) { index, image ->
                        ThumbnailItem(
                            image = image,
                            isSelected = (pagerState.currentPage == index),
                            onClick = {
                                if (isReadyForInteraction) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailItem(
    image: GalleryImage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = image.imageUrl,
            contentDescription = "썸네일",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
