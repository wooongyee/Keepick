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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onParticipantClick: (participantId: String) -> Unit, // [신규]
    onClearFocus: () -> Unit,                           // [신규]
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
            val myParticipantState = uiState.participants.find { it.isMe }
            val focusedParticipant =
                uiState.focusedParticipantId?.let { id -> uiState.participants.find { it.id == id } }
            if (isFullScreen) {
                Column(Modifier.fillMaxSize()) {
                    FullscreenHeader(
                        onBackClick = if (focusedParticipant != null) onClearFocus else onMinimize,
                        onInviteClick = onInviteClick
                    )

                    AnimatedContent(
                        targetState = focusedParticipant,
                        label = "GridToFocusTransition",
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith
                                    fadeOut(tween(200))
                        }
                    ) { targetParticipant ->
                        if (targetParticipant != null) {
                            FocusedParticipantView(
                                participant = targetParticipant,
                                onBackClick = onClearFocus, // 헤더와 별도로 내부 로직용으로 유지
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
            } else {
                // --- 플로팅 모드 UI (기존 로직) ---
                Column(Modifier.fillMaxSize()) {
                    FloatingHeader(onMaximizeClick = onExpand, onIconifyClick = onMinimize)
                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.participants.isNotEmpty()) {
                            val mainParticipant = myParticipantState ?: uiState.participants.first()
                            ParticipantCard(
                                participant = mainParticipant,
                                isFloating = true,
                                emojiEvent = uiState.emojiEvents[mainParticipant.id],
                                onEmojiShown = { eventId ->
                                    onEmojiShown(
                                        mainParticipant.id,
                                        eventId
                                    )
                                },
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

            // --- 하단 컨트롤 메뉴 (오버레이) ---
            if (isFullScreen) {
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
                        modifier = with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                state = rememberSharedContentState(key = "groupchat-controls"),
                                animatedVisibilityScope = animatedContentScope,
                                boundsTransform = boundsTransform
                            )
                        }
                    )
                }
            }
        }

    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FocusedParticipantView(
    participant: Participant,
    onBackClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        with(LocalSharedTransitionScope.current) {
            ParticipantCard(
                participant = participant,
                emojiEvent = null,
                onEmojiShown = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }
    }
}
