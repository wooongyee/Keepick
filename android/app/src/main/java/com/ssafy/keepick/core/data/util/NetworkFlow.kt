package com.ssafy.keepick.core.data.util

import com.ssafy.keepick.core.data.model.common.ApiResponse
import com.ssafy.keepick.core.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

/**
 * 네트워크 API 호출을 실행하고 그 결과를 Resource<T> 스트림으로 변환하는 유틸리티 함수.
 *
 * @param T API 응답 데이터의 타입.
 * @param apiCall 실제 네트워크 호출을 수행하는 suspend 람다.
 * @return Loading 상태로 시작하여 Success 또는 Failure 상태를 emit하는 Flow.
 */
fun <T> networkFlow(
    apiCall: suspend () -> ApiResponse<T>
): Flow<Resource<T>> = flow {
    emit(Resource.Loading)

    try {
        val response = apiCall()

        if (response.status in 200..299) { // 성공적인 HTTP 상태 코드 범위
            if (response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Failure.NullData)
            }
        } else {
            emit(Resource.Failure(errorMessage = response.message, code = response.status))
        }

    } catch (e: HttpException) {
        emit(Resource.Failure(errorMessage = e.message(), code = e.code()))
    } catch (e: IOException) {
        emit(Resource.Failure(errorMessage = "네트워크 연결을 확인해주세요.", code = -1))
    } catch (e: Exception) {
        emit(Resource.Failure(errorMessage = e.message ?: "알 수 없는 오류가 발생했습니다.", code = -2))
    }
}
