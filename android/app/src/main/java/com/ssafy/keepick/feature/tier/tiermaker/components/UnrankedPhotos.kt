package com.ssafy.keepick.feature.tier.tiermaker.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
import com.ssafy.keepick.feature.tier.tiermaker.Tier

private const val TAG = "UnrankedPhotos"

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun UnrankedPhotos(
    tier: Tier,
    draggedItem: DisplayGroupAlbum?,
    isDropTarget: Boolean,
    indicatorIndex: Int?,
    onDrop: (targetTierId: String, targetIndex: Int) -> Unit,
    onDragStart: (item: DisplayGroupAlbum, sourceTierId: String) -> Unit,
    onDragOver: (targetTierId: String, targetIndex: Int) -> Unit,
    onDragOperationEnd: () -> Unit
) {
    // !! 핵심 수정 1: 부모가 weight(1f)로 공간을 할당했으므로, 이 Column은 그 공간을 모두 차지하도록 함 !!
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "사용 가능한 사진 (${tier.items.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // !! 핵심 수정 2: weight(1f)를 사용하여 제목을 제외한 나머지 공간을 모두 채움 !!
                // 이렇게 하면 사진이 적어도 영역이 줄어들지 않음.
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isDropTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(8.dp)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = remember(tier.items) {
                        object : DragAndDropTarget {
                            override fun onEntered(event: DragAndDropEvent) {
                                onDragOver(tier.id, tier.items.size)
                            }
                            override fun onExited(event: DragAndDropEvent) {}
                            override fun onDrop(event: DragAndDropEvent): Boolean {
                                onDrop(tier.id, tier.items.size); return true
                            }
                            override fun onEnded(event: DragAndDropEvent) {
                                onDragOperationEnd()
                            }
                        }
                    }
                )
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 64.dp),
                // !! 핵심 수정 3: 간격을 8.dp로 통일 !!
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                // !! 핵심 수정 4: heightIn 제약조건을 없애고 부모 Box를 꽉 채우도록 함 !!
                modifier = Modifier.fillMaxSize()
            ) {
                items(tier.items, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = if (draggedItem?.id == item.id) 0.5f else 1f
                        }
                    ) {
                        DraggableImage(
                            item = item,
                            sourceTierId = tier.id,
                            onDragStart = onDragStart
                        )
                    }
                }
            }
        }
    }
}
