package com.ssafy.keepick.core.auth

import com.ssafy.keepick.core.data.auth.model.LoginRequest
import com.ssafy.keepick.core.domain.AuthRepository
import com.ssafy.keepick.core.domain.model.LoginType
import com.ssafy.keepick.core.domain.model.Resource
import javax.inject.Inject


class AuthManager @Inject constructor(
    private val kakaoAuthManager: KakaoAuthManager,
    private val naverAuthManager: NaverAuthManager,
    private val authRepository: AuthRepository, // 서버 통신용 Repository
    private val tokenManager: TokenManager // 토큰 저장용 Manager
) {

    /**
     * 카카오로 소셜 로그인 후, 서버로부터 서비스 토큰을 발급받아 저장합니다.
     * @return Result<Unit> 전체 로그인 과정의 성공 또는 실패
     */
    suspend fun signInWithKakao(): Result<Unit> = runCatching {
        // 1. 카카오 소셜 로그인 시도 및 소셜 액세스 토큰 획득
        val socialToken = kakaoAuthManager.login().getOrThrow()

        // 2. 우리 서버에 로그인 요청
        val request = LoginRequest(provider = LoginType.KAKAO.value, accessToken = socialToken)
        handleServerLogin(request)
    }

    /**
     * 네이버로 소셜 로그인 후, 서버로부터 서비스 토큰을 발급받아 저장합니다.
     * @return Result<Unit> 전체 로그인 과정의 성공 또는 실패
     */
    suspend fun signInWithNaver(): Result<Unit> = runCatching {
        // 1. 네이버 소셜 로그인 시도 및 소셜 액세스 토큰 획득
        val socialToken = naverAuthManager.login().getOrThrow()

        // 2. 우리 서버에 로그인 요청
        val request = LoginRequest(provider = LoginType.NAVER.value, accessToken = socialToken)
        handleServerLogin(request)
    }

    /**
     * 서버 로그인 결과를 처리하고, 성공 시 토큰을 저장하는 공통 로직
     */
    private suspend fun handleServerLogin(request: LoginRequest) {
        authRepository.login(request).collect { resource ->
            when (resource) {
                is Resource.Success -> {
                    // 3. 성공 시, 서비스 AccessToken을 DataStore에 저장
                    tokenManager.saveAccessToken(resource.data.accessToken)
                }
                is Resource.Failure -> {
                    // 3. 실패 시, 예외를 발생시켜 runCatching에서 잡히도록 함
                    throw RuntimeException("Server login failed: ${resource.errorMessage}")
                }
                is Resource.Loading -> {
                    // 위에서 필터링 했으므로 이 블록은 실행되지 않음
                }
            }
        }

    }
}
