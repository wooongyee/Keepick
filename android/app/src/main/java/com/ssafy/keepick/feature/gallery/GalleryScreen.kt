package com.ssafy.keepick.feature.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
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
    onToggleImageSelection: (String) -> Unit, // 이름 변경: onImageClick -> onToggleImageSelection
    onNavigateToImageDetail: (sharedKey : String, imageId: String, clusterId: String?) -> Unit,
    onNavigateToSimilarPhotos: () -> Unit,
    onImageLongClick: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeselectAll: () -> Unit,
    onCreateTierAlbumClick: () -> Unit,
    onCreateTimelineAlbumClick: () -> Unit,
    onDeleteSelectedImages: () -> Unit,
    onFilterIconClick: () -> Unit,
    onUploadClick: () -> Unit
) {
    // 1. '선택됨' 필터의 활성화 상태를 GalleryScreen 내부에서 관리
    var isShowingOnlySelected by remember { mutableStateOf(false) }
    // --- 핵심 수정 사항: 컨트롤러 표시 여부 상태 추가 ---
    var isControllerVisible by remember { mutableStateOf(true) }

    // 선택 모드가 변경될 때마다 컨트롤러를 항상 보이도록 초기화
    LaunchedEffect(isSelectionMode) {
        isControllerVisible = true
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
                    onSimilarPhotosClick = onNavigateToSimilarPhotos,
                    onShowOnlySelectedToggle = { isShowingOnlySelected = it },
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
                            keyPrefix = "gallery",
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            image = image,
                            isSelected = image.id in selectedImageIds,
                            onClick = {
                                if (isSelectionMode) {
                                    // 선택 모드일 때는 ViewModel의 선택 토글 함수 호출
                                    onToggleImageSelection(image.id)
                                } else {
                                    // 선택 모드가 아닐 때는 내비게이션 콜백 호출
                                    onNavigateToImageDetail("gallery_${image.id}", image.id, null)
                                }
                            },
                            onLongClick = { onImageLongClick(image.id) }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isSelectionMode && isControllerVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) {
            SelectionController(
                selectedCount = selectedImageIds.size,
                totalCount = displayedImages.size,
                onDeselectAll = onDeselectAll,
                onCreateTierAlbumClick = onCreateTierAlbumClick,
                onCreateTimelineAlbumClick = onCreateTimelineAlbumClick,
                onDeleteClick = onDeleteSelectedImages,
                onDismissRequest = { isControllerVisible = false } // 숨기기 버튼 클릭 시 상태 변경
            )
        }

        // 2. 컨트롤러가 숨겨졌을 때 나타나는 '보이기' 버튼
        AnimatedVisibility(
            visible = isSelectionMode && !isControllerVisible,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { isControllerVisible = true }, // 클릭 시 컨트롤러 다시 표시
                shape = CircleShape,
                modifier = Modifier.size(48.dp) // 작은 사이즈로 변경
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "컨트롤러 보이기")
            }
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
