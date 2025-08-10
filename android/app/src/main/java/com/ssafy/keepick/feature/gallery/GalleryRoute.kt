// feature/gallery/GalleryRoute.kt (수정)
package com.ssafy.keepick.feature.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun GalleryRoute(
    viewModel: GalleryViewModel,
    onNavigateToTierMaker: (String) -> Unit,
    onNavigateToTimelineAlbum: (String) -> Unit,
    onNavigateToImageDetail: (String) -> Unit,
    onUploadRequest: () -> Unit,
    onOpenTagFilter: () -> Unit
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            // 변경된 SideEffect 이름에 맞게 처리
            is GallerySideEffect.NavigateToTierMaker -> onNavigateToTierMaker(sideEffect.newAlbumId)
            is GallerySideEffect.NavigateToTimelineAlbum -> onNavigateToTimelineAlbum(sideEffect.newAlbumId)
            is GallerySideEffect.NavigateToImageDetail -> onNavigateToImageDetail(sideEffect.imageId)
            is GallerySideEffect.ShowToast -> { /* ... */ }
        }
    }

    // GalleryScreen 호출부는 변경 없음
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
        onDeselectAll = viewModel::onDeselectAll,
        onClearSelection = viewModel::onClearSelection,
        onCreateTierAlbumClick = viewModel::onCreateTierAlbumClick,
        onCreateTimelineAlbumClick = viewModel::onCreateTimelineAlbumClick,
        onDeleteSelectedImages = viewModel::onDeleteSelectedImages,
        onFilterIconClick = onOpenTagFilter,
        onUploadClick = onUploadRequest,
    )
}
