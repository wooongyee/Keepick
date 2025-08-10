package com.ssafy.keepick.feature.gallery.imagedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.keepick.core.data.photo.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.drop // 초기에 처리한 값을 건너뛰기 위한 import
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel(), ContainerHost<ImageDetailState, Unit> {

    override val container = container<ImageDetailState, Unit>(ImageDetailState())
    private val initialImageId: String = savedStateHandle.get<String>("imageId") ?: ""

    init {
        intent {
            // --- 핵심 수정 사항: Repository의 현재 값을 동기적으로 확인 ---
            val initialImages = ImageRepository.currentImages.value

            if (initialImages.isNotEmpty()) {
                // 1. 데이터가 이미 있는 경우: 로딩 상태를 건너뛰고 바로 UI 상태 설정
                val initialIndex = initialImages.indexOfFirst { it.id == initialImageId }.coerceAtLeast(0)
                reduce {
                    state.copy(
                        isLoading = false, // 로딩 상태를 false로 시작
                        initialImageId = initialImageId,
                        allImages = initialImages,
                        initialPage = initialIndex
                    )
                }
            } else {
                // 2. 데이터가 없는 경우: 정상적으로 로딩 상태로 진입
                reduce { state.copy(isLoading = true, initialImageId = initialImageId) }
            }

            // 3. 향후 데이터 변경을 관찰 (초기값은 이미 처리했으므로 drop(1)으로 건너뜀)
            ImageRepository.currentImages
                .drop(1)
                .onEach { images ->
                    if (images.isNotEmpty()) {
                        val initialIndex = images.indexOfFirst { it.id == initialImageId }.coerceAtLeast(0)
                        reduce {
                            state.copy(
                                isLoading = false,
                                allImages = images,
                                initialPage = initialIndex
                            )
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }
}
