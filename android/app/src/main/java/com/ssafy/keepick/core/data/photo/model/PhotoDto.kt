package com.ssafy.keepick.core.data.photo.model

import com.ssafy.keepick.core.domain.photo.model.Photo
import kotlinx.serialization.Serializable

@Serializable
data class PhotoDto(
    val photoId: Long,
    val originalUrl: String,
    val thumbnailUrl: String,
    val takenAt: String,
    val width: Int,
    val height: Int
)

fun PhotoDto.toDomain(): Photo {
    return Photo(
        id = this.photoId,
        originalUrl = this.originalUrl,
        thumbnailUrl = this.thumbnailUrl,
        takenAt = this.takenAt // TODO: 필요시 날짜 형식 파싱
    )
}
