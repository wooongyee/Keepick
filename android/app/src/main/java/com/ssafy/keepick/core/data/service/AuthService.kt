package com.ssafy.keepick.core.data.service

import com.ssafy.keepick.core.data.model.auth.LoginRequest
import com.ssafy.keepick.core.data.model.auth.LoginResponse
import com.ssafy.keepick.core.data.model.common.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST


interface AuthService {

    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<LoginResponse>

}
