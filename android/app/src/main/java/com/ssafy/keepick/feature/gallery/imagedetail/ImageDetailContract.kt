package com.ssafy.keepick.feature.gallery.imagedetail

import com.ssafy.keepick.feature.gallery.model.GalleryImage

data class ImageDetailState(
    val isLoading: Boolean = true,
    val initialImageId: String = "",
    val allImages: List<GalleryImage> = emptyList(),
    val initialPage: Int? = null,
    val clusterTitle: String? = null,
    val sharedKey: String = "",
    val currentVisibleImageId: String? = null
)
