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

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is LoginSideEffect.LaunchKakaoLogin -> {
                scope.launch {
                    // AuthManager에게 카카오 로그인을 요청하고, 최종 결과만 ViewModel에 전달
                    authManager.signInWithKakao()
                        .onSuccess { viewModel.onLoginSuccess() } // 성공 시 호출
                        .onFailure { error -> viewModel.onLoginFailure(error) } // 실패 시 호출
                }
            }
            is LoginSideEffect.LaunchNaverLogin -> {
                scope.launch {
                    // AuthManager에게 네이버 로그인을 요청하고, 최종 결과만 ViewModel에 전달
                    authManager.signInWithNaver()
                        .onSuccess { viewModel.onLoginSuccess() } // 성공 시 호출
                        .onFailure { error -> viewModel.onLoginFailure(error) } // 실패 시 호출
                }
            }
            is LoginSideEffect.NavigateToHome -> onNavigateToHome()
            is LoginSideEffect.ShowToast -> {
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LoginScreen(
        isLoading = uiState.isLoading,
        onKakaoLoginClick = viewModel::requestKakaoLogin,
        onNaverLoginClick = viewModel::requestNaverLogin,
        modifier = modifier
    )
}
