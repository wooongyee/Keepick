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
    onOpenTagFilter: () -> Unit
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
        specialAiTags = state.specialAiTags,
        personTags = state.personTags,
        generalTags = state.generalTags,
        selectedTags = state.selectedTags, // 다중 선택 리스트 사용
        isSelectionMode = state.isSelectionMode,
        selectedImageIds = state.selectedImageIds,
        isLoading = state.isLoading,
        isAnalyzing = state.isAnalyzing,
        analysisProgress = state.analysisProgress,
        onToggleTagSelection = viewModel::onToggleTagSelection, // 다중 선택 이벤트 핸들러 사용
        onImageClick = viewModel::onImageClick,
        onImageLongClick = viewModel::onImageLongClick,
        onClearSelection = viewModel::onClearSelection,
        onCreateAlbumClick = viewModel::onCreateAlbumClick,
        onDeleteSelectedImages = viewModel::onDeleteSelectedImages,
        onFilterIconClick = onOpenTagFilter,
        onUploadClick = onUploadRequest
    )
}
