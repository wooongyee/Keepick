package com.ssafy.keepick.core.domain

import com.ssafy.keepick.core.data.model.auth.LoginRequest
import com.ssafy.keepick.core.data.model.auth.LoginResponse
import com.ssafy.keepick.core.domain.model.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(request: LoginRequest): Flow<Resource<LoginResponse>>
}
