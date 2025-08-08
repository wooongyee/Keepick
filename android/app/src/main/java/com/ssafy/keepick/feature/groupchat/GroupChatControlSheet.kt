package com.ssafy.keepick.feature.groupchat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 컨트롤 패널과 확장형 바텀시트를 모두 포함하는 메인 컴포저블
 */
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
    val myParticipantState = uiState.participants.find { it.isMe }
    val isSheetExpanded = uiState.bottomSheetContent != BottomSheetContent.NONE

    val containerColor by animateColorAsState(
        targetValue = if (isSheetExpanded) Color(0xFF242424) else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "sheetColor"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isSheetExpanded) 24.dp else 0.dp,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "sheetCornerRadius"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 드래그 가능한 핸들
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (delta < -10) onShowSettings() // 위로 드래그 시 설정 열기
                            if (delta > 10) onHideBottomSheet()  // 아래로 드래그 시 닫기
                        },
                    ),
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

            AnimatedVisibility(
                visible = isSheetExpanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp) // 콘텐츠 최대 높이 제한
                ) {
                    AnimatedContent(
                        targetState = uiState.bottomSheetContent,
                        label = "sheetContent"
                    ) { content ->
                        when (content) {
                            BottomSheetContent.CHAT -> ChatContent()
                            BottomSheetContent.SETTINGS -> SettingsContent(
                                myParticipantState = myParticipantState,
                                onToggleScreenShare = onToggleScreenShare
                            )
                            else -> Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }

            // 하단 컨트롤 버튼 (항상 표시)
            ChatControls(
                isFullScreen = true,
                myParticipantState = myParticipantState,
                onHangupClick = onHangupClick,
                onToggleVideoClick = onToggleVideoClick,
                onToggleMicClick = onToggleMicClick,
                onShowChatClick = onShowChatClick,
                modifier = Modifier.padding(bottom = 16.dp, top = if(isSheetExpanded) 8.dp else 0.dp)
            )
        }
    }
}

/**
 * 채팅 화면 콘텐츠 (Placeholder)
 */
@Composable
private fun ChatContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("채팅 내역이 여기에 표시됩니다.", color = Color.White)
    }
}

/**
 * 설정 화면 콘텐츠 (Discord UI 기반)
 */
@Composable
private fun SettingsContent(myParticipantState: Participant?, onToggleScreenShare: () -> Unit) {
    val isSharingScreen = myParticipantState?.isScreenSharing == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
            contentColor = if (isSharingScreen) Color(0xFF3BA55D) else Color.White,
            onClick = onToggleScreenShare
        )
        // 필요에 따라 여기에 더 많은 설정 항목을 추가할 수 있습니다.
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    contentColor: Color = Color.White,
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
