// feature/tier/tieralbum/TierAlbumScreen.kt

package com.ssafy.keepick.feature.tier.tieralbum

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.R
import com.ssafy.keepick.core.designsystem.theme.KeepickTheme
import com.ssafy.keepick.feature.tier.tieralbum.components.TierAlbumTopAppBar
import com.ssafy.keepick.feature.tier.tieralbum.components.TierCardDeck
import com.ssafy.keepick.feature.tier.tieralbum.components.TierPhotoGrid
import com.ssafy.keepick.feature.tier.tieralbum.components.TierSelector
import com.ssafy.keepick.feature.tier.tiermaker.Tier

@Composable
fun TierAlbumScreen(
    albumTitle: String,
    allTiers: List<Tier>,
    selectedTier: Tier?,
    onTierSelected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TierAlbumTopAppBar(
                title = "티어 앨범",
                onNavigateBack = onNavigateBack,
                onEditClick = onEditClick,
                onCopyClick = onCopyClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TierSelector(
                tiers = allTiers,
                selectedTierId = selectedTier?.id,
                onTierSelected = onTierSelected,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (selectedTier != null) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // S등급, "카드를 넘겨가며 감상해보세요" 부분
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        // 등급 아이콘 (임시)
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground), // TODO: 적절한 아이콘으로 교체
                            contentDescription = "${selectedTier.title} 등급",
                            modifier = Modifier
                                .size(32.dp),
                            tint = Color.Gray
                        )
                        Column {
                            Text(
                                text = "${selectedTier.title}등급",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "카드를 넘겨가며 감상해보세요",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // 메인 카드 덱
                TierCardDeck(
                    photos = selectedTier.items,
                    modifier = Modifier.height(350.dp) // 카드 덱 영역 높이 지정
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 하단 전체 사진 그리드 (개선된 UI)
                TierPhotoGrid(
                    photos = selectedTier.items,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )

            } else {
                // 선택된 티어가 없을 경우 (데이터 로딩 실패 등)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("티어 정보를 불러올 수 없습니다.")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TierAlbumScreenPreview() {
    KeepickTheme {
        TierAlbumRoute(tierAlbumId = "preview", onNavigateBack = {}, onNavigateToEdit = {})
    }
}
