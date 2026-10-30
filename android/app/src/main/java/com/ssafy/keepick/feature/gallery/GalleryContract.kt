package com.ssafy.keepick.feature.gallery

import com.ssafy.keepick.feature.gallery.model.GalleryImage
data class GalleryState(
    val groupName: String = "",
    val allImages: List<GalleryImage> = emptyList(),
    val specialAiTags: List<String> = listOf("흐린 사진"),
    val personTags: List<String> = emptyList(),
    val generalTags: List<String> = listOf("인물", "풍경", "음식", "바다", "산", "도시"),
    val selectedTags: List<String> = emptyList(),
    val selectedImageIds: List<String> = emptyList(),
    val isSelectionMode: Boolean = false,
    val isLoading: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isCreatingAlbum: Boolean = false,
    val analysisProgress: Float = 0f
) {
    val filteredImages: List<GalleryImage>
        get() {
            // 선택된 태그가 없으면 모든 이미지 반환
            if (selectedTags.isEmpty()) return allImages

            val cleanSelectedTags = selectedTags.map { it.removePrefix("#") }
            return allImages.filter { image ->
                // 이미지의 태그 중 하나라도 선택된 태그 목록에 포함되는지 확인
                image.tags.any { tag -> cleanSelectedTags.contains(tag.removePrefix("#")) }
            }
        }
}

sealed interface GallerySideEffect {
    // SideEffect의 이름을 최종 목적지에 맞게 명확히 변경
    data class NavigateToTierMaker(val newAlbumId: String) : GallerySideEffect
    data class NavigateToTimelineAlbum(val newAlbumId: String) : GallerySideEffect
    data class NavigateToImageDetail(val imageId: String) : GallerySideEffect
    data class ShowToast(val message: String) : GallerySideEffect
}
