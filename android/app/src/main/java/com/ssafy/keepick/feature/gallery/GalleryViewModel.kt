package com.ssafy.keepick.feature.gallery

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.keepick.core.domain.model.Resource
import com.ssafy.keepick.core.domain.photo.PhotoRepository
import com.ssafy.keepick.feature.gallery.model.GalleryImage
import com.ssafy.keepick.feature.gallery.model.ImageStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository // Repository 주입
) : ViewModel(), ContainerHost<GalleryState, GallerySideEffect> {

    override val container = container<GalleryState, GallerySideEffect>(GalleryState())

    // 그룹별 이미지 로드 함수 추가 (현재는 시뮬레이션)
    fun loadImagesForGroup(groupName: String) = intent {
        reduce { state.copy(isLoading = true, allImages = emptyList(), selectedTag = "전체") }
        delay(500) // 네트워크 요청 시뮬레이션

        // 실제로는 여기서 groupName(또는 ID)을 사용하여 API를 호출해야 합니다.
        val newImages = when (groupName) {
            "SSAFY 13기 D207" -> (1..10).map {
                GalleryImage(UUID.randomUUID().toString(), "https://picsum.photos/id/${10 + it}/200", "2025.08.10", listOf("#인물", "#스터디"), ImageStatus.COMPLETE)
            }
            "가족방" -> (1..5).map {
                GalleryImage(UUID.randomUUID().toString(), "https://picsum.photos/id/${20 + it}/200", "2025.08.09", listOf("#풍경", "#여행"), ImageStatus.COMPLETE)
            }
            else -> (1..20).map {
                GalleryImage(UUID.randomUUID().toString(), "https://picsum.photos/id/${30 + it}/200", "2025.08.08", listOf("#음식", "#도시"), ImageStatus.COMPLETE)
            }
        }
        reduce { state.copy(isLoading = false, allImages = newImages) }
        postSideEffect(GallerySideEffect.ShowToast("$groupName 갤러리를 불러왔습니다."))
    }

    private fun loadInitialImages() = intent {
        photoRepository.getRandomPhotos(size = 10).onEach { resource ->
            when (resource) {
                is Resource.Success -> {
                    val images = resource.data.map { photo ->
                        GalleryImage(
                            id = photo.id.toString(),
                            imageUrl = photo.thumbnailUrl, // 썸네일 URL 사용
                            date = photo.takenAt,
                            tags = listOf("#서버", "#랜덤"), // 태그는 필요시 API 응답에 추가
                            status = ImageStatus.COMPLETE
                        )
                    }
                    Log.d("GalleryViewModel", "Loaded images: $images")
                    reduce { state.copy(allImages = images) }
                }
                is Resource.Failure -> {
                    postSideEffect(GallerySideEffect.ShowToast("이미지 로딩 실패: ${resource.errorMessage}"))
                }
                is Resource.Loading -> {
                    // TODO: 로딩 상태 UI 표시 (예: 스켈레톤 UI, 전체 프로그레스 바)
                }
            }
        }.launchIn(viewModelScope)
    }
    fun addImagesToGallery(uris: List<Uri>) = intent {
        val newImages = uris.map { uri ->
            GalleryImage(
                id = UUID.randomUUID().toString(),
                imageUrl = uri.toString(),
                date = "방금 전",
                tags = emptyList(),
                status = ImageStatus.UPLOADING // 1. UPLOADING 상태로 먼저 추가
            )
        }
        reduce { state.copy(allImages = newImages + state.allImages) }

        // 2. 백그라운드에서 업로드 및 분석 시뮬레이션
        viewModelScope.launch {
            startUploadAndAnalysis(newImages)
        }
    }

    private suspend fun startUploadAndAnalysis(images: List<GalleryImage>) = intent {
        // 3. 실제 업로드 로직 (여기서는 delay로 시뮬레이션)
        images.forEach { image ->
            delay(1000) // 업로드 시간 시뮬레이션
            reduce {
                state.copy(allImages = state.allImages.map {
                    if (it.id == image.id) it.copy(status = ImageStatus.ANALYZING) else it
                })
            }
        }

        // 4. AI 분석 시작
        reduce {
            state.copy(
                isAnalyzing = true,
                totalImageCount = images.size,
                analyzedImageCount = 0,
                analysisProgress = 0f
            )
        }

        for (i in images.indices) {
            delay(1500) // AI 분석 시간 시뮬레이션
            val analyzedImage = images[i]
            reduce {
                val newAnalyzedCount = state.analyzedImageCount + 1
                state.copy(
                    analyzedImageCount = newAnalyzedCount,
                    analysisProgress = newAnalyzedCount.toFloat() / state.totalImageCount,
                    allImages = state.allImages.map {
                        if (it.id == analyzedImage.id) {
                            it.copy(status = ImageStatus.COMPLETE, tags = listOf("#AI자동태그", "#분석완료"))
                        } else it
                    }
                )
            }
        }

        reduce { state.copy(isAnalyzing = false) }
        postSideEffect(GallerySideEffect.ShowToast("${images.size}개 사진 분석 완료!"))
    }


    fun onTagSelect(tag: String) = intent {
        reduce { state.copy(selectedTag = tag, isSelectionMode = false, selectedImageIds = emptyList()) }
    }

    fun onImageClick(imageId: String) = intent {
        if (state.isSelectionMode) {
            val newSelection = state.selectedImageIds.toMutableList()
            if (newSelection.contains(imageId)) newSelection.remove(imageId)
            else newSelection.add(imageId)
            reduce { state.copy(selectedImageIds = newSelection, isSelectionMode = newSelection.isNotEmpty()) }
        } else {
            postSideEffect(GallerySideEffect.NavigateToImageDetail(imageId))
        }
    }

    fun onImageLongClick(imageId: String) = intent {
        if (!state.isSelectionMode) {
            reduce { state.copy(isSelectionMode = true, selectedImageIds = listOf(imageId)) }
        }
    }

    fun onClearSelection() = intent {
        reduce { state.copy(isSelectionMode = false, selectedImageIds = emptyList()) }
    }

    fun onCreateAlbumClick() = intent {
        if (state.selectedImageIds.isNotEmpty()) {
            postSideEffect(GallerySideEffect.NavigateToCreateAlbum(state.selectedImageIds))
            onClearSelection()
        }
    }

    fun onDeleteSelectedImages() = intent {
        val remainingImages = state.allImages.filterNot { it.id in state.selectedImageIds }
        reduce {
            state.copy(
                allImages = remainingImages,
                selectedImageIds = emptyList(),
                isSelectionMode = false
            )
        }
        postSideEffect(GallerySideEffect.ShowToast("${state.selectedImageIds.size}개 사진 삭제 완료"))
    }

    fun onAiIconClick() = intent {

    }
}
