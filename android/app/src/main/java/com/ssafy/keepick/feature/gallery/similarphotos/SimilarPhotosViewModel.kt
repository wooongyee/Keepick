package com.ssafy.keepick.feature.gallery.similarphotos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.keepick.core.data.photo.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class SimilarPhotosViewModel @Inject constructor(
    private val imageRepository: ImageRepository
) : ViewModel(), ContainerHost<SimilarPhotosState, Unit> {

    override val container = container<SimilarPhotosState, Unit>(SimilarPhotosState())

    init {
        // ViewModel 생성 시 Repository의 상태들을 구독
        observeRepositoryState()
    }

    private fun observeRepositoryState() = intent {
        // AI 분석 진행 상태 구독
        imageRepository.analysisState
            .onEach { analysisState ->
                reduce { state.copy(isAnalyzing = analysisState.isAnalyzing, progress = analysisState.progress) }
            }
            .launchIn(viewModelScope)

        // 유사 사진 클러스터 결과 구독
        imageRepository.similarPhotoClusters
            .onEach { clusters ->
                reduce { state.copy(photoClusters = clusters) }
            }
            .launchIn(viewModelScope)
    }

    // 사용자 이벤트를 처리하는 단일 진입점
    fun onEvent(event: SimilarPhotosEvent) {
        when (event) {
            is SimilarPhotosEvent.OnRequestAnalysis -> intent { imageRepository.analyzeSimilarPhotos() }
            is SimilarPhotosEvent.OnDeleteSelectedPhotos -> deleteSelectedPhotos()
            is SimilarPhotosEvent.OnTogglePhotoSelection -> togglePhotoSelection(event.photoId)
            is SimilarPhotosEvent.OnImageLongClick -> handleImageLongClick(event.photoId)
            is SimilarPhotosEvent.OnClearSelection -> clearSelection()
        }
    }

    private fun deleteSelectedPhotos() = intent {
        val idsToDelete = state.selectedPhotoIds
        if (idsToDelete.isEmpty()) return@intent

        // 1. Repository에 삭제 요청 위임
        imageRepository.deletePhotos(idsToDelete.toList())

        // 2. ViewModel은 UI 선택 상태만 초기화
        // (photoClusters 업데이트는 observeRepositoryState가 자동으로 처리)
        reduce {
            state.copy(
                selectedPhotoIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    private fun handleImageLongClick(photoId: String) = intent {
        if (state.isSelectionMode) return@intent
        reduce {
            state.copy(
                isSelectionMode = true,
                selectedPhotoIds = setOf(photoId)
            )
        }
    }

    private fun togglePhotoSelection(photoId: String) = intent {
        if (!state.isSelectionMode) return@intent

        val newSelectedIds = state.selectedPhotoIds.toMutableSet()
        if (newSelectedIds.contains(photoId)) {
            newSelectedIds.remove(photoId)
        } else {
            newSelectedIds.add(photoId)
        }

        reduce {
            state.copy(
                selectedPhotoIds = newSelectedIds,
                isSelectionMode = newSelectedIds.isNotEmpty()
            )
        }
    }

    private fun clearSelection() = intent {
        reduce {
            state.copy(
                selectedPhotoIds = emptySet(),
                isSelectionMode = false
            )
        }
    }
}
