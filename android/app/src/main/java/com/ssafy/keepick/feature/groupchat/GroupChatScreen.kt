package com.ssafy.keepick.feature.groupchat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.ssafy.keepick.feature.groupchat.state.GroupChatSideEffect
import com.ssafy.keepick.feature.groupchat.state.GroupChatSize
import com.ssafy.keepick.feature.groupchat.state.GroupChatState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun GroupChatScreen() {

    val viewModel = LocalGroupChatViewModel.current
    // 1. Orbit의 collectAsState로 상태 구독
    val state: GroupChatState by viewModel.container.stateFlow.collectAsState()
    val context = LocalContext.current

    BackHandler(
        // 조건: 그룹 채팅이 '전체화면'일 때만 이 핸들러를 활성화합니다.
        enabled = state.size == GroupChatSize.FULLSCREEN,
        // 동작: 활성화 상태에서 뒤로가기 시 ViewModel의 handleBackPress 함수를 호출합니다.
        onBack = viewModel::handleBackPress
    )

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is GroupChatSideEffect.ShowToast -> {
                Toast.makeText(context, sideEffect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (state.isActive) {
        GroupChatOverlay(
            uiState = state, // GroupChatState 객체 전달
            onMinimize = viewModel::minimize,
            onExpand = viewModel::expand,
            onEndGroupChat = viewModel::endGroupChat,
            onInvite = viewModel::onInvite,
            onEmojiShown = viewModel::onEmojiShown
        )
    }
}
