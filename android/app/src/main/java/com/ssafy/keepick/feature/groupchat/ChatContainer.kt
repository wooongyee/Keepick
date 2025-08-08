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
import com.ssafy.keepick.feature.groupchat.state.GroupChatSize
import com.ssafy.keepick.feature.groupchat.state.GroupChatState

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ChatContainer(
    uiState: GroupChatState,
    targetSize: GroupChatSize,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier,
    onMinimize: () -> Unit,
    onExpand: () -> Unit,
    onInviteClick: () -> Unit,
    onHangupClick: () -> Unit = {},
    onToggleVideoClick: () -> Unit= {},
    onToggleMicClick: () -> Unit= {},
    onShowChatClick: () -> Unit= {},
    onEmojiShown: (participantId: String, eventId: String) -> Unit,
) {
    val participants = uiState.participants
    val isFullScreen = uiState.size == GroupChatSize.FULLSCREEN
    val myParticipantState = uiState.participants.find { it.isMe }

    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isFullScreen) 0.dp else 16.dp,
        animationSpec = tween(durationMillis = 500),
        label = "cornerRadiusAnimation"
    )

    // [수정] 최상위 Box에서는 sharedBounds를 제거하고 clip과 background만 남깁니다.
    // 애니메이션은 내부 요소들이 각각 담당합니다.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(animatedCornerRadius))
            .then(
                with (sharedTransitionScope){
                    Modifier.sharedBounds(
                        rememberSharedContentState(key = "container-bounds"),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = boundsTransform
                    )
                }
            )
            .background(Color(0xFF1C1C1E))
    ) {
        Column(Modifier.fillMaxSize()) {
            val headerPadding = if (isFullScreen) {
                WindowInsets.systemBars.asPaddingValues()
            } else {
                WindowInsets(0, 0, 0, 0).asPaddingValues()
            }

            // 헤더
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


            // 메인 콘텐츠
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
                        // [수정] 플로팅 모드의 카드에 sharedElement를 직접 적용합니다.
                        // "나"의 영상이 없다면 첫번째 참가자의 영상을 메인으로 사용합니다.
                        val mainParticipant = myParticipantState ?: participants.first()
                        ParticipantCard(
                            participant = mainParticipant,
                            isFloating = true,
                            emojiEvent = uiState.emojiEvents[mainParticipant.id],
                            onEmojiShown = { eventId -> onEmojiShown(mainParticipant.id, eventId) },
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

            // [수정] 컨트롤 바를 with(scope)로 감싸고 sharedElement를 적용합니다.
            with(sharedTransitionScope) {
                ChatControls(
                    isFullScreen = isFullScreen,
                    myParticipantState = myParticipantState,
                    onHangupClick = onHangupClick,
                    onToggleVideoClick = onToggleVideoClick,
                    onToggleMicClick = onToggleMicClick,
                    onShowChatClick = onShowChatClick,
                    modifier = Modifier
                        .padding(bottom = if (isFullScreen) 16.dp else 8.dp)
                        .padding(horizontal = if (isFullScreen) 16.dp else 8.dp)
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
