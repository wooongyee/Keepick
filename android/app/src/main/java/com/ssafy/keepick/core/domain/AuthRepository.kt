package com.ssafy.keepick.core.domain

import com.ssafy.keepick.core.data.auth.model.LoginRequest
import com.ssafy.keepick.core.data.auth.model.LoginResponse
import com.ssafy.keepick.core.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(request: LoginRequest): Flow<Resource<LoginResponse>>
}
