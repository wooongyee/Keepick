package com.ssafy.keepick.feature.highlight.album

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.R

@Composable
fun EmotionCard(
    section: EmotionSection,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val (iconRes, cardColor) = when (section.id) {
        "funny" -> R.drawable.funny to Color(0xFFFFEB3B)
        "serious" -> R.drawable.serious to Color(0xFFE91E63)
        "surprise" -> R.drawable.surprise to Color(0xFF2196F3)
        "tired" -> R.drawable.tired to Color(0xFF4CAF50)
        else -> R.drawable.ic_launcher_foreground to Color.Gray
    }

    // [개선] 노란색 배경에서 제목이 잘 보이도록 텍스트 색상을 동적으로 변경
    val titleColor = if (section.id == "funny") Color.Black.copy(alpha = 0.8f) else Color.White

    // [개선] 하단 텍스트의 가독성을 확보하기 위해 제어 가능한 그라데이션 적용
    val gradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to cardColor,
            0.6f to cardColor, // 카드의 상위 60%는 단색을 유지
            1.0f to Color.Black.copy(alpha = 0.8f) // 하위 40%는 검은색으로 점차 어두워짐
        )
    )

    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient) // [수정] 그라데이션 배경 적용
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = section.title,
                    modifier = Modifier.size(80.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = section.title,
                    color = titleColor, // [수정] 동적 텍스트 색상 적용
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
            // [수정] 하단 바를 제거하고 그라데이션 위에 직접 텍스트 배치
            Text(
                text = "${section.photos.size} photos",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
