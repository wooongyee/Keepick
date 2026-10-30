package com.ssafy.keepick.feature.login

import com.google.protobuf.copy

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel(), ContainerHost<LoginState, LoginSideEffect> {
    // AuthRepository 의존성 제거

    private val TAG = "LoginViewModel"

    override val container =
        viewModelScope.container<LoginState, LoginSideEffect>(LoginState.Idle())

    /**
     * 카카오 로그인 프로세스 시작을 요청합니다.
     */
    fun requestKakaoLogin() = intent {
        reduce { state.copy(isLoading = true) }
        postSideEffect(LoginSideEffect.LaunchKakaoLogin)
    }

    /**
     * 네이버 로그인 프로세스 시작을 요청합니다.
     */
    fun requestNaverLogin() = intent {
        reduce { state.copy(isLoading = true) }
        postSideEffect(LoginSideEffect.LaunchNaverLogin)
    }

    /**
     * AuthManager를 통한 전체 로그인 과정이 성공했을 때 호출됩니다.
     */
    fun onLoginSuccess() = intent {
        Log.d(TAG, "전체 로그인 성공. 홈 화면으로 이동합니다.")
        reduce { state.copy(isLoading = false) }
        postSideEffect(LoginSideEffect.NavigateToHome)
    }

    /**
     * AuthManager를 통한 로그인 과정 중 실패가 발생했을 때 호출됩니다.
     */
    fun onLoginFailure(error: Throwable) = intent {
        Log.e(TAG, "로그인 실패", error)
        reduce { state.copy(isLoading = false) }
        postSideEffect(LoginSideEffect.ShowToast(error.message ?: "알 수 없는 에러가 발생했습니다."))
    }
}
