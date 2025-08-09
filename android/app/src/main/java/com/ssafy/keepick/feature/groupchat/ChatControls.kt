package com.ssafy.keepick.feature.groupchat

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.keepick.core.designsystem.theme.SheetBackground

@Composable
fun ChatControls(
    isFullScreen: Boolean,
    isSheetExpanded: Boolean,
    modifier: Modifier = Modifier,
    myParticipantState: Participant?,
    onHangupClick: () -> Unit,
    onToggleVideoClick: () -> Unit,
    onToggleMicClick: () -> Unit,
    onShowChatClick: () -> Unit,
    isChatButtonEnabled: Boolean = true,
) {
    val buttonSize = if (isFullScreen) 56.dp else 48.dp

    val animatedPadding by animateDpAsState(
        targetValue = if (isSheetExpanded) 0.dp else 16.dp,
        animationSpec = tween(300),
        label = "paddingAnimation"
    )
    val animatedCorner by animateDpAsState(
        targetValue = if (isSheetExpanded) 0.dp else 32.dp,
        animationSpec = tween(300),
        label = "cornerRadiusAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = animatedPadding)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SheetBackground,
            shape = RoundedCornerShape(animatedCorner)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChatControlButton(
                    icon = if (myParticipantState?.isCameraOff == true) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    size = buttonSize,
                    isToggled = myParticipantState?.isCameraOff ?: false,
                    onClick = onToggleVideoClick
                )
                ChatControlButton(
                    icon = if (myParticipantState?.isMuted == true) Icons.Default.MicOff else Icons.Default.Mic,
                    size = buttonSize,
                    isToggled = myParticipantState?.isMuted ?: false,
                    onClick = onToggleMicClick
                )
                if (isFullScreen) {
                    ChatControlButton(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        size = buttonSize,
                        onClick = onShowChatClick,
                        enabled = isChatButtonEnabled
                    )
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
}

@Composable
fun ChatControlButton(
    icon: ImageVector,
    size: Dp,
    onClick: () -> Unit,
    isEndCall: Boolean = false,
    isToggled: Boolean = false,
    enabled: Boolean = true
) {
    val backgroundColor = when {
        isEndCall -> Color.Red
        isToggled -> Color(0xFF5E5E5E) // 활성화(토글됨) 색상
        else -> Color.DarkGray.copy(alpha = 0.8f)
    }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(icon, null, tint = Color.White)
    }
}
