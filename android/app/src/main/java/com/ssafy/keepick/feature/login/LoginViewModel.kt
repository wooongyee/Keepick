package com.ssafy.keepick.feature.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.keepick.core.data.model.auth.LoginRequest
import com.ssafy.keepick.core.domain.AuthRepository
import com.ssafy.keepick.core.domain.model.LoginType
import com.ssafy.keepick.core.domain.model.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel(),ContainerHost<LoginState, LoginSideEffect> {

    private val TAG = "LoginViewModel"

    override val container =
        viewModelScope.container<LoginState, LoginSideEffect>(LoginState.Idle())

    /**
     * Action을 public 함수로 구현
     */
    fun requestKakaoLogin() = intent {
        reduce { LoginState.Idle(isLoading = true) }
        postSideEffect(LoginSideEffect.LaunchKakaoLogin)
    }

    fun requestNaverLogin() = intent {
        reduce { LoginState.Idle(isLoading = true) }
        postSideEffect(LoginSideEffect.LaunchNaverLogin)
    }

    fun onSocialLoginSuccess(type: LoginType, socialToken: String) = intent {
        Log.d(TAG, "소셜 로그인 성공, 서버 통신 시작. type: $type")
        authRepository.login(LoginRequest(type.value, socialToken)).collect { resource ->
            when (resource) {
                is Resource.Success -> {
                    Log.d(TAG, "서버 로그인 성공")
                    reduce { LoginState.Idle(isLoading = false) }
                    postSideEffect(LoginSideEffect.NavigateToHome)
                }
                is Resource.Failure -> {
                    Log.d(TAG, "서버 로그인 API 실패: ${resource.errorMessage}")
                    reduce { LoginState.Idle(isLoading = false) }
                    postSideEffect(LoginSideEffect.ShowToast(resource.errorMessage))
                }
                is Resource.Loading -> { /* No-op */ }
            }
        }
    }

    fun onSocialLoginFailure(error: Throwable) = intent {
        Log.e(TAG, "소셜 로그인 실패", error)
        reduce { LoginState.Idle(isLoading = false) }
        postSideEffect(LoginSideEffect.ShowToast(error.message ?: "알 수 없는 에러가 발생했습니다."))
    }

}
