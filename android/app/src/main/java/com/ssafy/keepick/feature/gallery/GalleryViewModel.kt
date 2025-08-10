package com.ssafy.keepick.feature.gallery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.keepick.core.data.photo.ImageRepository
import com.ssafy.keepick.core.domain.photo.PhotoRepository
import com.ssafy.keepick.feature.gallery.model.GalleryImage
import com.ssafy.keepick.feature.gallery.model.ImageStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    init {
        // ViewModel이 처음 생성될 때, 초기 그룹에 대한 이미지 관찰을 시작합니다.
        // 이 로직은 한 번만 실행되므로, 화면을 갔다 와도 다시 실행되지 않습니다.
        loadImagesForGroup("키픽 공식 그룹")
    }

    /**
     * 그룹을 변경하거나 데이터를 새로고침할 때 호출됩니다.
     */
    fun loadImagesForGroup(groupName: String) = intent {
        // --- 핵심 수정 사항: 데이터가 이미 있는지 확인 ---
        // 만약 같은 그룹의 데이터를 이미 보고 있다면, UI를 막는 로딩을 실행하지 않습니다.
        // 대신 백그라운드에서 조용히 업데이트를 시도할 수 있습니다. (이 부분은 생략)
        if (state.groupName == groupName && !state.isLoading) {
            return@intent
        }

        // 새로운 그룹을 로드하는 경우, 로딩 상태로 전환
        reduce {
            state.copy(
                isLoading = true,
                groupName = groupName, // 상태에 현재 그룹 이름 기록
                allImages = emptyList(),
                selectedTags = emptyList()
            )
        }
        delay(500)

        // --- 핵심 수정 사항: ID를 예측 가능하고 안정적으로 생성 ---
        val newImages = when (groupName) {
            "SSAFY 13기 D207" -> (1..10).map {
                GalleryImage(
                    id = "ssafy-${it}", // UUID.randomUUID() 대신 고정된 ID 사용
                    imageUrl = "https://picsum.photos/id/${10 + it}/200",
                    date = "2025.08.10",
                    tags = listOf("#인물", "#스터디"),
                    status = ImageStatus.COMPLETE
                )
            }
            "가족방" -> (1..5).map {
                GalleryImage(
                    id = "family-${it}", // UUID.randomUUID() 대신 고정된 ID 사용
                    imageUrl = "https://picsum.photos/id/${20 + it}/200",
                    date = "2025.08.09",
                    tags = listOf("#풍경", "#여행"),
                    status = ImageStatus.COMPLETE
                )
            }
            else -> (1..20).map {
                GalleryImage(
                    id = "default-${it}", // UUID.randomUUID() 대신 고정된 ID 사용
                    imageUrl = "https://picsum.photos/id/${30 + it}/200",
                    date = "2025.08.08",
                    tags = listOf("#음식", "#도시"),
                    status = ImageStatus.COMPLETE
                )
            }
        }

        reduce { state.copy(isLoading = false, allImages = newImages) }
        ImageRepository.updateImages(newImages)
        refreshTags()
        postSideEffect(GallerySideEffect.ShowToast("$groupName 갤러리를 불러왔습니다."))
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
        ImageRepository.updateImages(newImages)
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
        val finalList = state.allImages.map {
            if (it.id in analyzedIds) {
                it.copy(status = ImageStatus.COMPLETE, tags = listOf("#풍경", "#바다", "#김민수").shuffled())
            } else it
        }

        reduce { state.copy(isAnalyzing = false, allImages = finalList) }

        // --- 핵심 수정 3: Repository 업데이트 ---
        ImageRepository.updateImages(finalList)

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

    /**
     * '전체 선택' 또는 '전체 해제' 버튼을 클릭했을 때 호출됩니다.
     * 현재 필터링된 이미지를 기준으로 동작합니다.
     */
    fun onDeselectAll() = intent {
        reduce { state.copy(selectedImageIds = emptyList()) }
    }

    fun onToggleImageSelection(imageId: String) = intent {
        // 이 함수는 선택 모드에서만 호출되므로, isSelectionMode 체크는 호출부에서 담당합니다.
        val newSelection = state.selectedImageIds.toMutableList()
        if (newSelection.contains(imageId)) {
            newSelection.remove(imageId)
        } else {
            newSelection.add(imageId)
        }
        // 마지막 아이템 선택 해제 시, 선택 모드도 함께 종료
        reduce { state.copy(selectedImageIds = newSelection, isSelectionMode = newSelection.isNotEmpty()) }
    }

    /**
     * 이미지 롱클릭 시 선택 모드를 시작하고, 해당 이미지를 선택 목록에 추가합니다.
     */
    fun onImageLongClick(imageId: String) = intent {
        if (state.isSelectionMode) return@intent
        reduce { state.copy(isSelectionMode = true, selectedImageIds = listOf(imageId)) }
    }

    fun onClearSelection() = intent {
        reduce { state.copy(isSelectionMode = false, selectedImageIds = emptyList()) }
    }
    fun onCreateTierAlbumClick() = intent {
        if (state.isCreatingAlbum || state.selectedImageIds.isEmpty()) return@intent

        // 1. 앨범 생성 시작을 알리고 로딩 상태로 변경
        reduce { state.copy(isCreatingAlbum = true) }
        postSideEffect(GallerySideEffect.ShowToast("티어 앨범 생성을 요청합니다..."))

        // 2. 백엔드 API 호출 시뮬레이션 (1초 딜레이)
        delay(1000)
        val newAlbumId = "tier-" + UUID.randomUUID().toString().take(8)

        // 이름이 변경된 SideEffect 발생
        postSideEffect(GallerySideEffect.NavigateToTierMaker(newAlbumId))

        // 3. 작업 완료 후, 받은 ID로 SideEffect를 보내고 상태 정리
        reduce { state.copy(isCreatingAlbum = false) }
        onClearSelection()
    }

    fun onCreateTimelineAlbumClick() = intent {
        if (state.isCreatingAlbum || state.selectedImageIds.isEmpty()) return@intent

        // 1. 앨범 생성 시작
        reduce { state.copy(isCreatingAlbum = true) }
        postSideEffect(GallerySideEffect.ShowToast("타임라인 앨범 생성을 요청합니다..."))

        // 2. 백엔드 API 호출 시뮬레이션 (1초 딜레이)
        delay(1000)
        val newAlbumId = "timeline-" + UUID.randomUUID().toString().take(8)

        // 3. 작업 완료 후, 받은 ID로 SideEffect를 보내고 상태 정리
        postSideEffect(GallerySideEffect.NavigateToTimelineAlbum(newAlbumId))
        reduce { state.copy(isCreatingAlbum = false) }
        onClearSelection()
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
        ImageRepository.updateImages(remainingImages)
        postSideEffect(GallerySideEffect.ShowToast("${state.selectedImageIds.size}개 사진 삭제 완료"))
    }

    fun onAiIconClick() = intent {
        // UI에서 직접 처리하므로 ViewModel의 역할은 없음
    }
}
