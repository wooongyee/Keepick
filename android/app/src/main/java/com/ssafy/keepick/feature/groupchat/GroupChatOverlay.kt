package com.ssafy.keepick.feature.groupchat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
val boundsTransform = BoundsTransform { _, _ ->
    tween<Rect>(durationMillis = 500)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GroupChatOverlay(
    uiState: GroupChatState,
    onMinimize: () -> Unit,
    onExpand: () -> Unit,
    onEndGroupChat: () -> Unit,
    onInvite: () -> Unit,
    onEmojiShown: (participantId: String, eventId: String) -> Unit,
    onToggleVideo: () -> Unit,
    onToggleMic: () -> Unit,
    onShowChatClick: () -> Unit,
    onShowSettings: () -> Unit,
    onHideBottomSheet: () -> Unit,
    onToggleScreenShare: () -> Unit,
    modifier: Modifier = Modifier,
) {

    SharedTransitionLayout(modifier.fillMaxSize()) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp

        val iconSize = 64.dp
        val floatingWidth = 220.dp
        val floatingHeight = 280.dp


        val floatingDefaultOffset = remember(screenWidth, screenHeight) {
            IntOffset(
                (screenWidth.value - floatingWidth.value - 16).roundToInt(),
                (screenHeight.value * 0.6f).roundToInt()
            )
        }

        var offset by remember { mutableStateOf(floatingDefaultOffset) }

        Box(
            modifier = Modifier
                .then(
                    if (uiState.size != GroupChatSize.FULLSCREEN) {
                        Modifier
                            .offset { offset }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    offset = offset.copy(
                                        x = (offset.x + dragAmount.x).roundToInt(),
                                        y = (offset.y + dragAmount.y).roundToInt()
                                    )
                                }
                            }
                    } else {
                        Modifier
                    }
                )
        ) {
            AnimatedContent(
                targetState = uiState.size,
                label = "GroupChatAnimatedContent",
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 300)) + scaleIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 300)) + scaleOut(animationSpec = tween(300))

                }
            ) { targetSize ->
                when (targetSize) {
                    GroupChatSize.FULLSCREEN, GroupChatSize.FLOATING -> {
                        ChatContainer(
                            uiState = uiState,
                            targetSize = targetSize,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedContentScope = this,
                            onMinimize = onMinimize,
                            onExpand = onExpand,
                            onInviteClick = onInvite,
                            onHangupClick = onEndGroupChat,
                            onEmojiShown = onEmojiShown,
                            onToggleVideoClick = onToggleVideo,
                            onToggleMicClick = onToggleMic,
                            onShowChatClick = onShowChatClick,
                            onShowSettings = onShowSettings,
                            onHideBottomSheet = onHideBottomSheet,
                            onToggleScreenShare = onToggleScreenShare,
                            modifier = when (targetSize) {
                                GroupChatSize.FULLSCREEN -> Modifier.fillMaxSize()
                                GroupChatSize.FLOATING -> Modifier.size(floatingWidth, floatingHeight)
                                else -> Modifier
                            }
                        )
                    }
                    GroupChatSize.ICON -> {
                        IconView(
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedContentScope = this,
                            onClick = onExpand,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    GroupChatSize.HIDDEN -> {}
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun IconView(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    with(sharedTransitionScope) {
        Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = "그룹챗으로 돌아가기",
            tint = Color.White,
            modifier = modifier
                .sharedElement(
                    rememberSharedContentState(key = "container-bounds"),
                    animatedVisibilityScope = animatedContentScope,
                    boundsTransform = boundsTransform
                )
                .clip(CircleShape)
                .background(Color(0xFF2C2C2E))
                .clickable(onClick = onClick)
                .padding(16.dp)
        )
    }
}
