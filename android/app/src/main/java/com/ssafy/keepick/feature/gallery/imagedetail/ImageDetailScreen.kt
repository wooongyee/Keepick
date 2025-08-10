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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.keepick.feature.gallery.model.GalleryImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ImageDetailScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedVisibilityScope,
    state: ImageDetailState,
    onNavigateBack: () -> Unit
) {
    val pagerState = rememberPagerState { state.allImages.size }
    val thumbnailListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.initialPage, state.allImages.size) {
        // 데이터 로딩이 완료되고 Pager가 준비되면(allImages.size > 0),
        // state.initialPage에 저장된 올바른 페이지로 즉시 이동합니다.
        if (state.allImages.isNotEmpty() && pagerState.currentPage != state.initialPage) {
            pagerState.scrollToPage(state.initialPage)
        }
    }

    // 페이저가 스크롤될 때, 썸네일 스트립도 함께 스크롤
    LaunchedEffect(pagerState.currentPage) {
        val targetIndex = pagerState.currentPage
        if (targetIndex >= 0 && thumbnailListState.layoutInfo.totalItemsCount > 0) {
            val visibleItems = thumbnailListState.layoutInfo.visibleItemsInfo
            if (visibleItems.isNotEmpty()) {
                val isVisible = visibleItems.any { it.index == targetIndex }
                if (!isVisible) {
                    thumbnailListState.animateScrollToItem(index = targetIndex)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            // 메인 이미지 페이저
            val keyPrefix = remember(state.sharedKey) {
                state.sharedKey.substringBefore('_', missingDelimiterValue = "")
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val image = state.allImages[pageIndex]

                val currentImageSharedKey = if (keyPrefix.isNotEmpty()) {
                    "${keyPrefix}_${image.id}"
                } else {
                    image.id // 만약을 위한 방어 코드
                }

                with(sharedTransitionScope) {
                    AsyncImage(
                        model = image.imageUrl,
                        contentDescription = "상세 이미지",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            // --- ▼ 핵심 수정 3: Pager의 각 페이지는 고유한 키를 사용해야 함 ---
                            .sharedElement(
                                state = rememberSharedContentState(key = currentImageSharedKey),
                                animatedVisibilityScope = animatedContentScope
                            )
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
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
                }

                // 클러스터 제목이 있을 경우 표시
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

            // 하단 썸네일 스트립
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    itemsIndexed(state.allImages, key = { _, item -> item.id }) { index, image ->
                        ThumbnailItem(
                            image = image,
                            isSelected = (pagerState.currentPage == index),
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
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
