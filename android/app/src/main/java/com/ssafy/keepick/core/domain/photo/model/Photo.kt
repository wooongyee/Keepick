package com.ssafy.keepick.core.domain.photo.model

data class Photo(
    val id: Long,
    val originalUrl: String,
    val thumbnailUrl: String,
    val takenAt: String
)
