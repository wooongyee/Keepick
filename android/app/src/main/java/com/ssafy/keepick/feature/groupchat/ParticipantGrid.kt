package com.ssafy.keepick.feature.groupchat

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.feature.groupchat.state.EmojiEvent
import com.ssafy.keepick.feature.groupchat.state.Participant

// [수정] Pager 사용을 위해 ExperimentalFoundationApi 옵트인 추가
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun ParticipantGrid(
    participants: List<Participant>,
    emojiEvents: Map<String, EmojiEvent>,
    onEmojiShown: (participantId: String, eventId: String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    // [수정] 화면 공유자와 일반 참여자를 명확하게 분리
    val screenSharingParticipant = participants.find { it.isScreenSharing }
    val gridParticipants = participants.filter { !it.isScreenSharing }

    // 일반 참여자 목록을 4명씩 나누어 페이지 목록을 생성
    val pages = gridParticipants.chunked(4)
    val pagerState = rememberPagerState { pages.size }

    Column(Modifier.fillMaxSize()) {
        // 1. [수정] 화면 공유 뷰 호출 (화면 공유자가 있을 때만 표시)
        if (screenSharingParticipant != null) {
            ScreenShareView(participant = screenSharingParticipant)
        }

        // 2. [수정] 참여자 그리드 페이저와 인디케이터를 담는 Box
        // weight(1f)를 주어 남은 공간을 모두 차지하도록 함
        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) { pageIndex ->
                val pageParticipants = pages.getOrNull(pageIndex) ?: emptyList()

                LazyVerticalGrid(
                    columns = if (pageParticipants.size < 3) GridCells.Fixed(1) else GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pageParticipants, key = { "page${pageIndex}_${it.id}" }) { participant ->
                        val videoModifier = if (participant.isMe) {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    rememberSharedContentState(key = "main-video"),
                                    animatedVisibilityScope = animatedContentScope,
                                    boundsTransform = boundsTransform
                                )
                            }
                        } else Modifier

                        ParticipantCard(
                            participant = participant,
                            emojiEvent = emojiEvents[participant.id],
                            onEmojiShown = { eventId -> onEmojiShown(participant.id, eventId) },
                            modifier = Modifier
                                .aspectRatio(0.75f)
                                .then(videoModifier)
                        )
                    }
                }
            }

            // 3. [수정] 페이저 인디케이터 호출 (페이지가 2개 이상일 때만 표시)
            if (pages.size > 1) {
                HorizontalPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

// --- [수정] Helper Composable들을 ParticipantGrid 밖의 private 함수로 이동 ---

@Composable
private fun ScreenShareView(participant: Participant) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
            .padding(8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${participant.name}님의 화면 공유",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HorizontalPagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.Gray.copy(alpha = 0.5f)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(pagerState.pageCount) { iteration ->
            val color = if (pagerState.currentPage == iteration) activeColor else inactiveColor
            Box(
                modifier = Modifier
                    .size(8.dp)
                    // [수정] 원 모양으로 만들기 위해 clip 추가
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
