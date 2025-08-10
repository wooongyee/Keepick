package com.ssafy.keepick.core.data.photo

import com.ssafy.keepick.feature.gallery.model.GalleryImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 두 ViewModel 간에 갤러리 이미지 목록을 공유하기 위한
 * 간단한 인메모리 저장소 (싱글턴 객체)
 */
object ImageRepository {
    private val _currentImages = MutableStateFlow<List<GalleryImage>>(emptyList())
    val currentImages = _currentImages.asStateFlow()

    fun updateImages(images: List<GalleryImage>) {
        _currentImages.value = images
    }
}
