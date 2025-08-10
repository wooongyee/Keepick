// feature/gallery/GalleryScreen.kt
package com.ssafy.keepick.feature.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.feature.gallery.components.GalleryImageItem
import com.ssafy.keepick.feature.gallery.components.SelectionController
import com.ssafy.keepick.feature.gallery.model.GalleryImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    images: List<GalleryImage>,
    allTags: List<String>,
    selectedTag: String,
    isSelectionMode: Boolean,
    selectedImageIds: List<String>,
    isLoading: Boolean, // 로딩 상태 추가
    isAnalyzing: Boolean,
    analysisProgress: Float,
    analyzedImageCount: Int,
    totalImageCount: Int,
    onTagSelect: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onImageLongClick: (String) -> Unit,
    onClearSelection: () -> Unit,
    onCreateAlbumClick: () -> Unit,
    onDeleteSelectedImages: () -> Unit,
    onUploadClick: () -> Unit
) {
    // 내부 Scaffold를 제거하고 Box를 최상위로 사용
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            // 그룹 변경 시 전체 화면 로딩 인디케이터
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (images.isEmpty() && selectedTag == "전체") {
            EmptyGalleryContent(onUploadClick = onUploadClick, modifier = Modifier.fillMaxSize())
        } else {
            Column(modifier = Modifier.fillMaxSize()) { // Scaffold의 paddingValues를 Column에 적용
                // 해시태그 필터 영역
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTags) { tag ->
                        FilterChip(
                            selected = tag == selectedTag,
                            onClick = { onTagSelect(tag) },
                            label = { Text(tag) }
                        )
                    }
                }

                // 이미지 그리드 영역
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.weight(1f), // Column 내에서 남은 공간을 모두 차지하도록
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(images, key = { it.id }) { image ->
                        GalleryImageItem(
                            modifier = Modifier.animateItem(
                                fadeInSpec = null, fadeOutSpec = null, placementSpec = spring<IntOffset>(
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                )
                            ),
                            image = image,
                            isSelected = image.id in selectedImageIds,
                            onClick = { onImageClick(image.id) },
                            onLongClick = { onImageLongClick(image.id) }
                        )
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
                AiAnalysisIndicator(
                    progress = analysisProgress,
                    analyzedCount = analyzedImageCount,
                    totalCount = totalImageCount
                )
            }
        }
    }
}

@Composable
private fun AiAnalysisIndicator(progress: Float, analyzedCount: Int, totalCount: Int) {
    Card(shape = CircleShape, elevation = CardDefaults.cardElevation(4.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(progress = progress, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI 분석 중... ($analyzedCount/$totalCount)",
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
