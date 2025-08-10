// feature/gallery/GalleryScreen.kt
package com.ssafy.keepick.feature.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.feature.gallery.components.CompactTagFilterView
import com.ssafy.keepick.feature.gallery.components.GalleryImageItem
import com.ssafy.keepick.feature.gallery.components.SelectionController
import com.ssafy.keepick.feature.gallery.model.GalleryImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    images: List<GalleryImage>,
    specialAiTags: List<String>,
    personTags: List<String>,
    generalTags: List<String>,
    selectedTags: List<String>, // <- 다중 선택 리스트만 남김
    isSelectionMode: Boolean,
    selectedImageIds: List<String>,
    isLoading: Boolean,
    isAnalyzing: Boolean,
    analysisProgress: Float,
    onToggleTagSelection: (String) -> Unit, // <- 다중 선택 핸들러만 남김
    onImageClick: (String) -> Unit,
    onImageLongClick: (String) -> Unit,
    onClearSelection: () -> Unit,
    onCreateAlbumClick: () -> Unit,
    onDeleteSelectedImages: () -> Unit,
    onFilterIconClick: () -> Unit,
    onUploadClick: () -> Unit
) {
    // 1. '선택됨' 필터의 활성화 상태를 GalleryScreen 내부에서 관리
    var isShowingOnlySelected by remember { mutableStateOf(false) }

    // 2. 선택 모드가 해제되면, '선택됨' 필터 상태도 자동으로 해제
    LaunchedEffect(isSelectionMode) {
        if (!isSelectionMode) {
            isShowingOnlySelected = false
        }
    }

    // 3. '선택됨' 필터 상태에 따라 그리드에 표시할 최종 이미지 목록 결정
    val displayedImages by remember(isShowingOnlySelected, images, selectedImageIds) {
        derivedStateOf {
            if (isSelectionMode && isShowingOnlySelected) {
                images.filter { it.id in selectedImageIds }
            } else {
                images
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (displayedImages.isEmpty() && selectedTags.isEmpty()) {
            EmptyGalleryContent(onUploadClick = onUploadClick, modifier = Modifier.fillMaxSize())
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                CompactTagFilterView(
                    modifier = Modifier.padding(vertical = 8.dp),
                    specialAiTags = specialAiTags,
                    personTags = personTags,
                    generalTags = generalTags,
                    selectedTags = selectedTags,
                    isSelectionMode = isSelectionMode,
                    isShowingOnlySelected = isShowingOnlySelected,
                    onTagClick = onToggleTagSelection,
                    onFilterIconClick = onFilterIconClick,
                    onShowOnlySelectedToggle = { isShowingOnlySelected = it }
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 4. 필터링된 최종 이미지 목록을 그리드에 전달
                    items(displayedImages, key = { it.id }) { image ->
                        GalleryImageItem(
                            image = image,
                            isSelected = image.id in selectedImageIds,
                            onClick = { onImageClick(image.id) },
                            onLongClick = { onImageLongClick(image.id) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            SelectionController(
                selectedCount = selectedImageIds.size,
                onCreateAlbumClick = onCreateAlbumClick,
                onDeleteClick = onDeleteSelectedImages
            )
        }

        // 우하단 AI 분석 상태 표시기
        AnimatedVisibility(
            visible = isAnalyzing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            AiAnalysisIndicator(progress = analysisProgress)
        }
    }
}

@Composable
private fun AiAnalysisIndicator(progress: Float) {
    Card(shape = CircleShape, elevation = CardDefaults.cardElevation(4.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(progress = progress, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI 동시 분석 중...(${(progress * 100).toInt()}%)", // 진행률 표시
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun EmptyGalleryContent(onUploadClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AddAPhoto,
            contentDescription = "사진 없음",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "갤러리가 비어있어요",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "첫 사진을 업로드 해보세요!",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onUploadClick) {
            Text("사진 업로드하기")
        }
    }
}
