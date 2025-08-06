package com.ssafy.keepick.feature.tier.tiermaker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.keepick.core.designsystem.theme.KeepickTheme
import com.ssafy.keepick.feature.group.DisplayGroupAlbum
import com.ssafy.keepick.feature.tier.tiermaker.components.TierMakerTopAppBar
import com.ssafy.keepick.feature.tier.tiermaker.components.TierRow
import com.ssafy.keepick.feature.tier.tiermaker.components.UnrankedPhotos

@Composable
fun TierMakerScreen(
    albumTitle: String,
    tiers: List<Tier>,
    isBattleMode: Boolean,
    draggedItem: DisplayGroupAlbum?,
    dropTargetInfo: DropTargetInfo?,
    onDragStart: (item: DisplayGroupAlbum, sourceTierId: String) -> Unit,
    onDragOver: (targetTierId: String, targetIndex: Int) -> Unit,
    onDrop: (targetTierId: String, targetIndex: Int) -> Unit,
    onDragOperationEnd: () -> Unit,
    onNavigateBack: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onBattleModeToggle: (Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TierMakerTopAppBar(
                title = albumTitle,
                isBattleMode,
                onNavigateBack,
                onSaveClick,
                onShareClick,
                onBattleModeToggle
            )
        }
    ) { paddingValues ->
        // !! 핵심 수정 1: LazyColumn을 Column으로 변경하여 레이아웃을 분리 !!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // --- 상단 고정 영역 ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(modifier = Modifier.height(8.dp))
                // LazyColumn의 items 대신 forEach를 사용하여 고정된 리스트를 렌더링
                tiers.filter { it.id != "unranked" }.forEach { tier ->
                    TierRow(
                        tier = tier,
                        draggedItem = draggedItem,
                        isDropTarget = dropTargetInfo?.targetTierId == tier.id,
                        indicatorIndex = if (dropTargetInfo?.targetTierId == tier.id) dropTargetInfo.targetIndex else null,
                        onDragStart = onDragStart,
                        onDragOver = onDragOver,
                        onDrop = onDrop,
                        onDragOperationEnd = onDragOperationEnd
                    )
                }
            }

            // --- 하단 스크롤 가능 영역 ---
            Spacer(modifier = Modifier.height(16.dp))

            // !! 핵심 수정 2: weight(1f)를 사용하여 남은 공간을 모두 차지하도록 함 !!
            Column(modifier = Modifier.weight(1f)) {
                tiers.firstOrNull { it.id == "unranked" }?.let { unrankedTier ->
                    UnrankedPhotos(
                        tier = unrankedTier,
                        draggedItem = draggedItem,
                        isDropTarget = dropTargetInfo?.targetTierId == "unranked",
                        indicatorIndex = if (dropTargetInfo?.targetTierId == "unranked") dropTargetInfo.targetIndex else null,
                        onDragStart = onDragStart,
                        onDragOver = onDragOver,
                        onDrop = onDrop,
                        onDragOperationEnd = onDragOperationEnd
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true, name = "TierMaker 모바일 미리보기")
@Composable
fun TierMakerScreenPreview() {
    KeepickTheme {
        TierMakerRoute(albumId = "preview", onNavigateBack = {})
    }
}
