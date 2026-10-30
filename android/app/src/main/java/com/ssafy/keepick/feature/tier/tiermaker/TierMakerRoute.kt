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

// --- Data Classes ---
data class Tier(val id: String, val title: String, val items: List<DisplayGroupAlbum>)
data class DragInfo(val item: DisplayGroupAlbum, val sourceTierId: String, val sourceIndex: Int)
data class DropTargetInfo(val targetTierId: String, val targetIndex: Int)

/** 배틀 상태를 관리하는 데이터 클래스 */
data class Battle(
    val newPhoto: DisplayGroupAlbum,
    val opponents: List<DisplayGroupAlbum>,
    val currentOpponentIndex: Int,
    val targetTierId: String,
    val initialTargetIndex: Int,
    val sourceInfo: DragInfo
)

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
                    thumbnailUrl = if (it < 5) R.drawable.dummy_group.toDrawableUri(context)
                    else if (it < 10) R.drawable.dummy_profile.toDrawableUri(context)
                    else R.drawable.dummy_album.toDrawableUri(context),
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
    var battleState by remember { mutableStateOf<Battle?>(null) }

    val insertItemIntoTier = { itemToMove: DisplayGroupAlbum, sourceTierId: String, targetTierId: String, finalIndex: Int ->
        val newTiers = tiers
            .map { tier ->
                if (tier.id == sourceTierId) tier.copy(items = tier.items.filterNot { it.id == itemToMove.id })
                else tier
            }
            .map { tier ->
                if (tier.id == targetTierId) {
                    val newItems = tier.items.toMutableList().apply { add(finalIndex.coerceIn(0, size), itemToMove) }
                    tier.copy(items = newItems)
                } else {
                    tier
                }
            }
        tiers = newTiers
        Log.d(TAG, "Tiers updated. Moved ${itemToMove.title} from $sourceTierId to $targetTierId at index $finalIndex")
    }

    // !! 핵심 수정: 배틀 결과 처리 로직을 사용자의 의도에 맞게 완벽히 수정 !!
    val handleBattleResult = { newPhotoWon: Boolean ->
        val currentBattle = battleState
        if (currentBattle != null) {
            if (newPhotoWon) {
                // "새로운 사진"이 더 높다 -> 다음으로 더 높은 순위의 상대와 배틀 계속
                val nextOpponentIndex = currentBattle.currentOpponentIndex + 1
                if (nextOpponentIndex < currentBattle.opponents.size) {
                    Log.d(TAG, "Battle Continue: New photo won. Challenging next higher rank...")
                    battleState = currentBattle.copy(currentOpponentIndex = nextOpponentIndex)
                } else {
                    // 모든 상대에게 이겼다 -> 최고 순위에 삽입
                    val finalIndex = currentBattle.initialTargetIndex - currentBattle.opponents.size
                    Log.d(TAG, "Battle End: New photo won all battles. Inserting at top position: $finalIndex.")
                    insertItemIntoTier(currentBattle.newPhoto, currentBattle.sourceInfo.sourceTierId, currentBattle.targetTierId, finalIndex)
                    battleState = null // 배틀 종료
                }
            } else {
                // "기존 사진"이 더 높다 -> 배틀 종료, 현재 상대 바로 뒤에 삽입
                val opponent = currentBattle.opponents[currentBattle.currentOpponentIndex]
                val targetTier = tiers.first { it.id == currentBattle.targetTierId }

                // 드래그 중인 아이템을 제외한 리스트에서 상대방의 현재 인덱스를 찾아야 정확함
                val opponentIndexInFilteredList = targetTier.items
                    .filterNot { it.id == currentBattle.newPhoto.id }
                    .indexOf(opponent)

                // 삽입 위치는 상대방 인덱스 바로 뒤(+1)가 됨
                val finalIndex = opponentIndexInFilteredList + 1

                Log.d(TAG, "Battle End: New photo lost against ${opponent.title}. Inserting at index $finalIndex (after opponent).")
                insertItemIntoTier(currentBattle.newPhoto, currentBattle.sourceInfo.sourceTierId, currentBattle.targetTierId, finalIndex)
                battleState = null // 배틀 종료
            }
        }
    }

    TierMakerScreen(
        albumTitle = "제주도 제일 재밌었던 곳 뽑기!",
        tiers = tiers,
        isBattleMode = isBattleMode,
        draggedItem = dragInfo?.item,
        dropTargetInfo = dropTargetInfo,
        battleState = battleState,
        onDragStart = { item, sourceTierId ->
            val sourceTier = tiers.find { it.id == sourceTierId }
            val sourceIndex = sourceTier?.items?.indexOf(item) ?: -1
            if (sourceIndex != -1) {
                dragInfo = DragInfo(item, sourceTierId, sourceIndex)
            }
        },
        onDragOver = { targetTierId, targetIndex ->
            if (dragInfo != null) {
                dropTargetInfo = DropTargetInfo(targetTierId, targetIndex)
            }
        },
        onDrop = { targetTierId, targetIndex ->
            val currentDrag = dragInfo
            if (currentDrag != null) {
                val targetTier = tiers.firstOrNull { it.id == targetTierId }
                if (targetTier != null) {
                    val isMeaningfulDrop = !(targetTierId == currentDrag.sourceTierId && targetIndex == currentDrag.sourceIndex)
                    if (isMeaningfulDrop) {
                        if (isBattleMode && targetTier.items.isNotEmpty() && targetTierId != "unranked") {
                            val opponents = targetTier.items
                                .subList(0, targetIndex)
                                .filterNot { it.id == currentDrag.item.id }
                                .asReversed()

                            if (opponents.isNotEmpty()) {
                                Log.d(TAG, "Battle initiated. Opponents: ${opponents.map { it.title }}")
                                battleState = Battle(
                                    newPhoto = currentDrag.item,
                                    opponents = opponents,
                                    currentOpponentIndex = 0,
                                    targetTierId = targetTierId,
                                    initialTargetIndex = targetIndex,
                                    sourceInfo = currentDrag
                                )
                            } else {
                                insertItemIntoTier(currentDrag.item, currentDrag.sourceTierId, targetTierId, targetIndex)
                            }
                        } else {
                            insertItemIntoTier(currentDrag.item, currentDrag.sourceTierId, targetTierId, targetIndex)
                        }
                    }
                }
            }
        },
        onDragOperationEnd = {
            dragInfo = null
            dropTargetInfo = null
        },
        onBattleResult = handleBattleResult,
        onDismissBattle = { battleState = null },
        onNavigateBack = onNavigateBack,
        onSaveClick = { /* TODO */ },
        onShareClick = { /* TODO */ },
        onBattleModeToggle = { isBattleMode = it }
    )
}
