package com.ssafy.keepick.core.domain.model

/**
 * 데이터의 상태를 표현하는 sealed class.
 * Loading, Success, Failure 상태를 가진다.
 */
sealed class Resource<out T> {
    data object Loading : Resource<Nothing>()

    data class Success<out R>(
        val data: R
    ) : Resource<R>()

    data class Failure(
        val errorMessage: String,
        val code: Int,
    ) : Resource<Nothing>() {
        companion object {
            val NullData = Failure(
                errorMessage = "ApiResponse data is null",
                code = 500 // 서버 내부 에러 코드로 가정
            )
        }
    }
}
