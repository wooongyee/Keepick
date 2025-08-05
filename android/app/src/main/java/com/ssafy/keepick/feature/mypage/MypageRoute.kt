package com.ssafy.keepick.feature.mypage

import androidx.compose.runtime.Composable

// 내비게이션의 목적지. ViewModel을 주입받아 Screen에 데이터를 전달
@Composable
fun MyPageRoute(
    onNavigateToLogin: () -> Unit,
) {
    // 이곳에서 ViewModel을 생성하거나 주입받아 사용합니다.
    // val viewModel: MypageViewModel = hiltViewModel()
    // val uiState by viewModel.uiState.collectAsState()

    MypageScreen(
    )
}
