package com.ssafy.keepick.core.data.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val accessToken : String
)
