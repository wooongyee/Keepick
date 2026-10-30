package com.ssafy.keepick.feature.groupchat

import android.graphics.Rect
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

private val SHARED_ELEMENT_BOUNDS_TRANSFORM_SPEC: AnimationSpec<Rect> = tween(durationMillis = 500)
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ParticipantGrid(
    participants: List<Participant>,
    emojiEvents: Map<String, EmojiEvent>,
    onEmojiShown: (participantId: String, eventId: String) -> Unit,
    onParticipantClick: (participantId: String) -> Unit,
) {
    val screenSharingParticipant = participants.find { it.isScreenSharing }
    val isScreenSharing = screenSharingParticipant != null
    val gridParticipants = participants.filter { !it.isScreenSharing }

    Column(Modifier.fillMaxSize()) {
        if (screenSharingParticipant != null) {
            ScreenShareView(participant = screenSharingParticipant)
        }

        val pageSize = if (isScreenSharing) 4 else 6

        ParticipantPagerGrid(
            gridParticipants = gridParticipants,
            pageSize = pageSize,
            emojiEvents = emojiEvents,
            onEmojiShown = onEmojiShown,
            modifier = Modifier.weight(1f),
            onParticipantClick = onParticipantClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun ParticipantPagerGrid(
    gridParticipants: List<Participant>,
    pageSize: Int,
    emojiEvents: Map<String, EmojiEvent>,
    onEmojiShown: (participantId: String, eventId: String) -> Unit,
    onParticipantClick: (participantId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (gridParticipants.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("참가자가 없습니다.", color = Color.White)
        }
        return
    }

    val pages = gridParticipants.chunked(pageSize)
    val pagerState = rememberPagerState { pages.size }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) { pageIndex ->
            val pageParticipants = pages.getOrNull(pageIndex) ?: emptyList()
            val commonParams = ParticipantLayoutParams(
                participants = pageParticipants,
                emojiEvents = emojiEvents,
                onEmojiShown = onEmojiShown,
                onParticipantClick = onParticipantClick,
            )
            when (pageParticipants.size) {
                1 -> ParticipantPageLayout_1(commonParams)
                2 -> ParticipantPageLayout_2(commonParams)
                3 -> ParticipantPageLayout_3(commonParams)
                4 -> ParticipantPageLayout_4(commonParams)
                else -> {
                    val maxRows = (pageSize + 1) / 2
                    ParticipantPageLayout_Default(commonParams, maxRows)
                }
            }
        }
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

/**
 * [신규] 반복적인 파라미터를 묶는 데이터 클래스
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private data class ParticipantLayoutParams(
    val participants: List<Participant>,
    val emojiEvents: Map<String, EmojiEvent>,
    val onEmojiShown: (participantId: String, eventId: String) -> Unit,
    val onParticipantClick: (participantId: String) -> Unit,
)

/**
 * [신규] ParticipantCard와 공통 Modifier를 묶은 헬퍼 컴포저블
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GridParticipantItem(
    participant: Participant,
    emojiEvent: EmojiEvent?,
    onEmojiShown: (eventId: String) -> Unit,
    onParticipantClick: (participantId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    with(LocalSharedTransitionScope.current) {
        ParticipantCard(
            participant = participant,
            emojiEvent = emojiEvent,
            onEmojiShown = onEmojiShown,
            modifier = modifier
                .clickable { onParticipantClick(participant.id) }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ParticipantPageLayout_1(params: ParticipantLayoutParams) {
    val p = params.participants.first()
    Box(
        Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp), contentAlignment = Alignment.Center
    ) {
        Row(Modifier.fillMaxHeight(0.5f)) {
            GridParticipantItem(
                participant = p,
                emojiEvent = params.emojiEvents[p.id],
                onEmojiShown = { params.onEmojiShown(p.id, it) },
                onParticipantClick = params.onParticipantClick,
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ParticipantPageLayout_2(params: ParticipantLayoutParams) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        params.participants.forEach { p ->
            GridParticipantItem(
                participant = p,
                emojiEvent = params.emojiEvents[p.id],
                onEmojiShown = { params.onEmojiShown(p.id, it) },
                onParticipantClick = params.onParticipantClick,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .aspectRatio(1f)
                    .fillMaxWidth(0.6f)
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ParticipantPageLayout_3(params: ParticipantLayoutParams) {
    val topParticipant = params.participants.first()
    val bottomParticipants = params.participants.drop(1)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            GridParticipantItem(
                participant = topParticipant,
                emojiEvent = params.emojiEvents[topParticipant.id],
                onEmojiShown = { params.onEmojiShown(topParticipant.id, it) },
                onParticipantClick = params.onParticipantClick,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
            )
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bottomParticipants.forEach { p ->
                GridParticipantItem(
                    participant = p,
                    emojiEvent = params.emojiEvents[p.id],
                    onEmojiShown = { params.onEmojiShown(p.id, it) },
                    onParticipantClick = params.onParticipantClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ParticipantPageLayout_4(params: ParticipantLayoutParams) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth() // 너비를 채우고
                .aspectRatio(1f), // 정사각형 비율을 유지
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rows = params.participants.chunked(2)
            rows.forEach { rowParticipants ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowParticipants.forEach { p ->
                        GridParticipantItem(
                            participant = p,
                            emojiEvent = params.emojiEvents[p.id],
                            onEmojiShown = { params.onEmojiShown(p.id, it) },
                            onParticipantClick = params.onParticipantClick,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ParticipantPageLayout_Default(params: ParticipantLayoutParams, maxRows: Int) {
    val rows = params.participants.chunked(2)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowParticipants ->
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowParticipants.forEach { p ->
                    GridParticipantItem(
                        participant = p,
                        emojiEvent = params.emojiEvents[p.id],
                        onEmojiShown = { params.onEmojiShown(p.id, it) },
                        onParticipantClick = params.onParticipantClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                if (rowParticipants.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        repeat(maxRows - rows.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}


// --- 유틸리티 및 Preview 코드 ---

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
private fun HorizontalPagerIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val activeColor = Color.White
        val inactiveColor = Color.Gray.copy(alpha = 0.5f)
        repeat(pagerState.pageCount) { iteration ->
            val color = if (pagerState.currentPage == iteration) activeColor else inactiveColor
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
