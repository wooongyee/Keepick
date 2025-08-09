package com.ssafy.keepick.core.data.auth

import com.ssafy.keepick.core.data.auth.model.LoginRequest
import com.ssafy.keepick.core.data.auth.model.LoginResponse
import com.ssafy.keepick.core.data.model.common.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("/api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): ApiResponse<LoginResponse>

}
