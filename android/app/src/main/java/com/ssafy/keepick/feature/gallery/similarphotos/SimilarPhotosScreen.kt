package com.ssafy.keepick.feature.gallery.similarphotos

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssafy.keepick.feature.gallery.components.GalleryImageItem
import com.ssafy.keepick.feature.gallery.similarphotos.components.SimilarPhotosSelectionController

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SimilarPhotosScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    state: SimilarPhotosState,
    onEvent: (SimilarPhotosEvent) -> Unit,
    onNavigateToImageDetail: (sharedKey: String, imageId: String, clusterId: String?) -> Unit,
) {
    var isControllerVisible by remember { mutableStateOf(true) }
    val isSelectionMode = state.selectedPhotoIds.isNotEmpty()

    LaunchedEffect(isSelectionMode) {
        if (isSelectionMode) {
            isControllerVisible = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isAnalyzing -> AnalysisInProgressScreen(progress = state.progress)
            state.photoClusters == null -> StartAnalysisScreen { onEvent(SimilarPhotosEvent.OnRequestAnalysis) }
            state.photoClusters.isEmpty() -> InfoMessage(
                text = "분석 결과, 유사한 사진이 없습니다.",
                onRequestAnalysis = { onEvent(SimilarPhotosEvent.OnRequestAnalysis) }
            )

            else ->
                ClusteredPhotosList(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    state = state,
                    onEvent = onEvent,
                    onNavigateToImageDetail = onNavigateToImageDetail
                )
        }

        AnimatedVisibility(
            visible = isSelectionMode && isControllerVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            // `SimilarPhotosSelectionController`에 `onDismissRequest` 콜백을 전달해야 함 (컴포넌트 수정 필요)
            SimilarPhotosSelectionController(
                selectedCount = state.selectedPhotoIds.size,
                onDeselectAll = { onEvent(SimilarPhotosEvent.OnClearSelection) },
                onDeleteClick = { onEvent(SimilarPhotosEvent.OnDeleteSelectedPhotos) },
                onDismissRequest = { isControllerVisible = false } // 숨기기 버튼 클릭 시 상태 변경
            )
        }

        // 4. 컨트롤러가 숨겨졌을 때 나타나는 '보이기' 버튼
        AnimatedVisibility(
            visible = isSelectionMode && !isControllerVisible,
            enter = scaleIn(),
            exit = scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { isControllerVisible = true }, // 클릭 시 컨트롤러 다시 표시
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "컨트롤러 보이기")
            }
        }
    }
}

// 화면 1: 분석 시작 유도 화면 (변경 없음)
@Composable
private fun StartAnalysisScreen(onRequestAnalysis: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "갤러리에 숨어있는 비슷하거나 중복된 사진을 찾아 정리해 보세요.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestAnalysis,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("유사한 사진 분석하기", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// 화면 2: 분석 진행 중 화면 (변경 없음)
@Composable
private fun AnalysisInProgressScreen(progress: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "유사한 사진을 분석하는 중입니다...", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ClusteredPhotosList(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    state: SimilarPhotosState,
    onEvent: (SimilarPhotosEvent) -> Unit,
    onNavigateToImageDetail: (sharedKey : String, imageId: String, clusterId: String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // 외부 패딩은 MainScreen에서, 내부 패딩은 여기서 관리
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp) // 아이템 그룹 간의 수직 간격
    ) {
        // 1. 리스트 최상단 헤더 (변경 없음)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "분석 결과", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { onEvent(SimilarPhotosEvent.OnRequestAnalysis) }) {
                    Text("다시 분석")
                }
            }
        }

        // 2. 각 클러스터를 헤더와 그리드 행으로 렌더링
        state.photoClusters?.forEachIndexed { index, cluster ->
            // 2-1. 클러스터 헤더
            item(key = "header_${cluster.id}") {
                Text(
                    text = "유사 사진 그룹 ${index + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp) // 헤더와 그리드 사이 간격
                )
            }

            // 2-2. 이미지를 4개씩 묶어 행으로 만듦
            val imageRows = cluster.images.chunked(4)

            // 2-3. 각 행을 LazyColumn의 아이템으로 렌더링 (LazyVerticalGrid 대체)
            items(
                items = imageRows,
                key = { row -> row.joinToString { it.id } } // 각 행에 대한 고유 키
            ) { rowImages ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 한 행에 포함된 이미지들을 렌더링
                    rowImages.forEach { image ->
                        GalleryImageItem(
                            keyPrefix = "similar",
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            image = image,
                            isSelected = image.id in state.selectedPhotoIds,
                            onClick = {
                                if (state.isSelectionMode) {
                                    onEvent(SimilarPhotosEvent.OnTogglePhotoSelection(image.id))
                                } else {
                                    onNavigateToImageDetail("similar_${image.id}", image.id, cluster.id)
                                }
                            },
                            onLongClick = { onEvent(SimilarPhotosEvent.OnImageLongClick(image.id)) },
                            modifier = Modifier
                                .weight(1f) // 모든 아이템이 동일한 너비를 갖도록 설정
                                .aspectRatio(1f) // 정사각형 비율 유지
                        )
                    }

                    // 마지막 행의 아이템이 4개 미만일 경우, 레이아웃을 유지하기 위해 빈 공간을 채움
                    if (rowImages.size < 4) {
                        for (i in 0 until (4 - rowImages.size)) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 3. 하단 컨트롤러에 의해 가려지지 않도록 공간 확보 (변경 없음)
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// 공통 메시지 컴포넌트 (재분석 버튼 추가)
@Composable
private fun InfoMessage(text: String, onRequestAnalysis: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestAnalysis) {
            Text("다시 분석하기")
        }
    }
}
