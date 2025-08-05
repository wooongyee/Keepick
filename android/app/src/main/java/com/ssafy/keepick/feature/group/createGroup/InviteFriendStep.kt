package com.ssafy.keepick.feature.group.createGroup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun InviteFriendsStep() { // ColumnScope와 onCreate 파라미터 제거
    val friends = listOf("김싸피", "이싸피", "박싸피", "최싸피", "정싸피", "강싸피") // 스크롤 테스트를 위해 항목 추가

    // 전체 콘텐츠를 담는 Column. weight는 제거합니다.
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 상단 중앙 정렬 섹션 ---
        Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = "친구 초대 아이콘",
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.5f))
                .padding(16.dp)
                .align(Alignment.CenterHorizontally)
        )
        Text(
            "친구 초대",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            "그룹에 초대할 친구들을 선택하거나 링크를 공유하세요.",
            color = Color.Gray,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(8.dp)) // 섹션 간 여백

        // --- 하단 좌측 정렬 섹션 ---
        Text("초대 링크", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = "https://keepick.com/invite/1asdfj3123",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { /* TODO: 링크 복사 로직 */ }) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "링크 복사")
                }
            }
        )

        Text("친구 목록", fontWeight = FontWeight.Bold)

        // 1. LazyColumn을 일반 Column으로 변경하고, forEach를 사용합니다.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            friends.forEach { friendName ->
                ListItem(
                    headlineContent = { Text(friendName) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                        )
                    }
                )
            }
        }
    }
}
