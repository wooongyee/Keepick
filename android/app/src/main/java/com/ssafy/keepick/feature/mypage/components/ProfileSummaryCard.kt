package com.ssafy.keepick.feature.mypage.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
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
import com.ssafy.keepick.feature.mypage.DisplayProfileSummary


@Composable
fun ProfileSummaryCard(profile: DisplayProfileSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp) // 상하 패딩 축소
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = profile.imageUrl,
                contentDescription = "프로필 사진",
                modifier = Modifier
                    .size(72.dp) // 이미지 사이즈 축소
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) // 폰트 사이즈 축소
                Text(text = "프로필 수정하기", fontSize = 14.sp, color = Color.Gray)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "프로필 수정으로 이동",
                tint = Color.LightGray
            )
        }
        Spacer(modifier = Modifier.height(20.dp)) // 간격 축소
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ProfileStat(count = profile.friendCount.toString(), label = "친구")
            ProfileStat(count = profile.groupCount.toString(), label = "그룹")
            ProfileStat(count = profile.albumCount.toString(), label = "앨범")
        }
    }
}

@Composable
fun ProfileStat(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF8A2BE2))
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
    }
}
