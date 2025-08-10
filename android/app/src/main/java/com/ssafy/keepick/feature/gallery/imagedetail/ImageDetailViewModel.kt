package com.ssafy.keepick.feature.gallery.imagedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.keepick.app.navigation.NavRoute
import com.ssafy.keepick.core.data.photo.ImageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class ImageDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val imageRepository: ImageRepository
) : ViewModel(), ContainerHost<ImageDetailState, Unit> {

    override val container = container<ImageDetailState, Unit>(ImageDetailState())
    init {
        // ViewModel은 애니메이션 키와 이미지 ID만 알면 됩니다. 인덱스는 여기서 계산합니다.
        val sharedKey: String = savedStateHandle[NavRoute.ImageDetail.SHARED_KEY_ARG] ?: ""
        val imageId: String = savedStateHandle[NavRoute.ImageDetail.IMAGE_ID_ARG] ?: ""
        val clusterId: String? = savedStateHandle[NavRoute.ImageDetail.CLUSTER_ID_ARG]

        intent {
            reduce {
                state.copy(
                    isLoading = true,
                    initialImageId = imageId,
                    sharedKey = sharedKey
                    // initialPage는 데이터 로딩 후 설정되므로 여기서 초기화하지 않습니다.
                )
            }
        }

        if (clusterId == null) {
            loadAllImages(imageId)
        } else {
            loadImagesFromCluster(imageId, clusterId)
        }
    }

    fun onPageChanged(pageIndex: Int) = intent {
        val imageId = state.allImages.getOrNull(pageIndex)?.id
        if (imageId != null) {
            reduce {
                state.copy(currentVisibleImageId = imageId)
            }
        }
    }
    private fun loadAllImages(initialImageId: String) {
        imageRepository.images
            .onEach { images ->
                if (images.isNotEmpty()) {
                    val initialIndex = images.indexOfFirst { it.id == initialImageId }.coerceAtLeast(0)
                    intent {
                        reduce {
                            // isLoading, allImages, initialPage를 한 번에 업데이트
                            state.copy(
                                isLoading = false,
                                allImages = images,
                                initialPage = initialIndex,
                                currentVisibleImageId = initialImageId,
                                clusterTitle = null
                            )
                        }
                    }
                } else {
                    intent { reduce { state.copy(isLoading = false, allImages = emptyList()) } }
                }
            }.launchIn(viewModelScope)
    }

    private fun loadImagesFromCluster(initialImageId: String, clusterId: String) {
        imageRepository.similarPhotoClusters
            .onEach { clusters ->
                val targetCluster = clusters?.find { it.id == clusterId }
                if (targetCluster != null) {
                    val imagesInCluster = targetCluster.images
                    val initialIndex = imagesInCluster.indexOfFirst { it.id == initialImageId }.coerceAtLeast(0)
                    val clusterIndex = clusters.indexOfFirst { it.id == clusterId }
                    intent {
                        reduce {
                            state.copy(
                                isLoading = false,
                                allImages = imagesInCluster,
                                initialPage = initialIndex,
                                currentVisibleImageId = initialImageId,
                                clusterTitle = "유사 사진 그룹 ${clusterIndex + 1}"
                            )
                        }
                    }
                } else {
                    intent { reduce { state.copy(isLoading = false, allImages = emptyList()) } }
                }
            }.launchIn(viewModelScope)
    }
}
