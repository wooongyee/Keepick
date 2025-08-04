package com.ssafy.keepick.core.data.model.auth

data class LoginRequest(
    val provider : String,
    val accessToken: String
)
