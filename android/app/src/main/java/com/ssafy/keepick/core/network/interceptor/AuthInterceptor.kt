package com.ssafy.keepick.core.network.interceptor

import com.ssafy.keepick.core.auth.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // runBlocking은 Interceptor 컨텍스트에서 DataStore의 비동기 데이터를 동기적으로
        // 가져오기 위해 사용됩니다. 이 사용 사례는 비교적 안전합니다.
        val token = runBlocking { tokenManager.accessTokenFlow.first() }

        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
