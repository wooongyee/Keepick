// ChatContainer.kt

package com.ssafy.keepick.feature.groupchat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun ChatContainer(
    uiState: GroupChatState,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier,
    onMinimize: () -> Unit,
    onExpand: () -> Unit,
    onInviteClick: () -> Unit,
    onHangupClick: () -> Unit,
    onToggleVideoClick: () -> Unit,
    onToggleMicClick: () -> Unit,
    onShowChatClick: () -> Unit,
    onEmojiShown: (participantId: String, eventId: String) -> Unit,
    onShowSettings: () -> Unit,
    onHideBottomSheet: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onParticipantClick: (participantId: String) -> Unit,
    onClearFocus: () -> Unit,
) {
    val isFullScreen = uiState.size == GroupChatSize.FULLSCREEN
    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isFullScreen) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 500),
        label = "cornerRadiusAnimation"
    )

    with(sharedTransitionScope) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(animatedCornerRadius))
                .sharedBounds(
                    rememberSharedContentState(key = "container-bounds"),
                    animatedVisibilityScope = animatedContentScope,
                    boundsTransform = boundsTransform
                )
                .background(Color(0xFF1C1C1E))
        ) {
            // [리팩토링] Fullscreen과 Floating 모드를 별도의 Composable로 분리하여 가독성 향상
            if (isFullScreen) {
                FullscreenChatContent(
                    uiState = uiState,
                    animatedContentScope = animatedContentScope,
                    onMinimize = onMinimize,
                    onInviteClick = onInviteClick,
                    onHangupClick = onHangupClick,
                    onToggleVideoClick = onToggleVideoClick,
                    onToggleMicClick = onToggleMicClick,
                    onShowChatClick = onShowChatClick,
                    onEmojiShown = onEmojiShown,
                    onShowSettings = onShowSettings,
                    onHideBottomSheet = onHideBottomSheet,
                    onToggleScreenShare = onToggleScreenShare,
                    onParticipantClick = onParticipantClick,
                    onClearFocus = onClearFocus,
                )
            } else {
                FloatingChatContent(
                    uiState = uiState,
                    animatedContentScope = animatedContentScope,
                    onExpand = onExpand,
                    onMinimize = onMinimize,
                    onHangupClick = onHangupClick,
                    onToggleVideoClick = onToggleVideoClick,
                    onToggleMicClick = onToggleMicClick,
                    onEmojiShown = onEmojiShown,
                )
            }
        }
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.FullscreenChatContent(
    uiState: GroupChatState,
    animatedContentScope: AnimatedContentScope,
    onMinimize: () -> Unit,
    onInviteClick: () -> Unit,
    onHangupClick: () -> Unit,
    onToggleVideoClick: () -> Unit,
    onToggleMicClick: () -> Unit,
    onShowChatClick: () -> Unit,
    onEmojiShown: (participantId: String, eventId: String) -> Unit,
    onShowSettings: () -> Unit,
    onHideBottomSheet: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onParticipantClick: (participantId: String) -> Unit,
    onClearFocus: () -> Unit,
) {
    val focusedParticipant =
        uiState.focusedParticipantId?.let { id -> uiState.participants.find { it.id == id } }
    var initialControlSheetHeight by remember { mutableStateOf(0.dp) }
    var hasMeasured by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize()) {
        // --- 메인 콘텐츠 영역 (헤더 + 참가자 뷰) ---
        Column(Modifier.fillMaxSize()) {
            FullscreenHeader(
                onBackClick = if (focusedParticipant != null) onClearFocus else onMinimize,
                onInviteClick = onInviteClick
            )

            AnimatedContent(
                // [2] 측정된 초기 높이값을 하단 패딩으로 사용
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = initialControlSheetHeight),
                targetState = focusedParticipant,
                label = "GridToFocusTransition",
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                }
            ) { targetParticipant ->
                if (targetParticipant != null) {
                    FocusedParticipantView(
                        participant = targetParticipant,
                    )
                } else {
                    ParticipantGrid(
                        participants = uiState.participants,
                        emojiEvents = uiState.emojiEvents,
                        onEmojiShown = onEmojiShown,
                        onParticipantClick = onParticipantClick,
                    )
                }
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            GroupChatControlSheet(
                uiState = uiState,
                onHangupClick = onHangupClick,
                onToggleVideoClick = onToggleVideoClick,
                onToggleMicClick = onToggleMicClick,
                onShowChatClick = onShowChatClick,
                onShowSettings = onShowSettings,
                onHideBottomSheet = onHideBottomSheet,
                onToggleScreenShare = onToggleScreenShare,
                modifier = Modifier
                    .sharedElement(
                        state = rememberSharedContentState(key = "groupchat-controls"),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = boundsTransform
                    )
                    .onSizeChanged { size ->
                        if (!hasMeasured && size.height > 0) {
                            initialControlSheetHeight = with(density) { size.height.toDp() }
                            hasMeasured = true
                        }
                    }
            )
        }
    }
}

/**
 * [리팩토링] 플로팅 모드 UI를 담당하는 컴포저블
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.FloatingChatContent(
    uiState: GroupChatState,
    animatedContentScope: AnimatedContentScope,
    onExpand: () -> Unit,
    onMinimize: () -> Unit,
    onHangupClick: () -> Unit,
    onToggleVideoClick: () -> Unit,
    onToggleMicClick: () -> Unit,
    onEmojiShown: (participantId: String, eventId: String) -> Unit,
) {
    val myParticipantState = uiState.participants.find { it.isMe }

    Column(Modifier.fillMaxSize()) {
        FloatingHeader(onMaximizeClick = onExpand, onIconifyClick = onMinimize)
        Box(modifier = Modifier.weight(1f)) {
            if (uiState.participants.isNotEmpty()) {
                val mainParticipant = myParticipantState ?: uiState.participants.first()
                ParticipantCard(
                    participant = mainParticipant,
                    isFloating = true,
                    emojiEvent = uiState.emojiEvents[mainParticipant.id],
                    onEmojiShown = { eventId -> onEmojiShown(mainParticipant.id, eventId) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
        ChatControls(
            isFullScreen = false,
            myParticipantState = myParticipantState,
            onHangupClick = onHangupClick,
            onToggleVideoClick = onToggleVideoClick,
            onToggleMicClick = onToggleMicClick,
            onShowChatClick = {},
            modifier = Modifier
                .padding(bottom = 8.dp)
                .padding(horizontal = 8.dp)
                .sharedElement(
                    rememberSharedContentState(key = "groupchat-controls"),
                    animatedVisibilityScope = animatedContentScope,
                    boundsTransform = boundsTransform
                )
        )
    }
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FocusedParticipantView(
    participant: Participant,
    // onBackClick은 Header에서 처리하므로 여기서 제거
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // [리팩토링] 불필요한 with(LocalSharedTransitionScope.current) 제거
        // 부모 Composable에서 이미 SharedTransitionScope가 제공되고 있음
        ParticipantCard(
            participant = participant,
            emojiEvent = null,
            onEmojiShown = {},
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
            // ParticipantCard 내부에서 sharedElement를 사용한다고 가정
        )
    }
}

