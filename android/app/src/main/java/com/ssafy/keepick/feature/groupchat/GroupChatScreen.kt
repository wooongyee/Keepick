package com.ssafy.keepick.feature.groupchat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun GroupChatScreen() {
    val viewModel = LocalGroupChatViewModel.current
    val state: GroupChatState by viewModel.container.stateFlow.collectAsState()
    val context = LocalContext.current

    BackHandler(
        enabled = state.size == GroupChatSize.FULLSCREEN,
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
            uiState = state,
            onMinimize = viewModel::minimize,
            onExpand = viewModel::expand,
            onEndGroupChat = viewModel::endGroupChat,
            onInvite = viewModel::onInvite,
            onEmojiShown = viewModel::onEmojiShown,
            onToggleVideo = viewModel::onToggleCamera,
            onToggleMic = viewModel::onToggleMic,
            onShowChatClick = viewModel::onShowChat,
            onShowSettings = viewModel::onShowSettings,
            onHideBottomSheet = viewModel::hideBottomSheet,
            onParticipantClick = viewModel::onParticipantClick, // 추가
            onClearFocus = viewModel::clearFocusedParticipant,
            onToggleScreenShare = viewModel::onToggleScreenShare
        )
    }
}
