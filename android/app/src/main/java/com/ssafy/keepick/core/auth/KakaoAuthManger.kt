package com.ssafy.keepick.core.auth

import android.content.Context
import android.util.Log
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.ssafy.keepick.core.util.TAG
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
        // 자동 생성된 TAG를 사용하여 로그 출력
        Log.d(TAG, "카카오 로그인을 시도합니다.")
        suspendCancellableCoroutine { continuation ->
            // 로그인 결과를 처리할 공통 콜백
            val callback: (token: OAuthToken?, error: Throwable?) -> Unit = { token, error ->
                when {
                    error != null -> {
                        // 사용자가 로그인을 취소한 경우 (뒤로가기 등)
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            val cancelException = RuntimeException("사용자가 로그인을 취소했습니다.", error)
                            Log.w(TAG, "카카오 로그인 취소", cancelException)
                            continuation.resumeWithException(cancelException)
                        } else {
                            // 그 외 모든 로그인 실패
                            val loginException = RuntimeException("카카오 로그인에 실패했습니다.", error)
                            Log.e(TAG, "카카오 로그인 실패", loginException)
                            continuation.resumeWithException(loginException)
                        }
                    }
                    token != null -> {
                        // 로그인 성공 시 액세스 토큰 반환
                        Log.i(TAG, "카카오 로그인 성공. Access Token을 받았습니다.")
                        // 보안상 실제 토큰 값을 로그로 남기지 않는 것이 좋습니다.
                        // Log.d(TAG, "Access Token: ${token.accessToken}")
                        continuation.resume(token.accessToken)
                    }
                    else -> {
                        // 토큰이 null인 예외적인 경우
                        val nullTokenException = RuntimeException("카카오 로그인 실패: 응답 토큰이 null입니다.")
                        Log.e(TAG, nullTokenException.message, nullTokenException)
                        continuation.resumeWithException(nullTokenException)
                    }
                }
            }

            // 기기에 카카오톡 앱 설치 여부 확인
            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                Log.d(TAG, "카카오톡 앱으로 로그인을 시도합니다.")
                UserApiClient.instance.loginWithKakaoTalk(context, callback = callback)
            } else {
                Log.d(TAG, "카카오톡 앱이 없어 카카오 계정(웹)으로 로그인을 시도합니다.")
                UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
            }
        }
    }.onFailure { exception ->
        // runCatching 블록에서 발생한 예외 로그 (네트워크 오류 등)
        Log.e(TAG, "로그인 처리 중 예외 발생", exception)
    }
}
