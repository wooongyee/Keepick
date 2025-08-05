package com.ssafy.keepick.core.data.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val provider : String,
    val accessToken: String
)
