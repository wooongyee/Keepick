package com.ssafy.keepick.feature.groupchat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ChatControls(
    isFullScreen: Boolean,
    modifier: Modifier = Modifier,
    myParticipantState: Participant?, // 내 상태를 받기 위한 파라미터 추가
    onHangupClick: () -> Unit,
    onToggleVideoClick: () -> Unit, // 파라미터 추가
    onToggleMicClick: () -> Unit,   // 파라미터 추가
    onShowChatClick: () -> Unit     // 파라미터 추가
) {
    val buttonSize = if (isFullScreen) 56.dp else 48.dp
    val containerColor = if (isFullScreen) Color.Transparent else Color.Black.copy(alpha = 0.2f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(if (isFullScreen) 0.dp else 32.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 카메라 버튼: isCameraOff 상태에 따라 아이콘과 토글 상태 변경
            ChatControlButton(
                icon = if (myParticipantState?.isCameraOff == true) Icons.Default.VideocamOff else Icons.Default.Videocam,
                size = buttonSize,
                isToggled = myParticipantState?.isCameraOff ?: false,
                onClick = onToggleVideoClick
            )
            // 마이크 버튼: isMuted 상태에 따라 아이콘과 토글 상태 변경
            ChatControlButton(
                icon = if (myParticipantState?.isMuted == true) Icons.Default.MicOff else Icons.Default.Mic,
                size = buttonSize,
                isToggled = myParticipantState?.isMuted ?: false,
                onClick = onToggleMicClick
            )

            // 전체화면일 때만 채팅 버튼 표시
            if (isFullScreen) {
                ChatControlButton(icon = Icons.AutoMirrored.Filled.Chat, size = buttonSize, onClick = onShowChatClick)
            }
            ChatControlButton(
                icon = Icons.Default.CallEnd,
                size = buttonSize,
                onClick = onHangupClick,
                isEndCall = true
            )
        }
    }
}

@Composable
fun ChatControlButton(
    icon: ImageVector,
    size: Dp,
    onClick: () -> Unit,
    isEndCall: Boolean = false,
    isToggled: Boolean = false // 토글 상태를 위한 파라미터 추가
) {
    // isToggled 상태가 true이면 (예: 음소거됨) 배경색을 다르게 표시
    val backgroundColor = when {
        isEndCall -> Color.Red
        isToggled -> Color(0xFF5E5E5E) // 활성화(토글됨) 색상
        else -> Color.DarkGray.copy(alpha = 0.8f)
    }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(icon, null, tint = Color.White)
    }
}
