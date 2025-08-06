package com.ssafy.keepick.feature.tier.tiermaker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ssafy.keepick.R
import com.ssafy.keepick.feature.group.DisplayGroupAlbum

private const val TAG = "TierMaker"

data class Tier(val id: String, val title: String, val items: List<DisplayGroupAlbum>)
data class DragInfo(val item: DisplayGroupAlbum, val sourceTierId: String, val sourceIndex: Int)
data class DropTargetInfo(val targetTierId: String, val targetIndex: Int)
private fun Int.toDrawableUri(context: Context): String {
    return Uri.parse("android.resource://${context.packageName}/$this").toString()
}

@Composable
fun TierMakerRoute(
    albumId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val availablePhotos = remember {
        mutableStateOf(
            (1..15).map {
                DisplayGroupAlbum(
                    id = it,
                    thumbnailUrl = if(it < 5) R.drawable.dummy_group.toDrawableUri(context) else R.drawable.dummy_album.toDrawableUri(context),
                    title = "사진 $it",
                    photoCount = 0, date = "", creator = ""
                )
            }
        )
    }

    var tiers by remember {
        mutableStateOf(
            listOf(
                Tier("S", "S", emptyList()),
                Tier("A", "A", emptyList()),
                Tier("B", "B", emptyList()),
                Tier("C", "C", emptyList()),
                Tier("D", "D", emptyList()),
                Tier("unranked", "사용 가능한 사진", availablePhotos.value)
            )
        )
    }

    var dragInfo by remember { mutableStateOf<DragInfo?>(null) }
    var dropTargetInfo by remember { mutableStateOf<DropTargetInfo?>(null) }
    var isBattleMode by remember { mutableStateOf(false) }

    TierMakerScreen(
        albumTitle = "제주도 제일 재밌었던 곳 뽑기!",
        tiers = tiers,
        isBattleMode = isBattleMode,
        draggedItem = dragInfo?.item,
        dropTargetInfo = dropTargetInfo,
        onDragStart = { item, sourceTierId ->
            val sourceTier = tiers.find { it.id == sourceTierId }
            val sourceIndex = sourceTier?.items?.indexOf(item) ?: -1
            if (sourceIndex != -1) {
                dragInfo = DragInfo(item, sourceTierId, sourceIndex)
                Log.d(TAG, "DragStart: ${item.title} from tier $sourceTierId at index $sourceIndex")
            }
        },
        onDragOver = { targetTierId, targetIndex ->
            if (dragInfo != null) {
                dropTargetInfo = DropTargetInfo(targetTierId, targetIndex)
            }
        },
        onDrop = { targetTierId, targetIndex ->
            val currentDrag = dragInfo
            Log.d(TAG, "onDrop: Dragging ${currentDrag?.item?.title} to tier $targetTierId at index $targetIndex")

            if (currentDrag != null) {
                val sourceTierId = currentDrag.sourceTierId
                val itemToMove = currentDrag.item

                tiers = tiers.toMutableList().apply {
                    val sourceTierIndex = indexOfFirst { it.id == sourceTierId }
                    val targetTierIndex = indexOfFirst { it.id == targetTierId }

                    if (sourceTierIndex == -1 || targetTierIndex == -1) {
                        Log.e(TAG, "onDrop: Invalid tier index.")
                        return@apply
                    }

                    if (sourceTierId == targetTierId) {
                        // 같은 티어 내에서 이동
                        val tier = this[sourceTierIndex]
                        val newList = tier.items.toMutableList().apply {
                            val movedItem = removeAt(currentDrag.sourceIndex)
                            // 드롭 시점에 계산된 최종 인덱스를 사용
                            val finalIndex = targetIndex.coerceIn(0, size)
                            add(finalIndex, movedItem)
                        }
                        this[sourceTierIndex] = tier.copy(items = newList)
                        Log.d(TAG, "Moved within tier $sourceTierId from ${currentDrag.sourceIndex} to $targetIndex")

                    } else {
                        // 다른 티어로 이동
                        // 1. 소스에서 제거
                        val sourceTier = this[sourceTierIndex]
                        val newSourceItems = sourceTier.items.filterNot { it.id == itemToMove.id }
                        this[sourceTierIndex] = sourceTier.copy(items = newSourceItems)
                        Log.d(TAG, "Removed from tier $sourceTierId. New count: ${newSourceItems.size}")

                        // 2. 타겟에 추가 (상태 불일치 방지를 위해 수정된 리스트에서 타겟을 다시 찾음)
                        val updatedTargetTierIndex = indexOfFirst { it.id == targetTierId }
                        val targetTier = this[updatedTargetTierIndex]
                        val newTargetItems = targetTier.items.toMutableList().apply {
                            // 드롭 시점에 계산된 최종 인덱스를 사용
                            val insertionIndex = targetIndex.coerceIn(0, size)
                            add(insertionIndex, itemToMove)
                        }
                        this[updatedTargetTierIndex] = targetTier.copy(items = newTargetItems)
                        Log.d(TAG, "Added to tier $targetTierId. New count: ${newTargetItems.size}")
                    }
                }
            }
        },
        onDragOperationEnd = {
            Log.d(TAG, "Drag operation ended.")
            dragInfo = null
            dropTargetInfo = null
        },
        onNavigateBack = onNavigateBack,
        onSaveClick = { /* TODO */ },
        onShareClick = { /* TODO */ },
        onBattleModeToggle = { isBattleMode = it }
    )
}
