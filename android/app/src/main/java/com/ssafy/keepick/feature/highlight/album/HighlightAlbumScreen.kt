package com.ssafy.keepick.feature.highlight.album

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.R
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

// 데이터 클래스와 더미 데이터 생성 함수는 이전과 동일합니다.
data class EmotionSection(
    val id: String,
    val title: String,
    val photos: List<DisplayGroupAlbum>
)

fun createDummyData(): List<EmotionSection> {
    val photos = (1..20).map {
        DisplayGroupAlbum(
            id = it,
            thumbnailUrl = "https://picsum.photos/seed/${Random.nextInt()}/400/600",
            title = "사진 $it", photoCount = 0, date = "", creator = ""
        )
    }
    return listOf(
        EmotionSection("funny", "빵터졌던 순간", photos.slice(0..4)),
        EmotionSection("serious", "진지했던 순간", photos.slice(5..9)),
        EmotionSection("surprise", "놀랐던 순간", photos.slice(10..14)),
        EmotionSection("tired", "잠오는 순간", photos.slice(15..19))
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightAlbumScreen(
    albumId: String,
    albumTitle: String,
    emotionSections: List<EmotionSection>,
    state: HighlightAlbumState, // [수정] ViewModel의 State를 직접 받음
    onNavigateBack: () -> Unit,
    onEmotionCardClick: (emotionId: String) -> Unit,
    onGiftBoxOpened: () -> Unit // [추가] 선물상자가 열렸을 때 호출할 함수
) {
    // [수정] 애니메이션 트리거 상태를 remember가 아닌 state에서 파생되도록 변경
    var animationTriggered by remember(state.isGiftBoxOpened) { mutableStateOf(state.isGiftBoxOpened) }
    var animationFinished by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val shakeAmount = remember { Animatable(0f) }
    val boxRotation = remember { Animatable(0f) }
    val SHAKE_THRESHOLD = 1000f

    LaunchedEffect(animationTriggered) {
        if (animationTriggered) {
            // 처음 열리는 것이 아닐 때는 딜레이 없이 바로 완료 처리
            if (!state.isGiftBoxOpened) {
                delay(1500)
                onGiftBoxOpened() // [수정] 애니메이션 완료 후, 열림 상태 업데이트 요청
            }
            animationFinished = true
        }
    }

    LaunchedEffect(shakeAmount.value) {
        if (!state.isGiftBoxOpened && shakeAmount.value >= SHAKE_THRESHOLD && !animationTriggered) {
            animationTriggered = true
        }
    }

    LaunchedEffect(state.shakeDetected) {
        if (state.shakeDetected && !animationTriggered) {
            coroutineScope.launch {
                shakeAmount.animateTo(SHAKE_THRESHOLD, tween(durationMillis = 500, easing = LinearEasing))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = albumTitle,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // 배경을 투명하게 설정
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            // [수정] 선물상자가 아직 열리지 않았을 때만 초기 UI(선물상자) 표시
            if (!animationTriggered) {
                val initialUiAlpha by animateFloatAsState(if (animationTriggered) 0f else 1f, tween(300), label = "")
                Column(modifier = Modifier.alpha(initialUiAlpha), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("우리의 순간들이 담긴 선물상자", color = Color(0xFFF57C00), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("선물 상자를 드래그하거나\n핸드폰을 흔들어 열어보세요!", color = Color.Yellow, fontSize = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(32.dp))
                    GiftBox(
                        rotation = boxRotation.value,
                        shakeProgress = shakeAmount.value / SHAKE_THRESHOLD,
                        modifier = Modifier.pointerInput(state.isGiftBoxOpened) {
                            if (!state.isGiftBoxOpened) { // 열린 후에는 제스처 막기
                                detectDragGestures(
                                    onDragEnd = {
                                        coroutineScope.launch { boxRotation.animateTo(0f, spring()) }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        coroutineScope.launch {
                                            val newRotation = (boxRotation.value + dragAmount.x * 0.1f).coerceIn(-45f, 45f)
                                            boxRotation.snapTo(newRotation)
                                            shakeAmount.snapTo(shakeAmount.value + abs(dragAmount.x))
                                        }
                                    }
                                )
                            }
                        }
                    )
                }
            }

            // [수정] 애니메이션이 시작되었거나, 이미 열려있는 상태일 때 카드 표시
            if (animationTriggered) {
                val density = LocalDensity.current
                val cardWidthPx = with(density) { 180.dp.toPx() }
                val cardHeightPx = with(density) { (180 / 0.7f).dp.toPx() }
                val spacingPx = with(density) { 16.dp.toPx() }

                emotionSections.take(4).forEachIndexed { index, section ->
                    val targetX = (if (index % 2 == 0) -1 else 1) * (cardWidthPx / 2 + spacingPx / 2)
                    val targetY = (if (index < 2) -1 else 1) * (cardHeightPx / 2 + spacingPx / 2)

                    AnimatingEmotionCard(
                        section = section,
                        index = index,
                        // [수정] 이미 열린 상태면 애니메이션 없이 바로 최종 위치에 있도록 startAnimation 제어
                        startAnimation = !state.isGiftBoxOpened,
                        targetX = targetX,
                        targetY = targetY,
                        modifier = Modifier
                            .size(width = 180.dp, height = (180 / 0.7f).dp),
                        onClick = { onEmotionCardClick(section.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GiftBox(
    rotation: Float,
    shakeProgress: Float,
    modifier: Modifier = Modifier
) {
    // [수정] 흔들기 진행도에 따라 스케일과 상하 움직임(wobble) 추가
    val scale = 1f + (shakeProgress * 0.1f)
    val wobble = sin(shakeProgress * 2 * PI.toFloat() * 5) * (shakeProgress * 10f)

    Image(
        painter = painterResource(id = R.drawable.giftbox),
        contentDescription = "선물 상자",
        modifier = modifier
            .size(150.dp)
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
                translationY = wobble // 상하 움직임 효과
            }
    )
}
@Composable
private fun AnimatingEmotionCard(
    section: EmotionSection,
    index: Int,
    startAnimation: Boolean,
    targetX: Float,
    targetY: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // [수정] startAnimation이 false이면(이미 열린 상태) 바로 최종값으로 설정
    val initialX = if (startAnimation) 0f else targetX
    val initialY = if (startAnimation) 0f else targetY
    val initialScale = if (startAnimation) 0f else 1f
    val initialRotation = if (startAnimation) Random.nextFloat() * 30f - 15f else 0f

    val translationX = remember { Animatable(initialX) }
    val translationY = remember { Animatable(initialY) }
    val scale = remember { Animatable(initialScale) }
    val rotation = remember { Animatable(initialRotation) }

    LaunchedEffect(startAnimation) {
        if (startAnimation) {
            delay(index * 200L + 200L)
            launch { translationX.animateTo(targetX, spring(stiffness = Spring.StiffnessLow)) }
            launch { translationY.animateTo(targetY, spring(stiffness = Spring.StiffnessLow)) }
            launch { rotation.animateTo(0f, spring(stiffness = Spring.StiffnessLow)) }
            launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            this.translationX = translationX.value
            this.translationY = translationY.value
            this.scaleX = scale.value
            this.scaleY = scale.value
            this.rotationZ = rotation.value
        }
    ) {
        EmotionCard(section = section, modifier = modifier, onClick = onClick)
    }
}
