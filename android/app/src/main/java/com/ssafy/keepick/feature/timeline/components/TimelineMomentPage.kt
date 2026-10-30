package com.ssafy.keepick.feature.timeline.components

import android.content.ClipDescription
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
import com.ssafy.keepick.feature.timeline.TimelineMoment

@Composable
fun TimelineMomentPage(
    moment: TimelineMoment,
    isInEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onSaveClick: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onAiSuggestClick: () -> Unit,
    isAiSuggesting: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Int,
    onPhotoIdDropped: (String, Int) -> Unit
) {
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = bottomContentPadding.dp)
        ) {
            // 상단 컨트롤 바
            MomentPageControls(
                isInEditMode = isInEditMode,
                onDismiss = onDismiss,
                onToggleEdit = onToggleEditMode,
                onSave = onSaveClick
            )

            // 컨텐츠
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    BasicTextField(
                        value = moment.title,
                        onValueChange = onTitleChanged,
                        enabled = isInEditMode,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                    )
                    Text(
                        text = moment.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BasicTextField(
                    value = moment.description,
                    onValueChange = onDescriptionChanged,
                    enabled = isInEditMode,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    decorationBox = { innerTextField ->
                        if (moment.description.isEmpty() && !isInEditMode) {
                            Text("이곳에서의 추억을 기록해보세요.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                        }
                        innerTextField()
                    }
                )

                // AI 추천 칩
                if (isInEditMode) {
                    AiSuggestionChip(
                        isLoading = isAiSuggesting,
                        onClick = onAiSuggestClick
                    )
                }

                // 사진 슬롯
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PhotoSlot(
                        photo = moment.photos.getOrNull(0),
                        isInEditMode = isInEditMode,
                        slotIndex = 0,
                        onPhotoIdDropped = { photoId -> onPhotoIdDropped(photoId, 0) },
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        (1..2).forEach { index ->
                            PhotoSlot(
                                photo = moment.photos.getOrNull(index),
                                isInEditMode = isInEditMode,
                                slotIndex = index,
                                onPhotoIdDropped = { photoId -> onPhotoIdDropped(photoId, index) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentPageControls(
    isInEditMode: Boolean,
    onDismiss: () -> Unit,
    onToggleEdit: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "닫기")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isInEditMode) {
                TextButton(onClick = onSave) {
                    Text("저장", fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onToggleEdit) {
                Icon(
                    imageVector = if (isInEditMode) Icons.Default.Done else Icons.Default.Edit,
                    contentDescription = if (isInEditMode) "수정 완료" else "수정하기"
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoSlot(
    photo: DisplayGroupAlbum?,
    isInEditMode: Boolean,
    slotIndex: Int,
    onPhotoIdDropped: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val highlightColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(16.dp)
    var isCurrentlyDragOver by remember { mutableStateOf(false) }

    val dropTarget = remember(isInEditMode) {
        object : DragAndDropTarget {
            private fun isPhotoDrag(event: DragAndDropEvent): Boolean =
                event.toAndroidDragEvent()?.clipData?.description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true

            override fun onEntered(event: DragAndDropEvent) {
                if (isInEditMode && isPhotoDrag(event)) {
                    isCurrentlyDragOver = true
                }
            }
            override fun onExited(event: DragAndDropEvent) {
                isCurrentlyDragOver = false
            }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                if (!isInEditMode || !isPhotoDrag(event)) {
                    isCurrentlyDragOver = false
                    return false
                }
                isCurrentlyDragOver = false
                val photoId = event.toAndroidDragEvent()?.clipData?.getItemAt(0)?.text?.toString()
                if (photoId != null) {
                    onPhotoIdDropped(photoId)
                    Log.d("PhotoSlot", "Dropped photo ID: $photoId into slot $slotIndex")
                    return true
                }
                return false
            }
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isCurrentlyDragOver) 3.dp else 1.dp,
                color = if (isCurrentlyDragOver) highlightColor else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dropTarget
            ),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = photo.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = "사진 추가",
                tint = if (isCurrentlyDragOver) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewTimelineMomentPage() {
    MaterialTheme {
        var isInEditMode by remember { mutableStateOf(false) }
        var momentTitle by remember { mutableStateOf("강릉 겨울 바다 여행 🌊") }
        var momentDescription by remember { mutableStateOf("파도가 너무 예뻤던 강릉 겨울 바다!") }
        var isAiSuggesting by remember { mutableStateOf(false) }

        TimelineMomentPage(
            moment = TimelineMoment(
                id = "moment_preview",
                date = "2025년 01월 20일",
                title = momentTitle,
                description = momentDescription,
                photos = listOf(
                    DisplayGroupAlbum(301, "https://picsum.photos/id/1015/400/300", "강릉 바다", 3, "2025-01-20", "Keepick"),
                    DisplayGroupAlbum(302, "https://picsum.photos/id/1043/400/300", "커피", 3, "2025-01-20", "Keepick"),
                    DisplayGroupAlbum(303, "https://picsum.photos/id/1080/400/300", "해산물", 3, "2025-01-20", "Keepick")
                )
            ),
            isInEditMode = isInEditMode,
            onToggleEditMode = { isInEditMode = !isInEditMode },
            onSaveClick = { isInEditMode = false },
            onTitleChanged = { momentTitle = it },
            onDescriptionChanged = { momentDescription = it },
            onAiSuggestClick = { isAiSuggesting = !isAiSuggesting },
            isAiSuggesting = isAiSuggesting,
            onDismiss = { },
            bottomContentPadding = if (isInEditMode) 300 else 0,
            onPhotoIdDropped = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewTimelineMomentPageNoPhotos() {
    MaterialTheme {
        var isInEditMode by remember { mutableStateOf(true) } // 수정 모드로 시작
        var momentTitle by remember { mutableStateOf("사진 없는 모먼트") }
        var momentDescription by remember { mutableStateOf("") } // 설명 비어있음

        TimelineMomentPage(
            moment = TimelineMoment(
                id = "moment_no_photo",
                date = "2025년 02월 01일",
                title = momentTitle,
                description = momentDescription,
                photos = emptyList() // 사진 없음
            ),
            isInEditMode = isInEditMode,
            onToggleEditMode = { isInEditMode = !isInEditMode },
            onSaveClick = { },
            onTitleChanged = { momentTitle = it },
            onDescriptionChanged = { momentDescription = it },
            onAiSuggestClick = { },
            isAiSuggesting = false,
            onDismiss = { },
            bottomContentPadding = if (isInEditMode) 300 else 0,
            onPhotoIdDropped = { _, _ -> }
        )
    }
}
