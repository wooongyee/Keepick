package com.ssafy.keepick.core.auth

import android.content.Context
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NaverAuthManager(private val context: Context) {

    /**
     * 네이버 로그인을 시도하고, 성공 시 액세스 토큰을 반환합니다.
     *
     * @return Result<String> 성공 시 액세스 토큰, 실패 시 에러를 담은 Result 객체
     */
    suspend fun login(): Result<String> = runCatching {
        suspendCancellableCoroutine { continuation ->
            val oauthLoginCallback = object : OAuthLoginCallback {
                override fun onSuccess() {
                    val accessToken = NaverIdLoginSDK.getAccessToken()
                    if (accessToken != null) {
                        continuation.resume(accessToken)
                    } else {
                        // 성공 콜백에서 토큰이 null인 이례적인 상황
                        continuation.resumeWithException(
                            RuntimeException("네이버 로그인 성공 후 토큰을 받아오지 못했습니다.")
                        )
                    }
                }

                override fun onFailure(httpStatus: Int, message: String) {
                    val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                    continuation.resumeWithException(
                        RuntimeException(errorDescription ?: "네이버 로그인에 실패했습니다. (코드: $httpStatus, 메시지: $message)")
                    )
                }

                override fun onError(errorCode: Int, message: String) {
                    onFailure(errorCode, message)
                }
            }
            NaverIdLoginSDK.authenticate(context, oauthLoginCallback)
        }
    }
}
