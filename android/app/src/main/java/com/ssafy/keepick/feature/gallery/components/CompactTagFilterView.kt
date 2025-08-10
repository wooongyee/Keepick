package com.ssafy.keepick.feature.gallery.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactTagFilterView(
    modifier: Modifier = Modifier,
    specialAiTags: List<String>,
    personTags: List<String>,
    generalTags: List<String>,
    selectedTags: List<String>,
    isSelectionMode: Boolean, // 1. 선택 모드 상태 추가
    isShowingOnlySelected: Boolean, // 2. '선택됨' 필터 활성화 상태 추가
    onTagClick: (String) -> Unit,
    onFilterIconClick: () -> Unit,
    onSimilarPhotosClick: () -> Unit,
    onShowOnlySelectedToggle: (Boolean) -> Unit, // 3. '선택됨' 필터 토글 콜백 추가
) {
    val bottomRowTags by remember(personTags, generalTags) {
        derivedStateOf { personTags + generalTags }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 기존 필터들은 '선택됨' 모드가 아닐 때만 활성화
            FilterChip(
                enabled = !isShowingOnlySelected,
                selected = selectedTags.isEmpty(),
                onClick = { onTagClick("전체") },
                label = { Text("전체") }
            )
            Spacer(Modifier.width(8.dp))
            // 4. 선택 모드일 때만 '선택됨' 필터 칩 표시
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row {
                    FilterChip(
                        selected = isShowingOnlySelected,
                        onClick = { onShowOnlySelectedToggle(!isShowingOnlySelected) },
                        label = { Text("선택됨") },
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }

            specialAiTags.forEach { tag ->
                FilterChip(
                    enabled = !isShowingOnlySelected,
                    selected = tag in selectedTags,
                    onClick = { onTagClick(tag) },
                    label = { Text(tag) }
                )
                Spacer(Modifier.width(8.dp))
            }

            Spacer(Modifier.weight(1f))

            // AI 기능(유사 사진)으로 이동하는 아이콘 버튼 추가
            IconButton(onClick = onSimilarPhotosClick) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "유사 사진 분석")
            }

            IconButton(onClick = onFilterIconClick) {
                Icon(Icons.Default.FilterAlt, contentDescription = "상세 필터 보기")
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bottomRowTags, key = { it }) { tag ->
                FilterChip(
                    enabled = !isShowingOnlySelected,
                    selected = tag in selectedTags,
                    onClick = { onTagClick(tag) },
                    label = { Text(tag) }
                )
            }
        }
    }
}
