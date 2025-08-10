package com.ssafy.keepick.feature.gallery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.keepick.core.data.photo.ImageRepository
import com.ssafy.keepick.core.domain.photo.PhotoRepository
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
    private val photoRepository: PhotoRepository,
    private val imageRepository: ImageRepository
) : ViewModel(), ContainerHost<GalleryState, GallerySideEffect> {

    override val container = container<GalleryState, GallerySideEffect>(GalleryState())

    init {
        observeRepositoryState()
        // ViewModel이 처음 생성될 때, 초기 그룹에 대한 이미지 로드를 요청합니다.
        loadImagesForGroup("키픽 공식 그룹")
    }

    /**
     * Repository의 데이터 변경을 구독하여 UI 상태(State)를 자동으로 업데이트합니다.
     */
    private fun observeRepositoryState() = intent {
        // 이미지 목록 구독
        imageRepository.images
            .onEach { images ->
                reduce { state.copy(allImages = images) }
                refreshTags() // 이미지 목록이 변경될 때마다 태그 목록도 갱신
            }
            .launchIn(viewModelScope)

        // AI 분석 상태 구독
        imageRepository.analysisState
            .onEach { analysisState ->
                reduce {
                    state.copy(
                        isAnalyzing = analysisState.isAnalyzing,
                        analysisProgress = analysisState.progress
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 그룹을 변경하거나 데이터를 새로고침할 때, Repository에 데이터 로드를 요청합니다.
     */
    fun loadImagesForGroup(groupName: String) = intent {
        if (state.groupName == groupName && !state.isLoading) return@intent

        reduce { state.copy(isLoading = true, groupName = groupName, selectedTags = emptyList()) }

        // 실제 데이터 로딩은 Repository에 위임합니다.
        viewModelScope.launch {
            imageRepository.loadImagesForGroup(groupName)
            // 로딩이 완료되면 UI 상태를 업데이트합니다.
            reduce { state.copy(isLoading = false) }
            postSideEffect(GallerySideEffect.ShowToast("$groupName 갤러리를 불러왔습니다."))
        }
    }

    /**
     * 새로운 이미지를 갤러리에 추가하도록 Repository에 요청합니다.
     */
    fun addImagesToGallery(uris: List<Uri>) {
        // 복잡한 업로드/분석 로직은 모두 Repository가 담당합니다.
        imageRepository.addAndProcessImages(uris)
    }

    /**
     * 선택된 이미지를 삭제하도록 Repository에 요청합니다.
     */
    fun onDeleteSelectedImages() = intent {
        val idsToDelete = state.selectedImageIds // 이제 List 타입
        if (idsToDelete.isEmpty()) return@intent

        viewModelScope.launch {
            imageRepository.deletePhotos(idsToDelete) // .toList() 불필요
            reduce {
                state.copy(
                    selectedImageIds = emptyList(), // emptySet() -> emptyList()
                    isSelectionMode = false
                )
            }
            postSideEffect(GallerySideEffect.ShowToast("${idsToDelete.size}개 사진 삭제 완료"))
        }
    }

    /**
     * 현재 이미지 목록을 기반으로 필터링에 사용할 태그 목록을 갱신합니다.
     */
    private fun refreshTags() = intent {
        val allTagsFromImages = state.allImages
            .flatMap { it.tags }
            .map { it.removePrefix("#") }
            .distinct()

        val knownPersonNames = listOf("김민수", "이영희") // 예시
        val personTags = allTagsFromImages.filter { it in knownPersonNames }
        val specialTags = state.specialAiTags
        val newGeneralTags = allTagsFromImages.filter { it !in personTags && it !in specialTags }

        reduce {
            state.copy(
                personTags = personTags.distinct(),
                generalTags = (state.generalTags + newGeneralTags).distinct()
            )
        }
    }

    fun onToggleImageSelection(imageId: String) = intent {
        // state.selectedImageIds를 List로 다루도록 수정
        val newSelection = state.selectedImageIds.toMutableList()
        if (newSelection.contains(imageId)) {
            newSelection.remove(imageId)
        } else {
            newSelection.add(imageId)
        }
        // 수정된 리스트로 상태를 업데이트합니다.
        reduce { state.copy(selectedImageIds = newSelection, isSelectionMode = newSelection.isNotEmpty()) }
    }

    fun onImageLongClick(imageId: String) = intent {
        if (state.isSelectionMode) return@intent
        reduce { state.copy(isSelectionMode = true, selectedImageIds = listOf(imageId)) }
    }

    fun onClearSelection() = intent {
        reduce { state.copy(isSelectionMode = false, selectedImageIds = emptyList()) }
    }
    fun onToggleTagSelection(tag: String) = intent {
        val currentSelection = state.selectedTags.toMutableList()
        // '유사한 사진'을 exclusiveGroup에서 제거합니다.
        val exclusiveGroup = listOf("흐린 사진")

        when {
            tag == "전체" -> reduce { state.copy(selectedTags = emptyList()) }
            tag in exclusiveGroup -> {
                val newSelection = if (currentSelection == listOf(tag)) emptyList() else listOf(tag)
                reduce { state.copy(selectedTags = newSelection) }
            }
            else -> {
                currentSelection.removeAll(exclusiveGroup)
                if (currentSelection.contains(tag)) currentSelection.remove(tag)
                else {
                    if (currentSelection.size < 5) currentSelection.add(tag)
                    else postSideEffect(GallerySideEffect.ShowToast("필터는 최대 5개까지 선택할 수 있습니다."))
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


    /**
     * 이미지 롱클릭 시 선택 모드를 시작하고, 해당 이미지를 선택 목록에 추가합니다.
     */
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

    fun onApplyFilters(tags: List<String>) = intent {
        reduce { state.copy(selectedTags = tags) }
    }

    fun onAiIconClick() = intent {
        // UI에서 직접 처리하므로 ViewModel의 역할은 없음
    }
}
