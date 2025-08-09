// feature/gallery/GalleryRoute.kt (수정)
package com.ssafy.keepick.feature.gallery

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
    // ViewModel의 State를 구독
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    // ViewModel의 SideEffect를 구독하고 처리
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
        onTagSelect = viewModel::onTagSelect,
        onImageClick = viewModel::onImageClick,
        onImageLongClick = viewModel::onImageLongClick,
        onClearSelection = viewModel::onClearSelection,
        onCreateAlbumClick = viewModel::onCreateAlbumClick,
        onUploadClick = onUploadRequest // Empty View 및 TopBar에서 사용할 업로드 요청 콜백
    )
}
