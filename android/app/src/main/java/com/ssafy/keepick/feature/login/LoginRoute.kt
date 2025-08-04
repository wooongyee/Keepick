package com.ssafy.keepick.feature.login

import android.app.Activity
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssafy.keepick.core.auth.di.AuthEntryPoint
import com.ssafy.keepick.core.domain.model.LoginType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun LoginRoute(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val authManager = EntryPointAccessors.fromActivity(
        context as Activity,
        AuthEntryPoint::class.java
    ).getAuthManager()

    // ViewModel의 SideEffect를 구독합니다.
    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            // ViewModel로부터 로그인 실행 '명령'을 받으면
            is LoginSideEffect.LaunchKakaoLogin -> {
                scope.launch {
                    // UI 레이어에서 실제 소셜 로그인 로직을 수행하고
                    authManager.signInWithKakao()
                        // 그 결과를 다시 ViewModel에 전달합니다.
                        .onSuccess { token -> viewModel.onSocialLoginSuccess(LoginType.KAKAO, token) }
                        .onFailure { error -> viewModel.onSocialLoginFailure(error) }
                }
            }
            is LoginSideEffect.LaunchNaverLogin -> {
                scope.launch {
                    authManager.signInWithNaver()
                        .onSuccess { token -> viewModel.onSocialLoginSuccess(LoginType.NAVER, token) }
                        .onFailure { error -> viewModel.onSocialLoginFailure(error) }
                }
            }
            // 나머지 SideEffect 처리
            is LoginSideEffect.NavigateToHome -> onNavigateToHome()
            is LoginSideEffect.ShowToast -> {
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // LoginScreen에는 변경이 없습니다.
    LoginScreen(
        isLoading = (uiState as? LoginState.Idle)?.isLoading ?: false,
        onKakaoLoginClick = viewModel::requestKakaoLogin,
        onNaverLoginClick = viewModel::requestNaverLogin,
        modifier = modifier
    )
}
