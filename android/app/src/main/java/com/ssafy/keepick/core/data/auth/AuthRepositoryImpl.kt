package com.ssafy.keepick.core.data.auth

import com.ssafy.keepick.core.data.auth.model.LoginRequest
import com.ssafy.keepick.core.data.auth.model.LoginResponse
import com.ssafy.keepick.core.data.util.networkFlow
import com.ssafy.keepick.core.domain.AuthRepository
import com.ssafy.keepick.core.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService
) : AuthRepository {

    override suspend fun login(request: LoginRequest): Flow<Resource<LoginResponse>> =
        networkFlow {
            authService.login(request)
        }
}
