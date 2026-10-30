package com.ssafy.keepick.feature.tier.tiermaker.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
import com.ssafy.keepick.feature.tier.tiermaker.Tier

private const val TAG = "TierMaker"

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TierRow(
    tier: Tier,
    draggedItem: DisplayGroupAlbum?,
    isDropTarget: Boolean,
    indicatorIndex: Int?,
    onDragStart: (item: DisplayGroupAlbum, sourceTierId: String) -> Unit,
    onDragOver: (targetTierId: String, targetIndex: Int) -> Unit,
    onDrop: (targetTierId: String, targetIndex: Int) -> Unit,
    onDragOperationEnd: () -> Unit
) {
    val tierColor = when (tier.id) {
        "S" -> Color(0xFFFFD700); "A" -> Color(0xFF4CAF50); "B" -> Color(0xFF2196F3);
        "C" -> Color(0xFFF44336); "D" -> Color(0xFF9E9E9E); else -> Color.Transparent
    }

    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    val titleBoxWidth = 44.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = remember(tier.items) {
                    object : DragAndDropTarget {
                        private fun getTargetIndex(event: DragAndDropEvent): Int {
                            val titleBoxWidthPx = with(density) { titleBoxWidth.toPx() }
                            return calculateInsertionIndex(
                                dragX = event.toAndroidDragEvent().x,
                                lazyListState = lazyListState,
                                itemCount = tier.items.size,
                                contentStartOffsetPx = titleBoxWidthPx
                            )
                        }
                        override fun onEntered(event: DragAndDropEvent) {
                            onDragOver(tier.id, getTargetIndex(event))
                        }
                        override fun onMoved(event: DragAndDropEvent) {
                            onDragOver(tier.id, getTargetIndex(event))
                        }
                        override fun onExited(event: DragAndDropEvent) {
                            // Row 전체가 타겟이므로, isDraggingOver 관리는 onDragOver에서 처리
                        }
                        override fun onDrop(event: DragAndDropEvent): Boolean {
                            val finalIndex = getTargetIndex(event)
                            Log.d(TAG, "DROP in tier ${tier.id}, final calculated index: $finalIndex")
                            onDrop(tier.id, finalIndex)
                            return true
                        }
                        override fun onEnded(event: DragAndDropEvent) {
                            onDragOperationEnd()
                        }
                    }
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 티어 제목 Box
        Box(
            modifier = Modifier
                .width(titleBoxWidth)
                .fillMaxHeight()
                .background(tierColor, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(tier.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // 2. 아이템 영역 Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                )
                // isDropTarget 상태에 따라 배경색 변경
                .background(
                    if (isDropTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else Color.Transparent
                )
        ) {
            if (tier.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("이곳에 사진을 놓으세요", color = Color.Gray, fontSize = 12.sp)
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    state = lazyListState,
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tier.items, key = { _, item -> item.id }) { index, item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isDropTarget && indicatorIndex == index) {
                                InsertionIndicator()
                                Spacer(Modifier.width(4.dp))
                            }
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    alpha = if (draggedItem?.id == item.id) 0.5f else 1f
                                }
                            ) {
                                DraggableImage(item, tier.id, onDragStart)
                            }
                        }
                    }
                    item {
                        if (isDropTarget && indicatorIndex == tier.items.size) {
                            InsertionIndicator()
                        }
                    }
                }
            }
        }
    }
}

private fun calculateInsertionIndex(
    dragX: Float,
    lazyListState: LazyListState,
    itemCount: Int,
    contentStartOffsetPx: Float // !! 핵심 수정 2: 제목 박스 너비를 파라미터로 받음 !!
): Int {
    val contentRelativeDragX = dragX - contentStartOffsetPx
    if (contentRelativeDragX < 0) {
//        Log.d(TAG, "Hovering over title area. FINAL Index: 0")
        return 0
    }

    val dataItems = lazyListState.layoutInfo.visibleItemsInfo.filter { it.index < itemCount }
    if (dataItems.isEmpty()) {
        return 0
    }

//    Log.d(TAG, "---------------------------------")
//    Log.d(TAG, "dragX: $dragX, contentRelativeDragX: $contentRelativeDragX, itemCount: $itemCount")

    val listRelativeDragX = contentRelativeDragX - lazyListState.layoutInfo.beforeContentPadding
//    Log.d(TAG, "-> listRelativeDragX (보정): $listRelativeDragX")

    val targetItem = dataItems.firstOrNull {
        val midpoint = it.offset + (it.size / 2)
//        Log.d(TAG, "  - Checking item [${it.index}] (offset: ${it.offset}, size: ${it.size}, midpoint: $midpoint)")
        listRelativeDragX < midpoint
    }

    return if (targetItem == null) {
//        Log.d(TAG, "-> FINAL: Dragged past all midpoints. Index: $itemCount")
        itemCount
    } else {
//        Log.d(TAG, "-> FINAL: Found target item [${targetItem.index}]. Inserting at index: ${targetItem.index}")
        targetItem.index
    }
}
