package com.ssafy.keepick.feature.login

sealed interface LoginSideEffect {
    object LaunchKakaoLogin : LoginSideEffect
    object LaunchNaverLogin : LoginSideEffect
    object NavigateToHome : LoginSideEffect
    data class ShowToast(val message: String) : LoginSideEffect
}

data class LoginState(
    val isLoading: Boolean = false
) {
    companion object {
        fun Idle(isLoading: Boolean = false) = LoginState(isLoading)
    }
}
