package com.ssafy.keepick.feature.highlight.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import org.orbitmvi.orbit.compose.collectAsState


@Composable
fun HighlightAlbumRoute(
    albumId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEmotionDetail: (albumId: String, albumTitle: String, emotionId: String) -> Unit,
    viewModel: HighlightAlbumViewModel = hiltViewModel()
) {
    val state by viewModel.collectAsState()

    val emotionSections = remember { createDummyData() }

    DisposableEffect(Unit) {
        viewModel.startShakeDetection()
        onDispose {
            viewModel.stopShakeDetection()
        }
    }

    val albumTitle =  "2025 제주도 하이라이트"
    HighlightAlbumScreen(
        albumId = albumId,
        albumTitle = albumTitle,
        emotionSections = emotionSections,
        state = state, // [수정] state 객체 전체를 전달
        onNavigateBack = onNavigateBack,
        // [수정] onEmotionCardClick 호출 시 albumTitle 전달
        onEmotionCardClick = { emoId -> onNavigateToEmotionDetail(albumId, albumTitle, emoId) },
        onGiftBoxOpened = viewModel::onGiftBoxOpened // [추가] 선물상자 열기 이벤트 핸들러 전달
    )
}
