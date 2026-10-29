package com.ssafy.keepick.feature.mypage.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ssafy.keepick.feature.mypage.DisplayFriend
import com.ssafy.keepick.feature.mypage.EmptyContentView
import com.ssafy.keepick.feature.mypage.SectionHeader


@Composable
fun SentRequestListContent(sentRequests: List<DisplayFriend>) {
    if (sentRequests.isEmpty()) {
        EmptyContentView(message = "보낸 친구 요청이 없습니다.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { SectionHeader(title = "보낸 요청 (${sentRequests.size})") }
        items(items = sentRequests, key = { "sent_${it.name}" }) { request ->
            FriendListItem(friend = request)
        }
    }
}


@Composable
fun FriendRequestItem(friend: DisplayFriend) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        FriendListItemBase(
            imageUrl = friend.imageUrl,
            name = friend.name,
            subtitle = friend.subtitle
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { /* 수락 */ },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8A2BE2)) // 포인트 컬러
                ) { Text("수락", color = Color.White) }
                Button(
                    onClick = { /* 거절 */ },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)) // 부드러운 회색
                ) { Text("거절", color = Color.DarkGray) }
            }
        }
    }
}
