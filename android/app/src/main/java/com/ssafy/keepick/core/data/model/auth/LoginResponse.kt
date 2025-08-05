package com.ssafy.keepick.core.data.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val accessToken : String
)
