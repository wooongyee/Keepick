package com.ssafy.keepick.feature.login

sealed interface LoginState {
    data class Idle(val isLoading: Boolean = false) : LoginState
    data class Error(val message: String) : LoginState
}

sealed interface LoginSideEffect {
    object LaunchKakaoLogin : LoginSideEffect
    object LaunchNaverLogin : LoginSideEffect
    object NavigateToHome : LoginSideEffect
    data class ShowToast(val message: String) : LoginSideEffect
}
