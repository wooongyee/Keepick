package com.ssafy.keepick.core.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.ssafy.keepick.core.datastore.UserToken
import com.ssafy.keepick.core.datastore.UserTokenSerializer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Proto DataStore 인스턴스 생성
private val Context.tokenDataStore: DataStore<UserToken> by dataStore(
    fileName = "user_token.pb",
    serializer = UserTokenSerializer()
)
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 저장된 토큰을 Flow 형태로 제공
    val accessTokenFlow: Flow<String?> = context.tokenDataStore.data
        .map { userToken ->
            // 토큰 값이 비어있으면 null 반환
            if (userToken.accessToken.isNullOrEmpty()) {
                null
            } else {
                userToken.accessToken
            }
        }

    // AccessToken 저장
    suspend fun saveAccessToken(token: String) {
        context.tokenDataStore.updateData { userToken ->
            userToken.toBuilder().setAccessToken(token).build()
        }
    }

    // AccessToken 삭제 (로그아웃 시 사용)
    suspend fun clearAccessToken() {
        context.tokenDataStore.updateData { userToken ->
            userToken.toBuilder().clearAccessToken().build()
        }
    }
}
