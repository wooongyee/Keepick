package com.ssafy.keepick.feature.timeline.components

import android.content.ClipDescription
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ssafy.keepick.feature.group.DisplayGroupAlbum

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineCover(
    image: DisplayGroupAlbum?,
    title: String,
    description: String,
    isInEditMode: Boolean,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onPhotoIdDropped: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCurrentlyDragOver by remember { mutableStateOf(false) }

    val dropTarget = remember {
        object : DragAndDropTarget {
            private fun isPhotoDrag(event: DragAndDropEvent): Boolean =
                event.toAndroidDragEvent()?.clipData?.description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true

            override fun onEntered(event: DragAndDropEvent) {
                if (isPhotoDrag(event)) {
                    isCurrentlyDragOver = true
                    Log.d("TimelineCover", "Drag entered")
                }
            }

            override fun onExited(event: DragAndDropEvent) {
                isCurrentlyDragOver = false
                Log.d("TimelineCover", "Drag exited")
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                if (!isPhotoDrag(event)) {
                    isCurrentlyDragOver = false
                    return false
                }
                isCurrentlyDragOver = false
                val photoId = event.toAndroidDragEvent()!!.clipData.getItemAt(0).text.toString()
                onPhotoIdDropped(photoId)
                Log.d("TimelineCover", "Dropped photo ID: $photoId")
                return true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(480.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant) // 기본 배경색 지정
            .dragAndDropTarget(
                shouldStartDragAndDrop = { isInEditMode },
                target = dropTarget
            )
            .border(
                width = if (isCurrentlyDragOver) 3.dp else 0.dp,
                color = if (isCurrentlyDragOver) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(2.dp)
            )
    ) {
        // 배경 이미지 (존재할 경우)
        if (image != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "대표 이미지",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 텍스트 가독성을 위한 그라데이션 오버레이
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        ),
                        startY = 0f,
//                        endY = 800f // 컴포넌트 높이보다 큰 값으로 설정하여 자연스러운 그라데이션
                    )
                )
        )

        // --- 수정된 부분: 콘텐츠 위치 조정을 위한 부모 Column ---
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 30%의 공간을 차지하는 스페이서
            Spacer(modifier = Modifier.fillMaxHeight(0.3f))

            // 기존 콘텐츠 Column (상단 패딩 제거)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (isInEditMode) {
                    BasicTextField(
                        value = title,
                        onValueChange = onTitleChanged,
                        textStyle = textStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(text = title, style = textStyle)
                }

                Spacer(Modifier.height(8.dp))

                val descriptionStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                if (isInEditMode) {
                    BasicTextField(
                        value = description,
                        onValueChange = onDescriptionChanged,
                        textStyle = descriptionStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(text = description, style = descriptionStyle)
                }
            }
        }
        // --- 수정 끝 ---

        // 이미지가 없고, 수정 모드일 때 안내 텍스트 표시
        if (image == null && isInEditMode) {
            Text(
                text = "이곳에 사진을 드래그하여\n대표 이미지로 설정하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// ------ Preview Composable ------

@Preview(showBackground = true, name = "View Mode (With Image)")
@Composable
private fun TimelineCoverPreview_ViewMode_WithImage() {
    MaterialTheme {
        TimelineCover(
            image = DisplayGroupAlbum(1, "https://picsum.photos/id/43/800/600", "여행 대표 사진", 20, "2025-08-07", "Keepick"),
            title = "꿈같았던 스위스 여행",
            description = "알프스의 아름다운 풍경과 함께한 잊지 못할 시간",
            isInEditMode = false,
            onTitleChanged = {},
            onDescriptionChanged = {},
            onPhotoIdDropped = {}
        )
    }
}

@Preview(showBackground = true, name = "Edit Mode (With Image)")
@Composable
private fun TimelineCoverPreview_EditMode_WithImage() {
    MaterialTheme {
        var title by remember { mutableStateOf("꿈같았던 스위스 여행") }
        var description by remember { mutableStateOf("알프스의 아름다운 풍경과 함께한 잊지 못할 시간") }

        TimelineCover(
            image = DisplayGroupAlbum(1, "https://picsum.photos/id/43/800/600", "여행 대표 사진", 20, "2025-08-07", "Keepick"),
            title = title,
            description = description,
            isInEditMode = true,
            onTitleChanged = { title = it },
            onDescriptionChanged = { description = it },
            onPhotoIdDropped = {}
        )
    }
}

@Preview(showBackground = true, name = "Edit Mode (No Image)")
@Composable
private fun TimelineCoverPreview_EditMode_NoImage() {
    MaterialTheme {
        var title by remember { mutableStateOf("새 앨범 제목") }
        var description by remember { mutableStateOf("앨범에 대한 설명을 입력하세요") }

        TimelineCover(
            image = null,
            title = title,
            description = description,
            isInEditMode = true,
            onTitleChanged = { title = it },
            onDescriptionChanged = { description = it },
            onPhotoIdDropped = {}
        )
    }
}

@Preview(showBackground = true, name = "View Mode (No Image)")
@Composable
private fun TimelineCoverPreview_ViewMode_NoImage() {
    MaterialTheme {
        TimelineCover(
            image = null,
            title = "새 앨범 제목",
            description = "앨범에 대한 설명을 입력하세요",
            isInEditMode = false,
            onTitleChanged = {},
            onDescriptionChanged = {},
            onPhotoIdDropped = {}
        )
    }
}
