// feature/gallery/GalleryRoute.kt (수정)
package com.ssafy.keepick.feature.gallery

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun GalleryRoute(
    viewModel: GalleryViewModel,
    onNavigateToCreateAlbum: (List<String>) -> Unit,
    onNavigateToImageDetail: (String) -> Unit,
    onUploadRequest: () -> Unit,
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is GallerySideEffect.NavigateToCreateAlbum -> onNavigateToCreateAlbum(sideEffect.imageIds)
            is GallerySideEffect.NavigateToImageDetail -> onNavigateToImageDetail(sideEffect.imageId)
            is GallerySideEffect.ShowToast -> {
                // TODO: Toast 메시지 표시 로직 구현
            }
        }
    }

    GalleryScreen(
        images = state.filteredImages,
        allTags = state.allTags,
        selectedTag = state.selectedTag,
        isSelectionMode = state.isSelectionMode,
        selectedImageIds = state.selectedImageIds,
        isLoading = state.isLoading,
        isAnalyzing = state.isAnalyzing,
        analysisProgress = state.analysisProgress,
        analyzedImageCount = state.analyzedImageCount,
        totalImageCount = state.totalImageCount,
        onTagSelect = viewModel::onTagSelect,
        onImageClick = viewModel::onImageClick,
        onImageLongClick = viewModel::onImageLongClick,
        onClearSelection = viewModel::onClearSelection,
        onCreateAlbumClick = viewModel::onCreateAlbumClick,
        onDeleteSelectedImages = viewModel::onDeleteSelectedImages,
        onUploadClick = onUploadRequest,
    )
}

@Composable
private fun AiAnalysisInfoDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("AI 분석 기능 안내") },
        text = {
            Text(
                "Keepick의 AI는 다음과 같은 기능을 제공합니다:\n\n" +
                    "• 유사한 사진 분류\n" +
                    "• 흐리게 나온 사진 감지\n" +
                    "• 사진 속 인물, 풍경, 음식 등 자동 태그"
            )
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("확인")
            }
        }
    )
}
