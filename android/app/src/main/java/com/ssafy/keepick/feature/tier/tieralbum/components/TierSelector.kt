// feature/tier/tieralbum/components/TierSelector.kt

package com.ssafy.keepick.feature.tier.tieralbum.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.keepick.feature.tier.tiermaker.Tier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TierSelector(
    tiers: List<Tier>,
    selectedTierId: String?,
    onTierSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tiers, key = { it.id }) { tier ->
            if (tier.items.isNotEmpty()) { // 아이템이 있는 티어만 표시
                FilterChip(
                    selected = tier.id == selectedTierId,
                    onClick = { onTierSelected(tier.id) },
                    label = { Text("${tier.title}등급 (${tier.items.size})") }
                )
            }
        }
    }
}
