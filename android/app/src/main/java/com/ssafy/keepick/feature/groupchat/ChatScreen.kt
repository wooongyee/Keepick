package com.ssafy.keepick.feature.groupchat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.R
import com.ssafy.keepick.core.designsystem.theme.ChatInputBackground
import com.ssafy.keepick.core.designsystem.theme.KeepickTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// --- 신규 채팅 UI 컴포넌트 ---

// 채팅 메시지 데이터 클래스
data class ChatMessage(
    val author: String,
    val timestamp: String,
    val content: String,
    val authorImage: Int? = null // 예시로 Int 리소스 사용
)

@Composable
fun ChatScreen(
    containerColor: Color
) {
    val mockMessages = remember {
        mutableStateOf(
            listOf(
                ChatMessage("JaeWoong", "2025.07.23. 오후 2:33", "하이요", R.drawable.dummy_profile),
                ChatMessage("JaeWoong", "2025.08.06. 오전 1:40", "어어ㅓ", R.drawable.dummy_profile),
                ChatMessage("keepick", "2025.08.09. 오전 10:00", "채팅 UI 테스트입니다. 스크롤이 잘 되는지 확인해보세요.", R.drawable.dummy_profile),
                ChatMessage("JaeWoong", "2025.07.23. 오후 2:33", "하이요", R.drawable.dummy_profile),
                ChatMessage("JaeWoong", "2025.08.06. 오전 1:40", "어어ㅓ", R.drawable.dummy_profile),
                ChatMessage("JaeWoong", "2025.07.23. 오후 2:33", "하이요", R.drawable.dummy_profile),
                ChatMessage("JaeWoong", "2025.08.06. 오전 1:40", "어어ㅓ", R.drawable.dummy_profile),
            )
        )
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor)
    ) {
        // [수정] 헤더를 LazyColumn 밖으로 분리
        ChatHeader(groupName = "Keepick 스터디")

        // 메시지 목록
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // [삭제] WelcomeMessage item 제거
            items(mockMessages.value) { message ->
                ChatMessageItem(message = message)
            }
        }

        // 메시지 입력창
        ChatInput(
            onMessageSent = { text ->
                val newMessage = ChatMessage(
                    author = "Me",
                    timestamp = SimpleDateFormat("yyyy.MM.dd. a h:mm", Locale.KOREA).format(Date()),
                    content = text,
                    authorImage = R.drawable.dummy_profile
                )
                mockMessages.value = mockMessages.value + newMessage
                coroutineScope.launch {
                    listState.animateScrollToItem(mockMessages.value.size)
                }
            },
            onFocus = {
                coroutineScope.launch {
                    listState.animateScrollToItem(mockMessages.value.size)
                }
            }
        )
    }
}

@Composable
fun ChatHeader(groupName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
//        Image(
//            painter = painterResource(id = R.drawable.dummy_profile), // 그룹 프로필 이미지
//            contentDescription = "$groupName profile image",
//            modifier = Modifier
//                .size(64.dp)
//                .clip(CircleShape)
//                .background(Color(0xFF4E5058)),
//            contentScale = ContentScale.Crop
//        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = "채팅방에 오신 걸 환영합니다!",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "여기는 $groupName 그룹 채팅방이에요.",
                color = Color(0xFFB5BAC1),
                fontSize = 14.sp
            )
        }
    }
}


// 개별 채팅 메시지 아이템
@Composable
fun ChatMessageItem(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // 아바타
        if (message.authorImage != null) {
            Image(
                painter = painterResource(id = message.authorImage),
                contentDescription = "${message.author} avatar",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF2A634)), // 디스코드 기본 아바타 색
                contentScale = ContentScale.Crop
            )
        } else {
            // 이미지가 없을 경우 기본 아이콘
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF2A634))
            )
        }


        Spacer(modifier = Modifier.width(12.dp))

        Column {
            // 작성자 및 타임스탬프
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.author,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message.timestamp,
                    color = Color(0xFF949BA4),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // 메시지 내용
            Text(
                text = message.content,
                color = Color(0xFFDBDEE1),
                fontSize = 16.sp,
                lineHeight = 22.sp // 가독성을 위한 줄 간격
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class) // [추가]
@Composable
fun ChatInput(
    onMessageSent: (String) -> Unit,
    onFocus: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val textStyle = TextStyle(color = Color.White, fontSize = 16.sp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
    ) {

            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onFocus()
                        }
                    },
                textStyle = textStyle,
                cursorBrush = SolidColor(Color.White),
            ) { innerTextField ->
                Surface(
                    color = ChatInputBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp) // 둥근 모서리 적용
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            innerTextField()
                            if (text.isEmpty()) {
                                Text(
                                    text = "그룹에 메시지 보내기",
                                    color = Color(0xFF949BA4),
                                    style = textStyle
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        val isSendEnabled = text.isNotBlank()
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "메시지 전송",
                            tint = if (isSendEnabled) Color(0xFF7289DA) else Color(0xFFB5BAC1),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(enabled = isSendEnabled) {
                                    onMessageSent(text)
                                    text = ""
                                }
                        )
                    }
                }
            }
    }
}

@Preview(name = "Chat Input Preview", showBackground = true)
@Composable
private fun ChatInputPreview() {
    KeepickTheme {
        Surface(color = Color(0xFF2B2D31)) { // 실제 배경과 유사한 색상으로 감싸기
            ChatInput(
                onMessageSent = {},
                onFocus = {}
            )
        }
    }
}
