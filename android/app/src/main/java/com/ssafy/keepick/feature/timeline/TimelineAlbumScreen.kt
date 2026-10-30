package com.ssafy.keepick.feature.timeline

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
import com.ssafy.keepick.feature.timeline.components.DraggablePhotoGrid
import com.ssafy.keepick.feature.timeline.components.TimelineAlbumTopAppBar
import com.ssafy.keepick.feature.timeline.components.TimelineCover
import com.ssafy.keepick.feature.timeline.components.TimelineMomentCard
import com.ssafy.keepick.feature.timeline.components.TimelineMomentItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimelineAlbumScreen(
    listState: LazyListState,
    moments: List<TimelineMoment>,
    selectedMomentId: String?,
    momentBounds: Map<String, Rect>,
    representativeImage: DisplayGroupAlbum?,
    albumTitle: String,
    albumDescription: String,
    availablePhotos: List<DisplayGroupAlbum>,
    isAiSuggesting: String?,
    isInEditMode: Boolean,
    onItemDropped: (photoId: String, targetKey: String, slotIndex: Int) -> Unit,
    onItemDragStarted: (DisplayGroupAlbum) -> Unit,
    draggedItem: DisplayGroupAlbum?,
    onNavigateBack: () -> Unit,
    onToggleEditMode: () -> Unit,
    onSaveClick: () -> Unit,
    onMomentCardClick: (String) -> Unit,
    onRegisterMomentBounds: (String, Rect) -> Unit,
    onAlbumTitleChanged: (String) -> Unit,
    onAlbumDescriptionChanged: (String) -> Unit,
    onMomentTitleChanged: (String, String) -> Unit,
    onMomentDescriptionChanged: (String, String) -> Unit,
    onAiSuggestClick: (String) -> Unit,
    onAddMoment: () -> Unit,
    onDismissMoment: () -> Unit
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        val wasLight = insetsController.isAppearanceLightStatusBars
        insetsController.isAppearanceLightStatusBars = false
        onDispose {
            insetsController.isAppearanceLightStatusBars = wasLight
        }
    }

    val isMomentExpanded = selectedMomentId != null
    val isDataEmpty = moments.isEmpty()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = moments.indexOfFirst { it.id == selectedMomentId }.coerceAtLeast(0),
        pageCount = { moments.size }
    )

    LaunchedEffect(selectedMomentId) {
        if (selectedMomentId != null) {
            val targetPage = moments.indexOfFirst { it.id == selectedMomentId }
            if (targetPage != -1 && pagerState.currentPage != targetPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var pagerAlpha by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(isMomentExpanded) {
        if (!isMomentExpanded) {
            launch { Animatable(dragOffsetY).animateTo(0f, animationSpec = tween(300)) { dragOffsetY = value } }
            launch { Animatable(pagerAlpha).animateTo(1f, animationSpec = tween(300)) { pagerAlpha = value } }
        }
    }

    BackHandler(enabled = isMomentExpanded) {
        onDismissMoment()
    }

    val bottomContentPadding by animateIntAsState(
        targetValue = if (isInEditMode && isMomentExpanded) 300 else 0,
        label = "bottomContentPadding",
        animationSpec = tween(durationMillis = 300)
    )
    val listAlpha by animateFloatAsState(
        targetValue = if (isMomentExpanded) 1f - pagerAlpha else 1f,
        label = "listAlpha"
    )

    val listBottomPadding by animateDpAsState(
        targetValue = if (isInEditMode && !isMomentExpanded) 300.dp else 0.dp,
        label = "listBottomPadding"
    )


    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = listAlpha },
            contentPadding = PaddingValues(bottom = listBottomPadding)
        ) {
            if (!isDataEmpty) {
                item {
                    TimelineCover(
                        image = representativeImage,
                        title = albumTitle,
                        description = albumDescription,
                        isInEditMode = isInEditMode,
                        onTitleChanged = onAlbumTitleChanged,
                        onDescriptionChanged = onAlbumDescriptionChanged,
                        onPhotoIdDropped = { photoId ->
                            onItemDropped(photoId, "cover", 0)
                        }
                    )
                }

                itemsIndexed(moments, key = { _, moment -> moment.id }) { index, moment ->
                    // 각 모먼트 아이템을 위한 전체 너비 컨테이너
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        // 레이어 1: 블러 처리된 배경 이미지
                        val firstImage = moment.photos.firstOrNull()
                        if (firstImage != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(firstImage.thumbnailUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Blurred background",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .matchParentSize() // 부모 Box 크기에 맞춤
                                    .blur(25.dp) // 강한 블러 효과
                                    .background(Color.Black.copy(alpha = 0.2f)) // 이미지 어둡게 처리
                            )
                        }else{
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .blur(radius = 25.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF80CBC4).copy(alpha = 0.4f),
                                                    Color(0xFF001F3F).copy(alpha = 0.4f)
                                                )
                                            )
                                        )
                                )
                            }
                        }

                        TimelineMomentCard(
                            moment = moment,
                            onClick = { onMomentCardClick(moment.id) },
                            onRegisterBounds = { bounds -> onRegisterMomentBounds(moment.id, bounds) },
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                .graphicsLayer { rotationZ = if (index % 2 == 0) -1.5f else 1.5f }
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAddMoment),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .blur(radius = 25.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF80CBC4).copy(alpha = 0.4f),
                                                Color(0xFF001F3F).copy(alpha = 0.4f)
                                            )
                                        )
                                    )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .padding(vertical = 24.dp)
                        ) {
                            AddMomentButton()
                        }
                    }
                }
            }

            if (isDataEmpty && !isMomentExpanded) {
                item {
                    EmptyState(
                        modifier = Modifier
                            .fillParentMaxHeight(0.8f)
                            .fillParentMaxWidth(),
                        onAddMoment = onAddMoment
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        ) {
            TimelineAlbumTopAppBar(
                isInEditMode = isInEditMode,
                isMomentExpanded = isMomentExpanded,
                onNavigateBack = onNavigateBack,
                onEditClick = onToggleEditMode,
                onAddMomentClick = onAddMoment,
                onSaveClick = onSaveClick
            )
        }

        if (isMomentExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (pagerAlpha * 0.5f).coerceIn(0f, 0.5f)))
                    .graphicsLayer { alpha = if (isMomentExpanded) 1f else 0f }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isInEditMode) {
                        if (!isInEditMode) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    val screenHeight = size.height.toFloat()
                                    if (dragOffsetY > screenHeight / 4) {
                                        onDismissMoment()
                                    } else {
                                        coroutineScope.launch {
                                            Animatable(dragOffsetY).animateTo(0f, tween(300)) { dragOffsetY = value }
                                            Animatable(pagerAlpha).animateTo(1f, tween(300)) { pagerAlpha = value }
                                        }
                                    }
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount
                                    if (dragOffsetY < 0) dragOffsetY = 0f
                                    val screenHeight = size.height.toFloat()
                                    pagerAlpha = (1f - (dragOffsetY / (screenHeight / 1.5f))).coerceIn(0f, 1f)
                                }
                            )
                        }
                    }
                    .graphicsLayer {
                        translationY = dragOffsetY
                        alpha = pagerAlpha
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val moment = moments.getOrNull(pageIndex)
                    if (moment != null) {
                        TimelineMomentItem(
                            moment = moment,
                            isExpanded = true,
                            initialBounds = momentBounds[moment.id] ?: Rect.Zero,
                            onRegisterBounds = {},
                            onClick = {},
                            onDismiss = onDismissMoment,
                            isInEditMode = isInEditMode,
                            onToggleEditMode = onToggleEditMode,
                            onSaveClick = onSaveClick,
                            isAiSuggesting = isAiSuggesting == moment.id,
                            onAiSuggestClick = { onAiSuggestClick(moment.id) },
                            onTitleChanged = { newTitle -> onMomentTitleChanged(moment.id, newTitle) },
                            onDescriptionChanged = { newDesc -> onMomentDescriptionChanged(moment.id, newDesc) },
                            bottomContentPadding = bottomContentPadding,
                            onPhotoIdDropped = { photoId, slotIndex ->
                                onItemDropped(photoId, "moment-${moment.id}", slotIndex)
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isInEditMode,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(top = 16.dp)
            ) {
                DraggablePhotoGrid(
                    photos = availablePhotos.filter { it.id != draggedItem?.id },
                    title = "사용 가능한 사진",
                    onDragStart = { photo -> onItemDragStarted(photo) }
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    onAddMoment: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = "빈 앨범 아이콘",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "첫 모먼트를 추가해보세요",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "이 앨범의 추억을 기록하고 사진을 추가하여 타임라인을 채워보세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
fun AddMomentButton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AddCircle,
            contentDescription = "모먼트 추가",
            tint = Color.White // 흰색으로 변경
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "새로운 모먼트 추가하기",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White // 흰색으로 변경
        )
    }
}


@Preview(showSystemUi = true)
@Composable
fun PreviewTimelineAlbumScreenContent() {
    MaterialTheme {
        val sampleMoments = remember {
            mutableStateListOf(
                TimelineMoment(
                    id = "moment1",
                    date = "2025년 08월 01일",
                    title = "제주도 첫날! 해변 산책 🏖️",
                    description = "성산일출봉 근처 해변을 산책하며 아름다운 노을을 감상했어요.",
                    photos = listOf(
                        DisplayGroupAlbum(1, "https://picsum.photos/id/10/400/600", "노을", 10, "2025-08-01", "UserA")
                    )
                ),
                TimelineMoment(
                    id = "moment2",
                    date = "2025년 08월 02일",
                    title = "한라산 등반 성공! ⛰️",
                    description = "한라산 백록담 정상에서 바라본 풍경은 정말 최고였어요!",
                    photos = listOf(
                        DisplayGroupAlbum(2, "https://picsum.photos/id/100/400/600", "백록담", 25, "2025-08-02", "UserB"),
                        DisplayGroupAlbum(3, "https://picsum.photos/id/101/400/600", "정상", 25, "2025-08-02", "UserB")
                    )
                )
            )
        }

        val representativeImage = remember {
            DisplayGroupAlbum(100, "https://picsum.photos/id/90/800/600", "제주도 여행 대표", 50, "2025-08-01", "Admin")
        }

        val availablePhotos = remember {
            mutableStateListOf(
                DisplayGroupAlbum(201, "https://picsum.photos/id/1/100/100", "사용 가능 사진 1", 1, "2025-01-01", "UserC"),
                DisplayGroupAlbum(202, "https://picsum.photos/id/2/100/100", "사용 가능 사진 2", 1, "2025-01-02", "UserD"),
                DisplayGroupAlbum(203, "https://picsum.photos/id/3/100/100", "사용 가능 사진 3", 1, "2025-01-03", "UserE")
            )
        }

        var selectedMomentId by remember { mutableStateOf<String?>(null) }
        var isInEditMode by remember { mutableStateOf(false) }

        TimelineAlbumScreen(
            listState = rememberLazyListState(),
            moments = sampleMoments,
            selectedMomentId = selectedMomentId,
            momentBounds = remember { mutableStateMapOf() },
            representativeImage = representativeImage,
            albumTitle = "2025 제주도 여행기",
            albumDescription = "잊지 못할 제주에서의 아름다운 추억들",
            availablePhotos = availablePhotos,
            isAiSuggesting = null,
            isInEditMode = isInEditMode,
            onItemDropped = { _, _, _ -> },
            onItemDragStarted = { },
            draggedItem = null,
            onNavigateBack = { },
            onToggleEditMode = { isInEditMode = !isInEditMode },
            onSaveClick = { },
            onMomentCardClick = { id -> selectedMomentId = id },
            onRegisterMomentBounds = { _, _ -> },
            onAlbumTitleChanged = { },
            onAlbumDescriptionChanged = { },
            onMomentTitleChanged = { _, _ -> },
            onMomentDescriptionChanged = { _, _ -> },
            onAiSuggestClick = { },
            onAddMoment = { },
            onDismissMoment = { selectedMomentId = null }
        )
    }
}


@Preview(showSystemUi = true)
@Composable
fun PreviewTimelineAlbumScreenEmpty() {
    MaterialTheme {
        TimelineAlbumScreen(
            listState = rememberLazyListState(),
            moments = emptyList(), // 빈 목록
            selectedMomentId = null,
            momentBounds = remember { mutableStateMapOf() },
            representativeImage = null,
            albumTitle = "새로운 앨범",
            albumDescription = "아직 모먼트가 없어요. 첫 모먼트를 추가해보세요!",
            availablePhotos = remember { mutableStateListOf() },
            isAiSuggesting = null,
            isInEditMode = false,
            onItemDropped = { _, _, _ -> },
            onItemDragStarted = {},
            draggedItem = null,
            onNavigateBack = {},
            onToggleEditMode = {},
            onSaveClick = {},
            onMomentCardClick = {},
            onRegisterMomentBounds = { _, _ -> },
            onAlbumTitleChanged = {},
            onAlbumDescriptionChanged = {},
            onMomentTitleChanged = { _, _ -> },
            onMomentDescriptionChanged = { _, _ -> },
            onAiSuggestClick = {},
            onAddMoment = {},
            onDismissMoment = {}
        )
    }
}
