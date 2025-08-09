package com.ssafy.keepick.feature.gallery

import com.ssafy.keepick.feature.gallery.model.GalleryImage

// 1. UI 상태를 정의하는 State 클래스
data class GalleryState(
    val allImages: List<GalleryImage> = emptyList(),
    val allTags: List<String> = listOf("전체", "#유사한 사진", "#흐린 사진", "#인물", "#풍경", "#음식", "#바다", "#산", "#도시"),
    val selectedTag: String = "전체",
    val selectedImageIds: List<String> = emptyList(),
    val isSelectionMode: Boolean = false,
) {
    // 필터링된 이미지 목록을 계산 속성으로 제공
    val filteredImages: List<GalleryImage>
        get() = if (selectedTag == "전체") {
            allImages
        } else {
            allImages.filter { it.tags.contains(selectedTag) }
        }
}

// 2. 일회성 이벤트를 정의하는 SideEffect sealed interface
sealed interface GallerySideEffect {
    data class NavigateToCreateAlbum(val imageIds: List<String>) : GallerySideEffect
    data class NavigateToImageDetail(val imageId: String) : GallerySideEffect
    data class ShowToast(val message: String) : GallerySideEffect
}
