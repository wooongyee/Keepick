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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

    val pagerState = rememberPagerState(initialPage = state.initialPage) { state.allImages.size }
    val thumbnailListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val image = state.allImages[pageIndex]
                with(sharedTransitionScope) {
                    AsyncImage(
                        model = image.imageUrl,
                        contentDescription = "상세 이미지",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedElement( // 히어로 애니메이션 적용
                                state = rememberSharedContentState(key = "image-${image.id}"),
                                animatedVisibilityScope = animatedContentScope
                            )
                    )
                }
            }

            // 상단 '닫기' 버튼
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.White)
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
