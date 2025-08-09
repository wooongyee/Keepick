package com.ssafy.keepick.feature.gallery

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.keepick.core.domain.model.Resource
import com.ssafy.keepick.core.domain.photo.PhotoRepository
import com.ssafy.keepick.feature.gallery.model.GalleryImage
import com.ssafy.keepick.feature.gallery.model.ImageStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val photoRepository: PhotoRepository // Repository 주입
) : ViewModel(), ContainerHost<GalleryState, GallerySideEffect> {

    override val container = container<GalleryState, GallerySideEffect>(GalleryState())

    init {
        loadInitialImages() // ViewModel 생성 시 서버에서 이미지 로드
    }

    private fun loadInitialImages() = intent {
        // 이전에 더미 데이터를 만들던 로직을 Repository 호출로 변경
        photoRepository.getRandomPhotos(size = 20).onEach { resource ->
            when (resource) {
                is Resource.Success -> {
                    // 서버에서 받은 Photo 리스트를 UI 모델인 GalleryImage 리스트로 변환
                    val images = resource.data.map { photo ->
                        GalleryImage(
                            id = photo.id.toString(),
                            imageUrl = photo.thumbnailUrl, // 썸네일 URL 사용
                            date = photo.takenAt,
                            tags = listOf("#서버", "#랜덤"), // 태그는 필요시 API 응답에 추가
                            status = ImageStatus.COMPLETE
                        )
                    }
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

    // Photo Picker에서 선택된 이미지들을 갤러리에 추가
    fun addImagesToGallery(uris: List<Uri>) = intent {
        val newImages = uris.map { uri ->
            GalleryImage(
                id = UUID.randomUUID().toString(),
                imageUrl = uri.toString(), // URI를 문자열로 저장
                date = "방금 전", // 실제로는 EXIF에서 추출
                tags = emptyList(),
                status = ImageStatus.ANALYZING // 업로드 및 분석 시작 상태
            )
        }
        reduce {
            state.copy(allImages = newImages + state.allImages) // 새 이미지를 맨 앞에 추가
        }
        // TODO: 백그라운드에서 실제 서버 업로드 작업 시작
    }

    fun onTagSelect(tag: String) = intent {
        reduce {
            state.copy(
                selectedTag = tag,
                isSelectionMode = false, // 태그 변경 시 선택 모드 해제
                selectedImageIds = emptyList()
            )
        }
    }

    fun onImageClick(imageId: String) = intent {
        if (state.isSelectionMode) {
            val newSelection = state.selectedImageIds.toMutableList()
            if (newSelection.contains(imageId)) newSelection.remove(imageId)
            else newSelection.add(imageId)

            reduce {
                state.copy(
                    selectedImageIds = newSelection,
                    isSelectionMode = newSelection.isNotEmpty() // 선택된 것이 없으면 선택 모드 자동 해제
                )
            }
        } else {
            postSideEffect(GallerySideEffect.NavigateToImageDetail(imageId))
        }
    }

    fun onImageLongClick(imageId: String) = intent {
        if (!state.isSelectionMode) {
            reduce {
                state.copy(isSelectionMode = true, selectedImageIds = listOf(imageId))
            }
        }
    }

    fun onClearSelection() = intent {
        reduce {
            state.copy(isSelectionMode = false, selectedImageIds = emptyList())
        }
    }

    fun onCreateAlbumClick() = intent {
        if (state.selectedImageIds.isNotEmpty()) {
            postSideEffect(GallerySideEffect.NavigateToCreateAlbum(state.selectedImageIds))
            // 앨범 만들기 화면으로 이동 후 선택 상태 초기화
            onClearSelection()
        }
    }
}
