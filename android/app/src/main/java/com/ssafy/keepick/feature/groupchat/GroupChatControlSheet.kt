package com.ssafy.keepick.feature.groupchat

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.keepick.core.designsystem.theme.ChatBackground
import com.ssafy.keepick.core.designsystem.theme.ChatHandleBar
import com.ssafy.keepick.core.designsystem.theme.KeepickTheme
import com.ssafy.keepick.core.designsystem.theme.SheetBackground
import kotlinx.coroutines.flow.distinctUntilChanged


// BottomSheetContent에서 SETTINGS 제거
private enum class SheetState { Collapsed, Expanded }

private val ExpandedContentHeight = 480.dp // 채팅창이 더 많은 내용을 표시할 수 있도록 높이 증가

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupChatControlSheet(
    modifier: Modifier = Modifier,
    uiState: GroupChatState,
    onHangupClick: () -> Unit,
    onToggleVideoClick: () -> Unit,
    onToggleMicClick: () -> Unit,
    onShowChatClick: () -> Unit,
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

    // 사용자가 직접 드래그해서 시트를 닫으면 상태 업데이트
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
        onShowChatClick = onShowChatClick
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
) {
    val myParticipantState = uiState.participants.find { it.isMe }
    val isSheetExpanded = draggableState.targetValue == SheetState.Expanded || draggableState.currentValue == SheetState.Expanded
    val isSheetAnimating = draggableState.isAnimationRunning


    val containerColor by animateColorAsState(
        targetValue = if (isSheetExpanded)  SheetBackground else ChatBackground,
        animationSpec = tween(300),
        label = "sheetColor"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isSheetExpanded) 24.dp else 0.dp,
        animationSpec = tween(300),
        label = "sheetCornerRadius"
    )
    val progress by remember {
        derivedStateOf {
            val expandedAnchor = draggableState.anchors.positionOf(SheetState.Expanded)
            if (draggableState.anchors.size > 1 && expandedAnchor != 0f) {
                (draggableState.requireOffset() / expandedAnchor).coerceIn(0f, 1f)
            } else {
                if (isSheetExpanded) 1f else 0f
            }
        }
    }
    val dynamicContentHeight = ExpandedContentHeight * progress

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .anchoredDraggable(
                state = draggableState,
                orientation = Orientation.Vertical,
            ),
        color = containerColor,
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SheetHandle()

            // 확장 가능한 콘텐츠 영역: 항상 ChatScreen을 포함
            SheetExpandableContent(
                height = dynamicContentHeight,
                containerColor = containerColor
            )

            ChatControls(
                isFullScreen = true,
                isSheetExpanded = isSheetExpanded,
                myParticipantState = myParticipantState,
                onHangupClick = onHangupClick,
                onToggleVideoClick = onToggleVideoClick,
                onToggleMicClick = onToggleMicClick,
                onShowChatClick = onShowChatClick,
                isChatButtonEnabled = !isSheetAnimating,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SheetExpandableContent(
    height: Dp,
    containerColor : Color
) {
    Box(
        modifier = Modifier
            .height(height)
            .fillMaxWidth()
    ) {
        // 높이가 0 이상일 때만 내용을 그려 성능 최적화
        if (height > 0.dp) {
            ChatScreen(
                containerColor = containerColor
            )
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(ChatHandleBar) // [수정]
        )
    }
}

@Composable
private fun GroupChatControlSheetPreviewWrapper(uiState: GroupChatState) {
    // R 클래스를 찾을 수 없으므로 Preview에서는 KeepickTheme만 사용
    KeepickTheme {
        Box(modifier = Modifier.background(Color(0xFF1C1C1E))) {
            GroupChatControlSheet(
                uiState = uiState,
                onHangupClick = {},
                onToggleVideoClick = {},
                onToggleMicClick = {},
                onShowChatClick = {},
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

/*
// --- 주석 처리된 Settings 관련 코드 ---

@Composable
private fun SettingsContent(myParticipantState: Participant?, onToggleScreenShare: () -> Unit) {
    val isSharingScreen = myParticipantState?.isScreenSharing == true

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            contentColor = if (isSharingScreen) Color(0xFF3BA55D) else Color.White, // 어두운 배경에 맞춰 글자색 변경
            onClick = onToggleScreenShare
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    contentColor: Color = Color.White, // 기본 글자색 흰색으로 변경
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

@Preview(name = "Sheet Expanded - Settings", showBackground = true)
@Composable
private fun GroupChatControlSheetExpandedSettingsPreview() {
    val mockUiState = GroupChatState(
        participants = listOf(
            Participant(id = "me", name = "jw", isMe = true, isScreenSharing = true)
        ),
        // SETTINGS를 CHAT으로 변경해야 Preview가 동작합니다.
        // bottomSheetContent = BottomSheetContent.SETTINGS
        bottomSheetContent = BottomSheetContent.CHAT
    )
    GroupChatControlSheetPreviewWrapper(uiState = mockUiState)
}
*/
