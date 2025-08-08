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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay


/**
 * [수정 없음] 참가자 그리드 레이아웃의 메인 컴포저블
 * 화면 공유 여부에 따라 레이아웃을 분기하지 않고, 구조를 통합하여 중복을 제거.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ParticipantGrid(
    participants: List<Participant>,
    emojiEvents: Map<String, EmojiEvent>,
    onEmojiShown: (participantId: String, eventId: String) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
) {
    // --- 데이터 준비 ---
    var screenSharingParticipant by remember { mutableStateOf(participants.find { it.isScreenSharing }) }
    val isScreenSharing = screenSharingParticipant != null
    val gridParticipants = participants.filter { !it.isScreenSharing }

    // --- 테스트용 로직 ---
    LaunchedEffect(participants) {
        delay(6000)
        screenSharingParticipant = null // 6초 후 화면 공유 종료
    }

    // --- UI 구조 통합 ---
    Column(Modifier.fillMaxSize()) {
        if (screenSharingParticipant != null) {
            ScreenShareView(participant = screenSharingParticipant!!)
        }

        val pageSize = if (isScreenSharing) 4 else 6

        ParticipantPagerGrid(
            gridParticipants = gridParticipants,
            pageSize = pageSize,
            emojiEvents = emojiEvents,
            onEmojiShown = onEmojiShown,
            sharedTransitionScope = sharedTransitionScope,
            animatedContentScope = animatedContentScope,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ParticipantPagerGrid(
    gridParticipants: List<Participant>,
    pageSize: Int,
    emojiEvents: Map<String, EmojiEvent>,
    onEmojiShown: (participantId: String, eventId: String) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedContentScope: AnimatedContentScope?,
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

            when (pageParticipants.size) {
                1 -> ParticipantPageLayout_1(
                    pageParticipants,
                    emojiEvents,
                    onEmojiShown,
                    sharedTransitionScope,
                    animatedContentScope
                )

                2 -> ParticipantPageLayout_2(
                    pageParticipants,
                    emojiEvents,
                    onEmojiShown,
                    sharedTransitionScope,
                    animatedContentScope
                )

                3 -> ParticipantPageLayout_3(
                    pageParticipants,
                    emojiEvents,
                    onEmojiShown,
                    sharedTransitionScope,
                    animatedContentScope
                )

                4 -> ParticipantPageLayout_4(
                    pageParticipants,
                    emojiEvents,
                    onEmojiShown,
                    sharedTransitionScope,
                    animatedContentScope
                ) // [신규] 4인 레이아웃 호출
                else -> {
                    // 5명 이상은 모두 일반 2열 그리드 레이아웃 사용
                    val maxRows = (pageSize + 1) / 2
                    ParticipantPageLayout_Default(
                        pageParticipants,
                        maxRows,
                        emojiEvents,
                        onEmojiShown,
                        sharedTransitionScope,
                        animatedContentScope
                    )
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


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ParticipantPageLayout_1(
    participants: List<Participant>,
    emojiEvents: Map<String, EmojiEvent>,
    onEmojiShown: (String, String) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedContentScope: AnimatedContentScope?
) {
    val p = participants.first()
    val sharedContentState = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) { rememberSharedContentState(key = "participant-${p.id}") }
    } else null

    Box(Modifier
        .fillMaxSize()
        .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Row(Modifier.fillMaxHeight(0.5f)) {
            val videoModifier =
                if (p.isMe && sharedContentState != null && sharedTransitionScope != null && animatedContentScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            sharedContentState,
                            animatedContentScope
                        )
                    }
                } else Modifier
            ParticipantCard(
                participant = p,
                emojiEvent = emojiEvents[p.id],
                onEmojiShown = { onEmojiShown(p.id, it) },
                modifier = Modifier
                    .aspectRatio(1f)
                    .then(videoModifier)
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ParticipantPageLayout_2(
    participants: List<Participant>,
    emojiEvents: Map<String, EmojiEvent>,
    onEmojiShown: (String, String) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedContentScope: AnimatedContentScope?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        participants.forEach { p ->
            val sharedContentState = if (sharedTransitionScope != null) {
                with(sharedTransitionScope) { rememberSharedContentState(key = "participant-${p.id}") }
            } else null
            val videoModifier =
                if (p.isMe && sharedContentState != null && sharedTransitionScope != null && animatedContentScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            sharedContentState,
                            animatedContentScope
                        )
                    }
                } else Modifier

            ParticipantCard(
                participant = p,
                emojiEvent = emojiEvents[p.id],
                onEmojiShown = { onEmojiShown(p.id, it) },
                modifier = Modifier
                    .weight(1f, fill = false)
                    .aspectRatio(1f)
                    .fillMaxWidth(0.6f)
                    .then(videoModifier)
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ParticipantPageLayout_3(
    participants: List<Participant>,
    emojiEvents: Map<String, EmojiEvent>,
    onEmojiShown: (String, String) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedContentScope: AnimatedContentScope?
) {
    val topParticipant = participants.first()
    val bottomParticipants = participants.drop(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        // 상단 1명
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val p = topParticipant
            val sharedContentState = if (sharedTransitionScope != null) {
                with(sharedTransitionScope) { rememberSharedContentState(key = "participant-${p.id}") }
            } else null
            val videoModifier =
                if (p.isMe && sharedContentState != null && sharedTransitionScope != null && animatedContentScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            sharedContentState,
                            animatedContentScope
                        )
                    }
                } else Modifier
            ParticipantCard(
                participant = p,
                emojiEvent = emojiEvents[p.id],
                onEmojiShown = { onEmojiShown(p.id, it) },
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .then(videoModifier)
            )
        }
        // 하단 2명
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bottomParticipants.forEach { p ->
                val sharedContentState = if (sharedTransitionScope != null) {
                    with(sharedTransitionScope) { rememberSharedContentState(key = "participant-${p.id}") }
                } else null
                val videoModifier =
                    if (p.isMe && sharedContentState != null && sharedTransitionScope != null && animatedContentScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                sharedContentState,
                                animatedContentScope
                            )
                        }
                    } else Modifier
                ParticipantCard(
                    participant = p,
                    emojiEvent = emojiEvents[p.id],
                    onEmojiShown = { onEmojiShown(p.id, it) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(videoModifier)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ParticipantPageLayout_4(
    participants: List<Participant>,
    emojiEvents: Map<String, EmojiEvent>,
    onEmojiShown: (String, String) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedContentScope: AnimatedContentScope?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center // 그리드를 중앙에 배치
    ) {
        Column(
            // 그리드 컨테이너의 크기를 제한하여 중앙 정렬 효과를 줌
            modifier = Modifier
                .fillMaxWidth(1f)
                .aspectRatio(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rows = participants.chunked(2)
            rows.forEach { rowParticipants ->
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowParticipants.forEach { p ->
                        val sharedContentState = if (sharedTransitionScope != null) {
                            with(sharedTransitionScope) { rememberSharedContentState(key = "participant-${p.id}") }
                        } else null
                        val videoModifier =
                            if (p.isMe && sharedContentState != null && sharedTransitionScope != null && animatedContentScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedElement(
                                        sharedContentState,
                                        animatedContentScope
                                    )
                                }
                            } else Modifier

                        ParticipantCard(
                            participant = p,
                            emojiEvent = emojiEvents[p.id],
                            onEmojiShown = { onEmojiShown(p.id, it) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .then(videoModifier)
                        )
                    }
                }
            }
        }
    }
}


/**
 * [수정] 4명 이상의 참가자를 위한 일반 2열 그리드 레이아웃
 * 기존의 ParticipantPageLayout 역할을 수행한다.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ParticipantPageLayout_Default(
    participants: List<Participant>,
    maxRows: Int,
    emojiEvents: Map<String, EmojiEvent>,
    onEmojiShown: (String, String) -> Unit,
    sharedTransitionScope: SharedTransitionScope?,
    animatedContentScope: AnimatedContentScope?
) {
    val rows = participants.chunked(2)
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
                    val sharedContentState = if (sharedTransitionScope != null) {
                        with(sharedTransitionScope) { rememberSharedContentState(key = "participant-${p.id}") }
                    } else null
                    val videoModifier =
                        if (p.isMe && sharedContentState != null && sharedTransitionScope != null && animatedContentScope != null) {
                            with(sharedTransitionScope) {
                                Modifier.sharedElement(
                                    sharedContentState,
                                    animatedContentScope
                                )
                            }
                        } else Modifier
                    ParticipantCard(
                        participant = p,
                        emojiEvent = emojiEvents[p.id],
                        onEmojiShown = { eventId -> onEmojiShown(p.id, eventId) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(videoModifier)
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


// --- 기존 유틸리티 및 Preview 코드 (변경 없음) ---

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
            Box(modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color))
        }
    }
}

private val dummyParticipants = listOf(
    Participant(id = "user_me", name = "나", isMe = true),
    Participant(id = "p1", name = "Pikachu"), Participant(id = "p2", name = "Squirtle"),
    Participant(id = "p3", name = "Bulbasaur"), Participant(id = "p4", name = "Charmander"),
    Participant(id = "p5", name = "Eevee"), Participant(id = "p6", name = "Snorlax"),
)

@Composable
fun ParticipantCard(
    participant: Participant,
    emojiEvent: EmojiEvent?,
    onEmojiShown: (String) -> Unit,
    modifier: Modifier
) {
    Box(
        modifier.background(Color.Gray.copy(0.3f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(participant.name, color = Color.White)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1C1C1E,
    widthDp = 411,
    heightDp = 891,
    name = "No Share (1)"
)
@Composable
private fun Preview_NoShare_1() {
    ParticipantGrid(
        participants = dummyParticipants.take(1),
        emojiEvents = emptyMap(),
        onEmojiShown = { _, _ -> })
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1C1C1E,
    widthDp = 411,
    heightDp = 891,
    name = "No Share (2)"
)
@Composable
private fun Preview_NoShare_2() {
    ParticipantGrid(
        participants = dummyParticipants.take(2),
        emojiEvents = emptyMap(),
        onEmojiShown = { _, _ -> })
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1C1C1E,
    widthDp = 411,
    heightDp = 891,
    name = "No Share (3)"
)
@Composable
private fun Preview_NoShare_3() {
    ParticipantGrid(
        participants = dummyParticipants.take(3),
        emojiEvents = emptyMap(),
        onEmojiShown = { _, _ -> })
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1C1C1E,
    widthDp = 411,
    heightDp = 891,
    name = "No Share (4)"
)
@Composable
private fun Preview_NoShare_4() {
    ParticipantGrid(
        participants = dummyParticipants.take(4),
        emojiEvents = emptyMap(),
        onEmojiShown = { _, _ -> })
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1C1C1E,
    widthDp = 411,
    heightDp = 891,
    name = "Screen Share (3 Others)"
)
@Composable
private fun Preview_Share_3() {
    val participants =
        listOf(dummyParticipants[0].copy(isScreenSharing = true)) + dummyParticipants.slice(1..3)
    ParticipantGrid(
        participants = participants,
        emojiEvents = emptyMap(),
        onEmojiShown = { _, _ -> })
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF1C1C1E,
    widthDp = 411,
    heightDp = 891,
    name = "No Share (7 Paged)"
)
@Composable
private fun Preview_NoShare_7_Paged() {
    ParticipantGrid(
        participants = dummyParticipants.take(7),
        emojiEvents = emptyMap(),
        onEmojiShown = { _, _ -> })
}
