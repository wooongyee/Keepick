package com.ssafy.keepick.core.data.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val provider : String,
    val accessToken: String
)
