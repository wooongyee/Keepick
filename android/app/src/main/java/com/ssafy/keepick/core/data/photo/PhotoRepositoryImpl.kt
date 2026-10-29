package com.ssafy.keepick.core.data.photo

import com.ssafy.keepick.core.data.photo.model.toDomain
import com.ssafy.keepick.core.data.util.networkFlow
import com.ssafy.keepick.core.domain.model.Resource
import com.ssafy.keepick.core.domain.photo.PhotoRepository
import com.ssafy.keepick.core.domain.photo.model.Photo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PhotoRepositoryImpl @Inject constructor(
    private val photoService: PhotoService
) : PhotoRepository {

    override suspend fun getRandomPhotos(size: Int): Flow<Resource<List<Photo>>> =
        networkFlow {
            photoService.getRandomPhotos(size)
        }.map { resource ->
            when (resource) {
                is Resource.Success -> Resource.Success(resource.data.map { it.toDomain() })
                is Resource.Failure -> resource // 실패는 그대로 전달
                Resource.Loading -> Resource.Loading
            }
        }
}
