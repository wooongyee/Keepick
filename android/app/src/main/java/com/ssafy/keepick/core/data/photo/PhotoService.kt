package com.ssafy.keepick.core.data.photo

import com.ssafy.keepick.core.data.model.common.ApiResponse
import com.ssafy.keepick.core.data.photo.model.PhotoDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PhotoService {
    @GET("api/photos/random")
    suspend fun getRandomPhotos(
        @Query("size") size: Int = 10
    ): ApiResponse<List<PhotoDto>>
}
