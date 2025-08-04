package com.ssafy.keepick.core.data.model.common

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/**
 * 서버로부터의 공통 응답을 표현하는 데이터 클래스.
 *
 * @param T 응답 데이터의 타입 (제네릭).
 * @property status 응답 상태 코드 (e.g., 200, 201, 404).
 * @property message 응답 메시지 (e.g., "성공", "유저를 찾을 수 없습니다.").
 * @property data 실제 응답 데이터. 데이터가 없을 경우 null일 수 있다.
 */
@OptIn(InternalSerializationApi::class)
@Serializable
data class ApiResponse<T>(
    val status: Int,
    val message: String,
    val data: T? = null
)
