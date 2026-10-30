// feature/highlight/emotion/EmotionDetailScreen.kt

package com.ssafy.keepick.feature.highlight.emotion

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ssafy.keepick.R
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
import com.ssafy.keepick.feature.highlight.album.EmotionCard
import com.ssafy.keepick.feature.highlight.album.EmotionSection
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class StickerState(
    val x: Float, val y: Float, val rotation: Float, val scale: Float, val drawableRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionDetailScreen(
    albumTitle: String,
    section: EmotionSection,
    onNavigateBack: () -> Unit
) {
    var currentIndex by remember(section) { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val offsetY = remember(section) { Animatable(0f) }
    var isFirstCardFlipped by remember(section) { mutableStateOf(false) }
    var isResetting by remember(section) { mutableStateOf(false) }

    val photoCount = section.photos.size
    val cardRotations = remember(section) {
        List(photoCount) { if (it == 0) 0f else Random.nextFloat() * 10f - 5f }
    }

    val cardDropOffsetYs = remember(section) { List(photoCount) { Animatable(0f) } }
    val cardDropRotations = remember(section) { List(photoCount) { Animatable(0f) } }

    LaunchedEffect(isResetting) {
        if (isResetting) {
            currentIndex = 0
            offsetY.snapTo(0f)
            isFirstCardFlipped = true

            cardDropOffsetYs.indices.reversed().map { index ->
                async {
                    delay((photoCount - 1 - index) * 150L)
                    launch {
                        cardDropOffsetYs[index].animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 300f))
                    }
                    launch {
                        cardDropRotations[index].animateTo(0f, spring(stiffness = 400f))
                    }
                }
            }.awaitAll()

            isResetting = false
        }
    }


    val context = LocalContext.current
    LaunchedEffect(section.photos) {
        section.photos.take(5).forEach { photo ->
            val request = ImageRequest.Builder(context).data(photo.thumbnailUrl).memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).build()
            context.imageLoader.enqueue(request)
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { /* 비워둠 */ },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            BackgroundStickers()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isFirstCardFlipped, isResetting) {
                        if (isFirstCardFlipped && !isResetting) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    coroutineScope.launch {
                                        if (offsetY.value < -300) {
                                            offsetY.animateTo(-2000f, spring(stiffness = 800f, dampingRatio = 0.8f))
                                            if (currentIndex >= photoCount - 1) {
                                                val initialY = -2500f
                                                cardDropOffsetYs.forEachIndexed { index, anim ->
                                                    anim.snapTo(initialY)
                                                    cardDropRotations[index].snapTo(Random.nextFloat() * 90f - 45f)
                                                }
                                                isResetting = true
                                            } else {
                                                currentIndex++
                                                offsetY.snapTo(0f)
                                            }
                                        } else {
                                            offsetY.animateTo(0f, spring(stiffness = 400f))
                                        }
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    coroutineScope.launch {
                                        if (offsetY.value + dragAmount < 100) {
                                            offsetY.snapTo(offsetY.value + dragAmount)
                                        }
                                    }
                                }
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val visiblePhotos = section.photos
                val density = LocalDensity.current

                (visiblePhotos.size - 1 downTo 0).forEach { index ->
                    if (index >= currentIndex || isResetting) {
                        val photo = visiblePhotos[index]
                        val isCurrentCard = index == currentIndex
                        val cardOffsetY = if (isCurrentCard && !isResetting) offsetY.value else 0f

                        val targetTranslationY = with(density) { ((index - currentIndex) * 50.dp).toPx() }
                        val animatedTranslationY by animateFloatAsState(targetTranslationY, spring(stiffness = 500f), label = "CardStackTranslation")

                        val targetScale = (1f - (index - currentIndex) * 0.08f).coerceAtLeast(0.5f)
                        val animatedScale by animateFloatAsState(if (isCurrentCard && !isResetting) targetScale + (cardOffsetY / 5000f) else targetScale, spring(stiffness = 500f), label = "CardStackScale")

                        val animatedRotationZ by animateFloatAsState(if (isCurrentCard && !isResetting) (offsetY.value / 15f).coerceIn(-45f, 45f) else cardRotations[index], spring(stiffness = 400f), label = "CardRotation")

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = animatedTranslationY + cardOffsetY + cardDropOffsetYs[index].value
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                    rotationZ = animatedRotationZ + cardDropRotations[index].value
                                }
                                .padding(horizontal = 32.dp)
                        ) {
                            if (index == 0) {
                                FlippableCard(
                                    // [수정] isFirstCardFlipped 단일 상태로만 제어
                                    isFlipped = isFirstCardFlipped,
                                    front = {
                                        EmotionCard(section = section) {
                                            if (currentIndex == 0 && !isResetting) {
                                                isFirstCardFlipped = true
                                            }
                                        }
                                    },
                                    back = {
                                        PhotoCard(photo = photo) {
                                            if (currentIndex == 0 && !isResetting) {
                                                isFirstCardFlipped = false
                                            }
                                        }
                                    }
                                )
                            } else {
                                PhotoCard(photo = photo)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCard(photo: DisplayGroupAlbum, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val imageRequest = remember(photo.thumbnailUrl) {
        ImageRequest.Builder(context).data(photo.thumbnailUrl).memoryCachePolicy(CachePolicy.ENABLED).diskCachePolicy(CachePolicy.ENABLED).crossfade(true).listener(onStart = { isLoading = true }, onSuccess = { _, _ -> isLoading = false }, onError = { _, _ -> isLoading = false }).build()
    }

    Box(
        modifier = modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(Color.Gray.copy(alpha = 0.5f))
    ) {
        AsyncImage(model = imageRequest, contentDescription = photo.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().sparkleEffect())
        }
    }
}

@Composable
fun FlippableCard(isFlipped: Boolean, modifier: Modifier = Modifier, front: @Composable () -> Unit, back: @Composable () -> Unit) {
    val rotation by animateFloatAsState(if (isFlipped) 180f else 0f, spring(dampingRatio = 0.8f, stiffness = 150f), label = "FlipAnimation")

    Box(
        modifier = modifier.graphicsLayer {
            rotationY = rotation
            cameraDistance = 12 * density
        }
    ) {
        if (rotation <= 90f) {
            front()
        } else {
            Box(Modifier.graphicsLayer { rotationY = 180f }) {
                back()
            }
        }
    }
}

fun Modifier.sparkleEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "SparkleTransition")
    val sparkleX by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), "SparkleX")
    val sparkleY by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1700, delayMillis = 200, easing = LinearEasing)), "SparkleY")
    val sparkleAlpha by transition.animateFloat(0.5f, 1f, infiniteRepeatable(tween(800), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse), "SparkleAlpha")

    drawWithContent {
        drawContent()
        val sparkleBrush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.8f * sparkleAlpha), Color.White.copy(alpha = 0.5f * sparkleAlpha), Color.Transparent),
            center = Offset(size.width * sparkleX, size.height * sparkleY),
            radius = size.minDimension / 4
        )
        drawRect(brush = sparkleBrush)
    }
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun BackgroundStickers() {
    val stickerResources = remember {
        listOf(R.drawable.sticker_1, R.drawable.sticker_2, R.drawable.sticker_3, R.drawable.sticker_4, R.drawable.sticker_5, R.drawable.sticker_6, R.drawable.sticker_7, R.drawable.sticker_8, R.drawable.sticker_9, R.drawable.sticker_10).shuffled()
    }
    val stickerStates = remember {
        val states = mutableListOf<StickerState>()
        val usedStickers = stickerResources.iterator()
        val marginHorizontal = 0.15f
        val marginVertical = 0.20f
        states.add(StickerState(x = Random.nextFloat() * 0.3f + 0.1f, y = Random.nextFloat() * marginVertical, rotation = Random.nextFloat() * 30f - 15f, scale = Random.nextFloat() * 0.4f + 0.8f, drawableRes = usedStickers.next()))
        states.add(StickerState(x = 1f - (Random.nextFloat() * 0.3f + 0.1f), y = Random.nextFloat() * marginVertical, rotation = Random.nextFloat() * 30f - 15f, scale = Random.nextFloat() * 0.4f + 0.8f, drawableRes = usedStickers.next()))
        states.add(StickerState(x = Random.nextFloat() * marginHorizontal, y = Random.nextFloat() * 0.4f + 0.3f, rotation = Random.nextFloat() * 30f - 15f, scale = Random.nextFloat() * 0.4f + 0.8f, drawableRes = usedStickers.next()))
        states.add(StickerState(x = 1f - (Random.nextFloat() * marginHorizontal), y = Random.nextFloat() * 0.4f + 0.3f, rotation = Random.nextFloat() * 30f - 15f, scale = Random.nextFloat() * 0.4f + 0.8f, drawableRes = usedStickers.next()))
        states.add(StickerState(x = Random.nextFloat() * 0.6f + 0.2f, y = 1f - (Random.nextFloat() * marginVertical), rotation = Random.nextFloat() * 30f - 15f, scale = Random.nextFloat() * 0.4f + 0.8f, drawableRes = usedStickers.next()))
        val randomCount = Random.nextInt(1, 4)
        for (i in 0 until randomCount) {
            if (!usedStickers.hasNext()) break
            val area = Random.nextInt(4)
            val x: Float
            val y: Float
            when (area) {
                0 -> { x = Random.nextFloat(); y = Random.nextFloat() * marginVertical }
                1 -> { x = Random.nextFloat(); y = 1f - Random.nextFloat() * marginVertical }
                2 -> { x = Random.nextFloat() * marginHorizontal; y = Random.nextFloat() * (1f - 2 * marginVertical) + marginVertical }
                else -> { x = 1f - Random.nextFloat() * marginHorizontal; y = Random.nextFloat() * (1f - 2 * marginVertical) + marginVertical }
            }
            states.add(StickerState(x = x, y = y, rotation = Random.nextFloat() * 45f - 22.5f, scale = Random.nextFloat() * 0.4f + 0.8f, drawableRes = usedStickers.next()))
        }
        states
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        stickerStates.forEach { state ->
            Icon(painter = painterResource(id = state.drawableRes), contentDescription = null, modifier = Modifier.align(Alignment.TopStart).offset(x = maxWidth * state.x, y = maxHeight * state.y).graphicsLayer { translationX = -size.width / 2; translationY = -size.height / 2; rotationZ = state.rotation; scaleX = state.scale; scaleY = state.scale }.size(Random.nextInt(80, 151).dp), tint = Color.Unspecified)
        }
    }
}
