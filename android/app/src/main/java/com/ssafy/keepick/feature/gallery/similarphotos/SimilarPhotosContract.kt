package com.ssafy.keepick.feature.gallery.similarphotos

import com.ssafy.keepick.feature.gallery.model.GalleryImage

data class SimilarPhotosState(
    val isAnalyzing: Boolean = false,
    val progress: Float = 0f,
    // null: 분석 전, emptyList: 분석 결과 없음, not-empty: 분석 결과 있음
    val photoClusters: List<PhotoCluster>? = null,
    val selectedPhotoIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
)

// 사진 클러스터 모델
data class PhotoCluster(
    val id: String, // 클러스터를 식별하는 고유 ID
    val images: List<GalleryImage>
)

// 분석 상태 Enum
enum class AnalysisStatus {
    NOT_STARTED, // 분석 시작 전
    IN_PROGRESS, // 분석 진행 중
    COMPLETE     // 분석 완료
}

sealed interface SimilarPhotosEvent {
    object OnRequestAnalysis : SimilarPhotosEvent
    data class OnTogglePhotoSelection(val photoId: String) : SimilarPhotosEvent
    object OnDeleteSelectedPhotos : SimilarPhotosEvent
    object OnClearSelection : SimilarPhotosEvent
    data class OnImageLongClick(val photoId: String) : SimilarPhotosEvent
}
