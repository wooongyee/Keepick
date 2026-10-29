package com.ssafy.keepick.feature.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
import com.ssafy.keepick.feature.timeline.TimelineMoment

/**
 * LazyColumn에 표시될 모먼트 카드. 애니메이션 로직 없이 순수하게 UI를 표시하고 자신의 위치를 보고하는 역할만 한다.
 */

@Composable
fun TimelineMomentCard(
    moment: TimelineMoment,
    onClick: () -> Unit,
    onRegisterBounds: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(24.dp)

    Card(
        modifier = modifier
            .onGloballyPositioned { onRegisterBounds(it.boundsInWindow()) }
            .aspectRatio(3f / 4f) // 카드의 비율 고정
            .clip(cardShape)
            .clickable(onClick = onClick),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 배경: 블러 효과를 제거하고 선명한 이미지를 사용
            val firstImage = moment.photos.firstOrNull()
            if (firstImage != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(firstImage.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "모먼트 대표 이미지",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // 이미지가 없을 경우의 기본 배경
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            // 텍스트 가독성을 위한 하단 그라데이션 오버레이
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 400f
                        )
                    )
            )

            // 전경: 텍스트 정보
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = moment.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = moment.date,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 266)
@Composable
fun PreviewTimelineMomentCardWithImage() {
    MaterialTheme {
        TimelineMomentCard(
            moment = TimelineMoment(
                id = "1",
                date = "2025년 08월 07일",
                title = "행복했던 제주도 여행 ✈️",
                description = "잊을 수 없는 추억이었어요!",
                photos = listOf(
                    DisplayGroupAlbum(
                        id = 101,
                        thumbnailUrl = "https://picsum.photos/id/237/200/300",
                        title = "제주도 바다",
                        photoCount = 1,
                        date = "2025-08-07",
                        creator = "Keepick"
                    )
                )
            ),
            onClick = {},
            onRegisterBounds = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 266)
@Composable
fun PreviewTimelineMomentCardNoImage() {
    MaterialTheme {
        TimelineMomentCard(
            moment = TimelineMoment(
                id = "2",
                date = "2025년 01월 15일",
                title = "새해 계획 세우기",
                description = "올해는 어떤 목표를 이뤄볼까?",
                photos = emptyList()
            ),
            onClick = {},
            onRegisterBounds = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 200, heightDp = 266)
@Composable
fun PreviewTimelineMomentCardLongTitle() {
    MaterialTheme {
        TimelineMomentCard(
            moment = TimelineMoment(
                id = "3",
                date = "2024년 07월 01일",
                title = "정말 길고 긴 제목의 모먼트입니다. 과연 다 보일까요?",
                description = "내용도 길게 써봐야지.",
                photos = listOf(
                    DisplayGroupAlbum(
                        id = 103,
                        thumbnailUrl = "https://picsum.photos/id/21/200/300",
                        title = "책",
                        photoCount = 5,
                        date = "2024-07-01",
                        creator = "Keepick"
                    )
                )
            ),
            onClick = {},
            onRegisterBounds = {}
        )
    }
}
