package com.ssafy.keepick.feature.groupchat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlin.math.roundToInt

data class Participant(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)

@OptIn(ExperimentalSharedTransitionApi::class)
val boundsTransform = BoundsTransform { _, _ ->
    tween<Rect>(durationMillis = 300)
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GroupChatOverlay(
    uiState: GroupChatState,
    onMinimize: () -> Unit,
    onExpand: () -> Unit,
    onEndGroupChat: () -> Unit,
    onBackPress: () -> Unit,
    onInvite: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = uiState.size != GroupChatSize.HIDDEN) {
        onBackPress()
    }

    SharedTransitionLayout(
        modifier
            .fillMaxSize()
    ) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp

        val iconSize = 64.dp
        val floatingWidth = 240.dp
        val floatingHeight = 280.dp

        val floatingDefaultOffset = remember(screenWidth, screenHeight) {
            IntOffset(
                (screenWidth.value - floatingWidth.value).roundToInt(),
                (screenHeight.value - floatingHeight.value - 100).roundToInt()
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
                    fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 200))
                }
            ) { targetSize ->
                when (targetSize) {
                    GroupChatSize.FULLSCREEN, GroupChatSize.FLOATING -> {
                        ChatContainer(
                            targetSize = targetSize,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedContentScope = this,
                            onBackClick = onMinimize, onIconifyClick = onMinimize, onMaximizeClick = onExpand, onInviteClick = onInvite, onHangupClick = onEndGroupChat,
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
fun ChatContainer(
    targetSize: GroupChatSize,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onIconifyClick: () -> Unit,
    onMaximizeClick: () -> Unit,
    onInviteClick: () -> Unit,
    onHangupClick: () -> Unit
) {
    val participants = remember {
        listOf(
            Participant(id = "user_me", name = "나"),
            Participant(name = "Jaewoong"),
            Participant(name = "Jaewoong"),
            Participant(name = "연결중...")
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                with(sharedTransitionScope) {
                    Modifier.sharedBounds(
                        rememberSharedContentState(key = "container-bounds"),
                        animatedVisibilityScope = animatedContentScope,
                        boundsTransform = boundsTransform,
                    )
                }
            )
            .background(Color(0xFF2C2C2E))


    ) {
        Column(Modifier.fillMaxSize()) {
            // 헤더
            when (targetSize) {
                GroupChatSize.FULLSCREEN -> FullscreenHeader(onBackClick, onInviteClick)
                GroupChatSize.FLOATING -> FloatingHeader(onMaximizeClick, onIconifyClick)
                else -> {}
            }

            // 메인 콘텐츠 (LazyGrid 또는 단일 Box)
            // Modifier.weight(1f)를 사용하여 남은 공간을 모두 차지하도록 함.
            Box(modifier = Modifier.weight(1f)) {
                when (targetSize) {
                    GroupChatSize.FULLSCREEN -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(participants, key = { _, p -> p.id }) { index, participant ->
                                val videoModifier = if (index == 0) {
                                    with(sharedTransitionScope) {
                                        Modifier.sharedElement(
                                            rememberSharedContentState(key = "main-video"),
                                            animatedVisibilityScope = animatedContentScope,
                                            boundsTransform = boundsTransform
                                        )
                                    }
                                } else Modifier
                                ParticipantView(
                                    participant = participant,
                                    modifier = Modifier.aspectRatio(0.75f).then(videoModifier)
                                )
                            }
                        }
                    }
                    GroupChatSize.FLOATING -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    with(sharedTransitionScope) {
                                        Modifier.sharedElement(
                                            rememberSharedContentState(key = "main-video"),
                                            animatedVisibilityScope = animatedContentScope,
                                            boundsTransform = boundsTransform
                                        )
                                    }
                                )
                                .background(Color.Black)
                        ) { /* 실제 비디오 뷰 */ }
                    }
                    else -> {}
                }
            }

            // 컨트롤 바
            with(sharedTransitionScope) {
                ChatControls(
                    modifier = Modifier
                        .padding(if (targetSize == GroupChatSize.FULLSCREEN) 16.dp else 8.dp)
                        .sharedElement(
                            rememberSharedContentState(key = "controls"),
                            animatedVisibilityScope = animatedContentScope,
                            boundsTransform = boundsTransform
                        ),
                    buttonSize = if (targetSize == GroupChatSize.FULLSCREEN) 48.dp else 36.dp,
                    shape = if (targetSize == GroupChatSize.FULLSCREEN) CircleShape else RoundedCornerShape(16.dp),
                    containerColor = if (targetSize == GroupChatSize.FULLSCREEN) Color(0xFF333333) else Color.Gray.copy(alpha = 0.2f),
                    onHangupClick = onHangupClick,
                )
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
                .clip(CircleShape)
                .background(Color(0xFF2C2C2E))
                .clickable(onClick = onClick)
                .padding(16.dp)
                // [수정] 아이콘은 이제 '내 영상'과 연결됨
                .sharedElement(
                    rememberSharedContentState(key = "main-video"),
                    animatedVisibilityScope = animatedContentScope,
                    boundsTransform = boundsTransform
                )
        )
    }
}

// --- 이하 Helper 및 Preview 코드는 변경 없음 ---
@Composable
private fun FloatingHeader(onMaximizeClick: () -> Unit, onIconifyClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Green))
        Spacer(Modifier.width(8.dp))
        Text("가족 모임 그룹챗", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.Fullscreen, "전체화면", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp).clickable(onClick = onMaximizeClick))
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Default.Close, "아이콘으로", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp).clickable(onClick = onIconifyClick))
    }
}

@Composable
private fun FullscreenHeader(onBackClick: () -> Unit, onInviteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = Color.White) }
        Text("그룹챗", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onInviteClick) { Icon(Icons.Default.PersonAdd, "초대하기", tint = Color.White) }
    }
}


@Composable
fun ChatControls(
    modifier: Modifier = Modifier,
    buttonSize: Dp,
    shape: Shape,
    containerColor: Color,
    onHangupClick: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatControlButton(icon = Icons.Default.Videocam, size = buttonSize, onClick = { /*TODO*/ })
            ChatControlButton(icon = Icons.Default.MicOff, size = buttonSize, onClick = { /*TODO*/ })
            ChatControlButton(icon = Icons.Default.VolumeOff, size = buttonSize, onClick = { /*TODO*/ })
            ChatControlButton(icon = Icons.AutoMirrored.Filled.Chat, size = buttonSize, onClick = { /*TODO*/ })
            ChatControlButton(icon = Icons.Default.CallEnd, size = buttonSize, onClick = onHangupClick, isEndCall = true)
        }
    }
}

@Composable
fun ParticipantView(participant: Participant, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.DarkGray)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(participant.name, color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ChatControlButton(icon: ImageVector, size: Dp, onClick: () -> Unit, isEndCall: Boolean = false) {
    IconButton(onClick = onClick, modifier = Modifier.size(size).clip(CircleShape).background(if (isEndCall) Color.Red else Color.Gray.copy(alpha = 0.5f))) {
        Icon(icon, null, tint = Color.White)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun GroupChatOverlayInteractivePreview() {
    var uiState by remember { mutableStateOf(GroupChatState(size = GroupChatSize.FULLSCREEN, isActive = true)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.LightGray)
    ) {
        if (uiState.isActive) {
            GroupChatOverlay(
                uiState = uiState,
                onMinimize = {
                    val nextSize = when (uiState.size) {
                        GroupChatSize.FULLSCREEN -> GroupChatSize.FLOATING
                        GroupChatSize.FLOATING -> GroupChatSize.ICON
                        else -> uiState.size
                    }
                    uiState = uiState.copy(size = nextSize)
                },
                onExpand = {
                    val nextSize = when (uiState.size) {
                        GroupChatSize.ICON -> GroupChatSize.FLOATING
                        GroupChatSize.FLOATING -> GroupChatSize.FULLSCREEN
                        else -> uiState.size
                    }
                    uiState = uiState.copy(size = nextSize)
                },
                onEndGroupChat = { uiState = uiState.copy(isActive = false) },
                onBackPress = {
                    val nextSize = when (uiState.size) {
                        GroupChatSize.FULLSCREEN -> GroupChatSize.FLOATING
                        GroupChatSize.FLOATING -> GroupChatSize.ICON
                        else -> uiState.size
                    }
                    uiState = uiState.copy(size = nextSize)
                },
                onInvite = {}
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Preview Controls", color = Color.White)
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { uiState = uiState.copy(size = GroupChatSize.FULLSCREEN, isActive = true) }) { Text("Full") }
                Button(onClick = { uiState = uiState.copy(size = GroupChatSize.FLOATING, isActive = true) }) { Text("Float") }
                Button(onClick = { uiState = uiState.copy(size = GroupChatSize.ICON, isActive = true) }) { Text("Icon") }
            }
        }
    }
}
