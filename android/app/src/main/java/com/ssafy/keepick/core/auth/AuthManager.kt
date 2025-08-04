package com.ssafy.keepick.core.auth

import javax.inject.Inject

class AuthManager @Inject constructor(
    private val kakaoAuthManager: KakaoAuthManager,
    private val naverAuthManager: NaverAuthManager,
) {
    suspend fun signInWithKakao(): Result<String> = kakaoAuthManager.login()

    suspend fun signInWithNaver(): Result<String> = naverAuthManager.login()
}
