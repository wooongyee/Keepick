package com.ssafy.keepick.feature.highlight.emotion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.ssafy.keepick.feature.highlight.album.EmotionSection
import com.ssafy.keepick.feature.highlight.album.createDummyData

@Composable
fun EmotionDetailRoute(
    albumId: String,
    albumTitle: String, // [추가] 앨범 제목 파라미터
    emotionId: String,
    onNavigateBack: () -> Unit
) {
    val emotionSection = remember(albumId, emotionId) {
        getDummyEmotionSection(emotionId)
    }

    if (emotionSection != null) {
        EmotionDetailScreen(
            albumTitle = albumTitle, // [추가] 앨범 제목 전달
            section = emotionSection,
            onNavigateBack = onNavigateBack
        )
    } else {
        // TODO: 에러 처리 화면
    }
}

private fun getDummyEmotionSection(emotionId: String): EmotionSection? {
    return createDummyData().find { it.id == emotionId }
}
