package com.ssafy.keepick.feature.timeline

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import com.ssafy.keepick.R
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

// 데이터 클래스는 이전과 동일합니다.
data class TimelineMoment(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "새로운 모먼트",
    var description: String = "이곳에서의 추억을 기록해보세요.",
    val date: String,
    var photos: List<DisplayGroupAlbum?> = listOf(null, null, null)
)

private fun Int.toDrawableUri(context: Context): String {
    return Uri.parse("android.resource://${context.packageName}/$this").toString()
}

@Composable
fun TimelineAlbumRoute(
    timelineAlbumId: String,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- Mock Data ---
    val allPhotos = remember {
        (1..25).map {
            DisplayGroupAlbum(
                id = it, // ID를 String으로 변경
                thumbnailUrl = when {
                    it % 3 == 0 -> R.drawable.dummy_group.toDrawableUri(context)
                    it % 3 == 1 -> R.drawable.dummy_profile.toDrawableUri(context)
                    else -> R.drawable.dummy_album.toDrawableUri(context)
                },
                title = "추억 사진 $it", photoCount = 0, date = "2025.07.${(it % 30) + 1}", creator = "User ${it % 5}"
            )
        }
    }

    var moments by remember {
        mutableStateOf(
            listOf(
                TimelineMoment(
                    title = "제주도 여행 1일차",
                    description = "드디어 기다리던 제주도 여행! 공항에서 렌트카 빌려서 성산으로 이동",
                    date = "2025년 7월 1일",
                    photos = listOf(allPhotos[0], allPhotos[1], null)
                ),
                TimelineMoment(
                    title = "우도에서의 하루",
                    description = "전기자전거 빌려서 섬 한 바퀴! 땅콩 아이스크림은 필수 코스.",
                    date = "2025년 7월 2일",
                    photos = listOf(allPhotos[2], allPhotos[3], allPhotos[4])
                )
            )
        )
    }

    var representativeImage by remember { mutableStateOf(allPhotos[5]) }
    var albumTitle by remember { mutableStateOf("제주도 가족 여행") }
    var albumDescription by remember { mutableStateOf("가족끼리 갔었던 소중한 제주도 여행 추억들") }
    var availablePhotos by remember { mutableStateOf(allPhotos.drop(6)) }

    // --- UI 상태 관리 ---
    val listState = rememberLazyListState()
    var selectedMomentId by remember { mutableStateOf<String?>(null) }
    var isInEditMode by remember { mutableStateOf(false) }
    var momentBounds by remember { mutableStateOf<Map<String, Rect>>(emptyMap()) }
    var isAiSuggesting by remember { mutableStateOf<String?>(null) }

    // --- [수정] 드래그 앤 드롭 상태 관리 ---
    // 현재 드래그 중인 아이템만 관리하면 됨
    var draggedItem by remember { mutableStateOf<DisplayGroupAlbum?>(null) }


    TimelineAlbumScreen(
        listState = listState,
        moments = moments,
        selectedMomentId = selectedMomentId,
        momentBounds = momentBounds,
        representativeImage = representativeImage,
        albumTitle = albumTitle,
        albumDescription = albumDescription,
        // [수정] 드래그 중인 아이템은 그리드에 표시되지 않도록 필터링
        availablePhotos = availablePhotos.filter { it.id != draggedItem?.id },
        isAiSuggesting = isAiSuggesting,
        isInEditMode = isInEditMode,
        draggedItem = draggedItem,
        onNavigateBack = {
            if (selectedMomentId != null) {
                selectedMomentId = null
                isInEditMode = false
            } else {
                onNavigateBack()
            }
        },
        onToggleEditMode = { isInEditMode = !isInEditMode },
        onSaveClick = {
            isInEditMode = false
            selectedMomentId = null
        },
        onMomentCardClick = { momentId ->
            if (selectedMomentId == null) {
                selectedMomentId = momentId
            }
        },
        onRegisterMomentBounds = { momentId, bounds ->
            momentBounds = momentBounds + (momentId to bounds)
        },
        // --- [수정] Drag & Drop 콜백 구현 ---
        onItemDragStarted = { item ->
            draggedItem = item
        },
        onItemDropped = { photoId, targetKey, slotIndex ->
            val photoToDrop = allPhotos.find { it.id.toString() == photoId }

            if (photoToDrop != null) {
                Log.d("TimelineRoute", "ItemDropped: photoId=$photoId, targetKey=$targetKey, slotIndex=$slotIndex")
                if (targetKey == "cover") {
                    // 기존 대표 이미지는 availablePhotos로 다시 이동
                    representativeImage?.let { oldCover ->
                        availablePhotos = availablePhotos + oldCover
                    }
                    representativeImage = photoToDrop
                } else if (targetKey.startsWith("moment-")) {
                    val momentId = targetKey.substringAfter("moment-")
                    moments = moments.map { moment ->
                        if (moment.id == momentId) {
                            val newPhotos = moment.photos.toMutableList()
                            // 기존에 있던 사진은 availablePhotos로 다시 이동
                            newPhotos[slotIndex]?.let { oldPhoto ->
                                availablePhotos = availablePhotos + oldPhoto
                            }
                            newPhotos[slotIndex] = photoToDrop
                            moment.copy(photos = newPhotos)
                        } else moment
                    }
                }
                // availablePhotos에서 드롭된 사진 제거
                availablePhotos = availablePhotos.filter { it.id != photoToDrop.id }
            }
            // 드래그 상태 초기화
            draggedItem = null
        },
        onAlbumTitleChanged = { newTitle -> albumTitle = newTitle },
        onAlbumDescriptionChanged = { newDesc -> albumDescription = newDesc },
        onMomentTitleChanged = { momentId, newTitle ->
            moments = moments.map { if (it.id == momentId) it.copy(title = newTitle) else it }
        },
        onMomentDescriptionChanged = { momentId, newDesc ->
            moments = moments.map { if (it.id == momentId) it.copy(description = newDesc) else it }
        },
        onAiSuggestClick = { momentId ->
            coroutineScope.launch {
                isAiSuggesting = momentId
                delay(2000)
                moments = moments.map {
                    if (it.id == momentId) it.copy(title = "AI 추천: 우도의 추억", description = "AI 추천: 푸른 바다와 함께한 즐거운 하루가 느껴지네요.")
                    else it
                }
                isAiSuggesting = null
            }
        },
        onAddMoment = {
            val newMoment = TimelineMoment(date = "날짜 미정")
            moments = moments + newMoment
            coroutineScope.launch {
                listState.animateScrollToItem(moments.size)
            }
        },
        onDismissMoment = {
            selectedMomentId = null
            isInEditMode = false
        }
    )
}
