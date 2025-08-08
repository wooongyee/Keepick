package com.ssafy.keepick.feature.groupchat

// --- 이 import 목록을 확인하고 적용하세요 ---
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.feature.groupchat.state.EmojiEvent
import com.ssafy.keepick.feature.groupchat.state.Participant
import kotlinx.coroutines.delay

@Composable
fun ParticipantCard(
    participant: Participant,
    modifier: Modifier = Modifier,
    isFloating: Boolean = false,
    emojiEvent: EmojiEvent?,
    onEmojiShown: (eventId: String) -> Unit
) {
    val showEmoji = emojiEvent != null

    if (showEmoji) {
        LaunchedEffect(emojiEvent) {
            delay(2000)
            onEmojiShown(emojiEvent!!.id)
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(participant.name, color = Color.White.copy(alpha = 0.7f))
            }

            if (!isFloating) {
                Text(
                    text = participant.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(
                            Color.Black.copy(alpha = 0.3f),
                            RoundedCornerShape(topEnd = 8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Box 스코프에서는 ColumnScope가 없는 범용 AnimatedVisibility가 사용되어야 합니다.
            Column(
                modifier = Modifier.fillMaxSize().align(Alignment.Center),
            ){
                AnimatedVisibility(
                    visible = showEmoji,
                    modifier = Modifier,
                    enter = slideInVertically { it / 2 } + fadeIn(tween(500)),
                    exit = fadeOut(tween(500))
                ) {
                    Text(emojiEvent?.emoji ?: "", fontSize = 48.sp)
                }

            }
        }
    }
}
