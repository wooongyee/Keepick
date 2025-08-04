package com.ssafy.keepick.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun AlbumItem(
    thumbnailUrl: String,
    albumTitle: String,
    photoCount: Int
) {
    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        // Box 대신 AsyncImage에 직접 Modifier를 적용하여
        // 로딩 중 배경색, 모양, 크기 비율을 모두 처리합니다.
        AsyncImage(
            model = thumbnailUrl, // 서버에서 받은 앨범 썸네일 URL
            contentDescription = "앨범 썸네일",
            contentScale = ContentScale.Crop, // 이미지를 뷰에 맞춰 자르기
            modifier = Modifier
                .aspectRatio(1f) // 정사각형 비율
                .clip(RoundedCornerShape(12.dp))
                .background(Color.LightGray) // 이미지를 로드하는 동안 표시될 배경색
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = albumTitle, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Text(text = "사진 ${photoCount}개", color = Color.Gray, fontSize = 12.sp)
    }
}
