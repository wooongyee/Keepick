// feature/gallery/GalleryRoute.kt (수정)
package com.ssafy.keepick.feature.gallery

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryRoute(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: GalleryViewModel,
    onNavigateToTierMaker: (String) -> Unit,
    onNavigateToTimelineAlbum: (String) -> Unit,
    onNavigateToImageDetail: (sharedKey : String,imageId: String, clusterId: String?) -> Unit,
    onNavigateToSimilarPhotos: () -> Unit,
    onUploadRequest: () -> Unit,
    onOpenTagFilter: () -> Unit
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is GallerySideEffect.NavigateToTierMaker -> onNavigateToTierMaker(sideEffect.newAlbumId)
            is GallerySideEffect.NavigateToTimelineAlbum -> onNavigateToTimelineAlbum(sideEffect.newAlbumId)
            is GallerySideEffect.NavigateToImageDetail -> {
                val sharedKey = "gallery_${sideEffect.imageId}"
                onNavigateToImageDetail(sharedKey,sideEffect.imageId, null)
            }
            is GallerySideEffect.ShowToast -> { /* ... */ }
        }
    }

    // GalleryScreen 호출부는 변경 없음
    GalleryScreen(
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
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
        onToggleImageSelection = viewModel::onToggleImageSelection, // ViewModel의 함수 연결
        onNavigateToImageDetail = onNavigateToImageDetail, // 상위로부터 받은 내비게이션 콜백 연결
        onNavigateToSimilarPhotos = onNavigateToSimilarPhotos,
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
