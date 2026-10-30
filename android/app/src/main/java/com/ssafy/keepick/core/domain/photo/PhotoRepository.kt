package com.ssafy.keepick.core.domain.photo

import com.ssafy.keepick.core.domain.model.Resource
import com.ssafy.keepick.core.domain.photo.model.Photo
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    suspend fun getRandomPhotos(size: Int): Flow<Resource<List<Photo>>>
}
