package com.ssafy.keepick.core.auth

import android.content.Context
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
class KakaoAuthManager(private val context: Context) {

    /**
     * 카카오 로그인을 시도하고, 성공 시 액세스 토큰을 반환합니다.
     * 카카오톡 앱이 있으면 앱으로, 없으면 웹으로 로그인을 시도합니다.
     *
     * @return Result<String> 성공 시 액세스 토큰, 실패 시 에러를 담은 Result 객체
     */
    suspend fun login(): Result<String> = runCatching {
        suspendCancellableCoroutine { continuation ->
            // 로그인 결과를 처리할 공통 콜백
            val callback: (token: com.kakao.sdk.auth.model.OAuthToken?, error: Throwable?) -> Unit = { token, error ->
                when {
                    error != null -> {
                        // 사용자가 로그인을 취소한 경우와 그 외 에러를 구분하여 처리
                        val kakaoException = if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            RuntimeException("사용자가 로그인을 취소했습니다.", error)
                        } else {
                            RuntimeException("카카오 로그인에 실패했습니다.", error)
                        }
                        continuation.resumeWithException(kakaoException)
                    }
                    token != null -> {
                        // 로그인 성공 시 액세스 토큰 반환
                        continuation.resume(token.accessToken)
                    }
                    else -> {
                        // 토큰이 null인 예외적인 경우
                        continuation.resumeWithException(RuntimeException("카카오 로그인 실패: 응답 토큰이 null입니다."))
                    }
                }
            }

            // 기기에 카카오톡 앱 설치 여부 확인
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                // 카카오톡으로 로그인 시도
                UserApiClient.instance.loginWithKakaoTalk(context, callback = callback)
            } else {
                // 카카오톡이 없으면 카카오 계정 웹으로 로그인 시도
                UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
            }
        }
    }
}
