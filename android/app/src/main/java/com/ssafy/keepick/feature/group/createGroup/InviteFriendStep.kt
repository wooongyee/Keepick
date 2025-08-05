package com.ssafy.keepick.feature.group.createGroup

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.core.util.AppConstants
import com.ssafy.keepick.core.util.shareInviteLink

@Composable
internal fun InviteFriendsStep() {
    val friends = listOf("김싸피", "이싸피", "박싸피", "최싸피", "정싸피", "강싸피", "오싸피", "유싸피", "임싸피")
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val inviteCode = "1asdfj3123" // 예시 초대 코드 (실제로는 서버에서 받아와야 함)
    val inviteUrl = "${AppConstants.FRONTEND_URL}/invite/$inviteCode"

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 상단: 스크롤되지 않는 고정 영역 ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp), // 상단 아이콘이 사라졌으므로 패딩 조정
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 제목과 설명
            Text(
                "친구 초대",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "그룹에 초대할 친구들을 선택하거나 링크를 공유하세요.",
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // --- 수정된 초대 링크 섹션 ---
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "초대 링크",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp) // OutlinedTextField와 높이 유사하게 설정
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = inviteUrl,
                        modifier = Modifier.weight(1f), // 아이콘을 제외한 나머지 공간을 모두 차지
                        maxLines = 1, // 무조건 한 줄로 표시
                        overflow = TextOverflow.Ellipsis // 넘치는 텍스트는 ...으로 처리
                    )
                    // 복사 버튼
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(inviteUrl))
                        Toast.makeText(context, "초대 링크가 복사되었습니다.", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = "초대 링크 복사")
                    }
                    // 공유 버튼
                    IconButton(onClick = { shareInviteLink(context, inviteCode) }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "초대 링크 공유")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "친구 목록",
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- 하단: 스크롤되는 친구 목록 영역 ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(friends) { friendName ->
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
