package com.ssafy.keepick.feature.groupchat

import androidx.compose.runtime.Composable


@Composable
fun GroupChatScreen(
    uiState: GroupChatState
) {
    val viewModel = LocalGroupChatViewModel.current

    if (uiState.isActive) {
        GroupChatOverlay(
            uiState = uiState,
            onMinimize = viewModel::minimize,
            onExpand = viewModel::expand,
            onEndGroupChat = viewModel::endGroupChat,
            onBackPress = viewModel::handleBackPress,
            onInvite = { /* TODO: 초대 로직 */ }
        )
    }
}
