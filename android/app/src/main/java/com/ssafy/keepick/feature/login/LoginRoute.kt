package com.ssafy.keepick.feature.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LoginRoute(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    // viewModel: LoginViewModel = hiltViewModel()
) {
    // val loginUiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginScreen(
        onKakaoLoginClick = {
            // TODO: viewModel.loginWithKakao() 호출
            println("카카오 로그인 버튼 클릭됨")
        },
        onNaverLoginClick = {
            // TODO: viewModel.loginWithNaver() 호출
            println("네이버 로그인 버튼 클릭됨")
        },
        modifier = modifier
    )
}
