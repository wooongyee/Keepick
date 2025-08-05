package com.ssafy.keepick.core.auth

import android.content.Context
import android.util.Log
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.ssafy.keepick.core.util.TAG
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
        // 2. 로그인 시도 로그
        Log.d(TAG, "네이버 로그인을 시도합니다.")
        suspendCancellableCoroutine { continuation ->
            val oauthLoginCallback = object : OAuthLoginCallback {
                override fun onSuccess() {
                    val accessToken = NaverIdLoginSDK.getAccessToken()
                    if (accessToken != null) {
                        // 3. 로그인 성공 및 토큰 수신 로그
                        Log.i(TAG, "네이버 로그인 성공. Access Token을 받았습니다.")
                        // 실제 운영 코드에서는 보안을 위해 토큰 자체를 로그로 남기지 않는 것이 좋습니다.
                        // Log.d(TAG, "Access Token: $accessToken")
                        continuation.resume(accessToken)
                    } else {
                        // 4. 토큰이 null일 경우 에러 로그
                        val errorMessage = "네이버 로그인 성공 후 토큰을 받아오지 못했습니다."
                        Log.e(TAG, errorMessage)
                        continuation.resumeWithException(RuntimeException(errorMessage))
                    }
                }

                override fun onFailure(httpStatus: Int, message: String) {
                    val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                    val errorMessage = errorDescription ?: "네이버 로그인에 실패했습니다. (코드: $httpStatus, 메시지: $message)"
                    // 5. 로그인 실패 로그
                    Log.e(TAG, "네이버 로그인 실패: $errorMessage")
                    continuation.resumeWithException(RuntimeException(errorMessage))
                }

                override fun onError(errorCode: Int, message: String) {
                    // onError는 onFailure를 호출하므로, 로그가 중복되지 않도록 onFailure에서만 처리합니다.
                    onFailure(errorCode, message)
                }
            }
            // 네이버 SDK 로그인 함수 호출
            NaverIdLoginSDK.authenticate(context, oauthLoginCallback)
        }
    }.onFailure { exception ->
        // 6. runCatching 블록에서 발생한 예외 로그 (네트워크 오류 등)
        Log.e(TAG, "로그인 처리 중 예외 발생", exception)
    }
}
