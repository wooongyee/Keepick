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
    private val photoRepository: PhotoRepository
) : ViewModel(), ContainerHost<GalleryState, GallerySideEffect> {

    override val container = container<GalleryState, GallerySideEffect>(GalleryState())

    // 그룹별 이미지 로드 함수 추가 (현재는 시뮬레이션)
    fun loadImagesForGroup(groupName: String) = intent {
        // 그룹 변경 시, 필터 상태(selectedTags)를 초기화하도록 수정
        reduce { state.copy(isLoading = true, allImages = emptyList(), selectedTags = emptyList()) }
        delay(500) // 네트워크 요청 시뮬레이션

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
        // 로딩 완료 후, 새로운 태그를 즉시 갱신
        reduce { state.copy(isLoading = false, allImages = newImages) }
        refreshTags()
        postSideEffect(GallerySideEffect.ShowToast("$groupName 갤러리를 불러왔습니다."))
    }

    private fun loadInitialImages() = intent {
        photoRepository.getRandomPhotos(size = 10).onEach { resource ->
            when (resource) {
                is Resource.Success -> {
                    val images = resource.data.map { photo ->
                        GalleryImage(
                            id = photo.id.toString(),
                            imageUrl = photo.thumbnailUrl,
                            date = photo.takenAt,
                            tags = listOf("#서버", "#랜덤"),
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
                    // 로딩 상태 처리
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
                status = ImageStatus.UPLOADING
            )
        }
        reduce { state.copy(allImages = newImages + state.allImages) }

        viewModelScope.launch {
            startUploadAndAnalysis(newImages)
        }
    }

    private suspend fun startUploadAndAnalysis(images: List<GalleryImage>) = intent {
        images.forEach { image ->
            delay(200)
            reduce {
                state.copy(allImages = state.allImages.map {
                    if (it.id == image.id) it.copy(status = ImageStatus.ANALYZING) else it
                })
            }
        }

        reduce { state.copy(isAnalyzing = true, analysisProgress = 0f) }

        val totalAnalysisTime = 1000L + (images.size * 500L)
        var elapsedTime = 0L
        val updateInterval = 100L

        while (elapsedTime < totalAnalysisTime) {
            delay(updateInterval)
            elapsedTime += updateInterval
            val progress = (elapsedTime.toFloat() / totalAnalysisTime).coerceAtMost(1.0f)
            reduce { state.copy(analysisProgress = progress) }
        }

        val analyzedIds = images.map { it.id }
        reduce {
            state.copy(
                isAnalyzing = false,
                allImages = state.allImages.map {
                    if (it.id in analyzedIds) {
                        it.copy(status = ImageStatus.COMPLETE, tags = listOf("#풍경", "#바다", "#김민수").shuffled())
                    } else it
                }
            )
        }

        refreshTags()
        postSideEffect(GallerySideEffect.ShowToast("${images.size}개 사진 동시 분석 완료!"))
    }

    private fun refreshTags() = intent {
        val allTagsFromImages = state.allImages
            .flatMap { it.tags }
            .map { it.removePrefix("#") }
            .distinct()

        val knownPersonNames = listOf("김민수", "이영희")
        val personTags = allTagsFromImages.filter { it in knownPersonNames }

        val baseGeneralTags = listOf("인물", "풍경", "음식", "바다", "산", "도시")
        val specialTags = state.specialAiTags
        val newGeneralTags = allTagsFromImages.filter { it !in personTags && it !in specialTags && it != "분석완료" }

        reduce {
            state.copy(
                personTags = personTags.distinct(),
                generalTags = (baseGeneralTags + newGeneralTags).distinct()
            )
        }
    }

    fun onApplyFilters(tags: List<String>) = intent {
        reduce { state.copy(selectedTags = tags) }
    }
    fun onToggleTagSelection(tag: String) = intent {
        val currentSelection = state.selectedTags.toMutableList()
        val exclusiveGroup = listOf("유사한 사진", "흐린 사진")

        when (tag) {
            "전체" -> {
                reduce { state.copy(selectedTags = emptyList()) }
            }

            // '유사한 사진' 또는 '흐린 사진' 같은 단독 선택 태그를 클릭한 경우
            in exclusiveGroup -> {
                // 이미 해당 태그가 단독으로 선택된 상태이면 선택 해제, 아니면 해당 태그만 선택
                val newSelection = if (currentSelection == listOf(tag)) emptyList() else listOf(tag)
                reduce { state.copy(selectedTags = newSelection) }
            }

            // 그 외 다중 선택 가능한 일반 태그를 클릭한 경우
            else -> {
                // 기존에 선택된 단독 선택 태그가 있다면 제거
                currentSelection.removeAll(exclusiveGroup)

                if (currentSelection.contains(tag)) {
                    currentSelection.remove(tag)
                } else {
                    if (currentSelection.size < 5) {
                        currentSelection.add(tag)
                    } else {
                        postSideEffect(GallerySideEffect.ShowToast("필터는 최대 5개까지 선택할 수 있습니다."))
                    }
                }
                reduce { state.copy(selectedTags = currentSelection) }
            }
        }
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
        // UI에서 직접 처리하므로 ViewModel의 역할은 없음
    }
}
