package com.ssafy.keepick.feature.groupchat

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun ChatContainer(
    uiState: GroupChatState,
    targetSize: GroupChatSize,
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
) {
    val participants = uiState.participants
    val isFullScreen = uiState.size == GroupChatSize.FULLSCREEN
    val myParticipantState = uiState.participants.find { it.isMe }

    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isFullScreen) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 500),
        label = "cornerRadiusAnimation"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(animatedCornerRadius))
            .then(
                with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        rememberSharedContentState(key = "container-bounds"),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = boundsTransform
                    )
                }
            )
            .background(Color(0xFF1C1C1E))
    ) {
        with(sharedTransitionScope) {
            Column(Modifier.fillMaxSize()) {
                val headerPadding = if (isFullScreen) {
                    WindowInsets.systemBars.asPaddingValues()
                } else {
                    WindowInsets(0, 0, 0, 0).asPaddingValues()
                }

                if (isFullScreen) {
                    FullscreenHeader(
                        onBackClick = onMinimize,
                        onInviteClick = onInviteClick,
                        modifier = Modifier.padding(headerPadding)
                    )
                } else {
                    FloatingHeader(
                        onMaximizeClick = onExpand,
                        onIconifyClick = onMinimize
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (isFullScreen) {
                        ParticipantGrid(
                            participants = participants,
                            emojiEvents = uiState.emojiEvents,
                            onEmojiShown = onEmojiShown,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedContentScope = animatedContentScope
                        )
                    } else {
                        if (participants.isNotEmpty()) {
                            val mainParticipant = myParticipantState ?: participants.first()
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
                                    .then(
                                        with(sharedTransitionScope) {
                                            Modifier.sharedElement(
                                                rememberSharedContentState(key = "main-video"),
                                                animatedVisibilityScope = animatedContentScope,
                                                boundsTransform = boundsTransform,
                                            )
                                        }
                                    )
                            )
                        }
                    }
                }

                if (isFullScreen) {
                    GroupChatControlPanel(
                        uiState = uiState,
                        onHangupClick = onHangupClick,
                        onToggleVideoClick = onToggleVideoClick,
                        onToggleMicClick = onToggleMicClick,
                        onShowChatClick = onShowChatClick,
                        onShowSettings = onShowSettings,
                        onHideBottomSheet = onHideBottomSheet,
                        onToggleScreenShare = onToggleScreenShare,
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState(key = "controls"),
                            animatedVisibilityScope = animatedContentScope,
                            boundsTransform = boundsTransform,
                        )
                    )
                } else {
                    // 플로팅 모드에서는 기존의 간단한 ChatControls를 사용
                    ChatControls(
                        isFullScreen = false,
                        myParticipantState = myParticipantState,
                        onHangupClick = onHangupClick,
                        onToggleVideoClick = onToggleVideoClick,
                        onToggleMicClick = onToggleMicClick,
                        onShowChatClick = {}, // 플로팅에서는 채팅 버튼 없음
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .padding(horizontal = 8.dp)
                            .sharedElement(
                                rememberSharedContentState(key = "controls"),
                                animatedVisibilityScope = animatedContentScope,
                                boundsTransform = boundsTransform,
                            )
                    )
                }
            }
        }
    }
}
