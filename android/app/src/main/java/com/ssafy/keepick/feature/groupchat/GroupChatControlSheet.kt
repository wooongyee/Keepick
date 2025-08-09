package com.ssafy.keepick.feature.groupchat

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.splineBasedDecay
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.core.designsystem.theme.KeepickTheme
import kotlinx.coroutines.flow.distinctUntilChanged

private enum class SheetState { Collapsed, Expanded }
private enum class SheetContentSlot { Sheet, ExpandableContent }

private val ExpandedContentHeight = 300.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupChatControlSheet(
    modifier: Modifier = Modifier,
    uiState: GroupChatState,
    onHangupClick: () -> Unit,
    onToggleVideoClick: () -> Unit,
    onToggleMicClick: () -> Unit,
    onShowChatClick: () -> Unit,
    onShowSettings: () -> Unit,
    onHideBottomSheet: () -> Unit,
    onToggleScreenShare: () -> Unit,
) {
    val density = LocalDensity.current
    val expandedAnchor = with(density) { -ExpandedContentHeight.toPx() }
    val initialState = if (uiState.bottomSheetContent != BottomSheetContent.NONE) SheetState.Expanded else SheetState.Collapsed

    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = initialState,
            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = spring(),
            decayAnimationSpec = splineBasedDecay(density)
        ).apply {
            updateAnchors(
                DraggableAnchors {
                    SheetState.Collapsed at 0f
                    SheetState.Expanded at expandedAnchor
                }
            )
        }
    }

    LaunchedEffect(uiState.bottomSheetContent, draggableState.anchors) {
        Log.d("GroupChatControlSheet", "bottomSheetContent: ${uiState.bottomSheetContent}")
        val targetState = if (uiState.bottomSheetContent != BottomSheetContent.NONE) SheetState.Expanded else SheetState.Collapsed
        if (draggableState.anchors.hasAnchorFor(targetState)) {
            draggableState.animateTo(targetState)
        }
    }

    val currentOnHideBottomSheet by rememberUpdatedState(onHideBottomSheet)
    LaunchedEffect(draggableState.currentValue) {
        snapshotFlow { draggableState.currentValue }
            .distinctUntilChanged()
            .collect {
                if (it == SheetState.Collapsed && uiState.bottomSheetContent != BottomSheetContent.NONE) {
                    currentOnHideBottomSheet()
                }
            }
    }

    SheetContent(
        modifier = modifier,
        uiState = uiState,
        draggableState = draggableState,
        onHangupClick = onHangupClick,
        onToggleVideoClick = onToggleVideoClick,
        onToggleMicClick = onToggleMicClick,
        onShowChatClick = onShowChatClick,
        onToggleScreenShare = onToggleScreenShare
    )

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SheetContent(
    modifier: Modifier = Modifier,
    uiState: GroupChatState,
    draggableState: AnchoredDraggableState<SheetState>,
    onHangupClick: () -> Unit,
    onToggleVideoClick: () -> Unit,
    onToggleMicClick: () -> Unit,
    onShowChatClick: () -> Unit,
    onToggleScreenShare: () -> Unit
) {
    val myParticipantState = uiState.participants.find { it.isMe }
    val isSheetExpanded = draggableState.targetValue == SheetState.Expanded
    val containerColor by animateColorAsState(
        targetValue = if (isSheetExpanded) Color.Yellow else Color.Magenta,
        animationSpec = tween(300),
        label = "sheetColor"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isSheetExpanded) 24.dp else 0.dp,
        animationSpec = tween(300),
        label = "sheetCornerRadius"
    )
    val expandedAnchor = draggableState.anchors.positionOf(SheetState.Expanded)
    // requireOffset()은 앵커가 설정된 후에만 호출되어야 안전합니다.
    val progress = if (draggableState.anchors.size > 1 && expandedAnchor != 0f) {
        (draggableState.requireOffset() / expandedAnchor).coerceIn(0f, 1f)
    } else {
        if (isSheetExpanded) 1f else 0f
    }
    val dynamicContentHeight = ExpandedContentHeight * progress

    Surface(
        modifier = modifier
            .fillMaxWidth()
//            .offset {
//                IntOffset(
//                    0,
//                    draggableState
//                        .requireOffset()
//                        .roundToInt()/5
//                )
//            }
            .anchoredDraggable(
                state = draggableState,
                orientation = Orientation.Vertical,
            ),
        color = containerColor,
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SheetHandle()

            // [해결 2] 동적으로 계산된 높이를 전달합니다.
            SheetExpandableContent(
                uiState = uiState,
                myParticipantState = myParticipantState,
                onToggleScreenShare = onToggleScreenShare,
                height = dynamicContentHeight // 고정 높이 대신 동적 높이 사용
            )

            ChatControls(
                isFullScreen = true,
                isSheetExpanded = isSheetExpanded,
                myParticipantState = myParticipantState,
                onHangupClick = onHangupClick,
                onToggleVideoClick = onToggleVideoClick,
                onToggleMicClick = onToggleMicClick,
                onShowChatClick = onShowChatClick,
            )
        }
    }
}
@Composable
private fun SheetExpandableContent(
    uiState: GroupChatState,
    myParticipantState: Participant?,
    onToggleScreenShare: () -> Unit,
    height: Dp // 부모로부터 동적 높이를 받습니다.
) {
    // 이 Box가 드래그에 따라 높이가 0dp에서 300dp까지 커지는 역할을 합니다.
    Box(modifier = Modifier.height(height)) {
        AnimatedContent(
            targetState = uiState.bottomSheetContent,
            label = "sheetContent",
            transitionSpec = {
                // 애니메이션은 그대로 유지합니다.
                if (targetState != BottomSheetContent.NONE) {
                    slideInVertically(animationSpec = tween(300)) { fullHeight -> fullHeight } + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutVertically(animationSpec = tween(300)) { fullHeight -> -fullHeight } + fadeOut(animationSpec = tween(300))
                } else {
                    slideInVertically(animationSpec = tween(300)) { fullHeight -> -fullHeight } + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutVertically(animationSpec = tween(300)) { fullHeight -> fullHeight } + fadeOut(animationSpec = tween(300))
                }
            }
        ) { content ->
            when (content) {
                BottomSheetContent.CHAT -> ChatContent()
                BottomSheetContent.SETTINGS -> SettingsContent(myParticipantState, onToggleScreenShare)
                // NONE 상태에서는 빈 Box를 그려 높이가 0이 되도록 합니다.
                BottomSheetContent.NONE -> Box(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ChatContent() {
    Box(
        modifier = Modifier
            // [해결 3] 고정 높이 대신 부모가 준 높이를 모두 채웁니다.
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("채팅 내역이 여기에 표시됩니다.", color = Color.Black)
    }
}

@Composable
private fun SettingsContent(myParticipantState: Participant?, onToggleScreenShare: () -> Unit) {
    val isSharingScreen = myParticipantState?.isScreenSharing == true

    Column(
        modifier = Modifier
            // [해결 3] 고정 높이 대신 부모가 준 높이를 모두 채웁니다.
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsItem(
            icon = Icons.Default.Podcasts,
            title = "사운드보드",
            onClick = { /* TODO */ }
        )
        SettingsItem(
            icon = Icons.Default.ScreenShare,
            title = if (isSharingScreen) "화면 공유 중지" else "화면 공유하기",
            contentColor = if (isSharingScreen) Color(0xFF3BA55D) else Color.Black,
            onClick = onToggleScreenShare
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    contentColor: Color = Color.Black, // 배경색이 밝아졌으므로 글자색 변경
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = contentColor)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, color = contentColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.Gray)
        )
    }
}

@Composable
private fun GroupChatControlSheetPreviewWrapper(uiState: GroupChatState) {
    KeepickTheme {
        Box(modifier = Modifier.background(Color(0xFF1C1C1E))) {
            GroupChatControlSheet(
                uiState = uiState,
                onHangupClick = {},
                onToggleVideoClick = {},
                onToggleMicClick = {},
                onShowChatClick = {},
                onShowSettings = {},
                onHideBottomSheet = {},
                onToggleScreenShare = {}
            )
        }
    }
}

@Preview(name = "Sheet Collapsed", showBackground = true)
@Composable
private fun GroupChatControlSheetCollapsedPreview() {
    val mockUiState = GroupChatState(
        participants = listOf(
            Participant(id = "me", name = "jw", isMe = true)
        ),
        bottomSheetContent = BottomSheetContent.NONE
    )
    GroupChatControlSheetPreviewWrapper(uiState = mockUiState)
}

@Preview(name = "Sheet Expanded - Chat", showBackground = true)
@Composable
private fun GroupChatControlSheetExpandedChatPreview() {
    val mockUiState = GroupChatState(
        participants = listOf(
            Participant(id = "me", name = "jw", isMe = true)
        ),
        bottomSheetContent = BottomSheetContent.CHAT
    )
    GroupChatControlSheetPreviewWrapper(uiState = mockUiState)
}

@Preview(name = "Sheet Expanded - Settings", showBackground = true)
@Composable
private fun GroupChatControlSheetExpandedSettingsPreview() {
    val mockUiState = GroupChatState(
        participants = listOf(
            Participant(id = "me", name = "jw", isMe = true, isScreenSharing = true)
        ),
        bottomSheetContent = BottomSheetContent.SETTINGS
    )
    GroupChatControlSheetPreviewWrapper(uiState = mockUiState)
}
