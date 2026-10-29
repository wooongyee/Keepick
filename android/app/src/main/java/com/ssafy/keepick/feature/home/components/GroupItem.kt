package com.ssafy.keepick.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun GroupItem(
    groupImageUrl: String,
    groupName: String,
    groupDetail: String,
    memberImageUrls: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 그룹 프로필 이미지 (Coil 사용)
            AsyncImage(
                model = groupImageUrl, // 서버에서 받은 이미지 URL
                contentDescription = "그룹 프로필",
                contentScale = ContentScale.Crop, // 이미지가 뷰 크기에 맞게 잘리도록 설정
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray) // 로딩 중 보일 배경색
            )
            Spacer(modifier = Modifier.width(16.dp))
            // 그룹 정보
            Column(modifier = Modifier.weight(1f)) {
                Text(text = groupName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = groupDetail, color = Color.Gray, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            // 멤버 프로필 이미지 목록 (Coil 사용)
            Row {
                // 상위 3개의 이미지만 표시
                memberImageUrls.take(3).forEach { url ->
                    AsyncImage(
                        model = url, // 각 멤버의 이미지 URL
                        contentDescription = "멤버 프로필",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray)
                    )
                    Spacer(modifier = Modifier.width((-8).dp)) // 이미지 겹치기 효과
                }
            }
        }
    }
}
