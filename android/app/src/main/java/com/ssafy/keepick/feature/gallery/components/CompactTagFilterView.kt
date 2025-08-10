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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.keepick.core.designsystem.theme.KeepickTheme

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
/**
 * 1. 기본 상태 미리보기
 * 여러 태그가 선택된 일반적인 경우를 시뮬레이션합니다.
 */
@Preview(showBackground = true, name = "Default Preview")
@Composable
fun CompactTagFilterView_DefaultPreview() {
    val specialTags = listOf("유사한 사진", "흐린 사진")
    val personTags = listOf("김민수", "이영희")
    val generalTags = listOf("풍경", "음식", "바다", "산", "도시")
    val selectedTags = listOf("풍경", "김민수")

    KeepickTheme {
        CompactTagFilterView(
            specialAiTags = specialTags,
            personTags = personTags,
            generalTags = generalTags,
            selectedTags = selectedTags,
            isSelectionMode = false, // <-- 추가
            isShowingOnlySelected = false, // <-- 추가
            onTagClick = {},
            onFilterIconClick = {},
            onShowOnlySelectedToggle = {} // <-- 추가
        )
    }
}

/**
 * 2. 선택된 태그가 없는 상태 미리보기
 * '전체' 칩이 활성화된 상태를 확인합니다.
 */
@Preview(showBackground = true, name = "No Tags Selected Preview")
@Composable
fun CompactTagFilterView_NoSelectionPreview() {
    val specialTags = listOf("유사한 사진", "흐린 사진")
    val personTags = listOf("김민수", "이영희")
    val generalTags = listOf("풍경", "음식", "바다", "산", "도시")
    val selectedTags = emptyList<String>()

    KeepickTheme {
        CompactTagFilterView(
            specialAiTags = specialTags,
            personTags = personTags,
            generalTags = generalTags,
            selectedTags = selectedTags,
            isSelectionMode = false, // <-- 추가
            isShowingOnlySelected = false, // <-- 추가
            onTagClick = {},
            onFilterIconClick = {},
            onShowOnlySelectedToggle = {} // <-- 추가
        )
    }
}

/**
 * 3. 하단 태그 목록이 비어있는 상태 미리보기
 * 인물 및 일반 태그가 없을 때의 UI를 확인합니다.
 */
@Preview(showBackground = true, name = "Empty Bottom Row Preview")
@Composable
fun CompactTagFilterView_EmptyBottomRowPreview() {
    val specialTags = listOf("유사한 사진", "흐린 사진")
    val personTags = emptyList<String>()
    val generalTags = emptyList<String>()
    val selectedTags = listOf("유사한 사진")

    KeepickTheme {
        Column {
            CompactTagFilterView(
                specialAiTags = specialTags,
                personTags = personTags,
                generalTags = generalTags,
                selectedTags = selectedTags,
                isSelectionMode = false, // <-- 추가
                isShowingOnlySelected = false, // <-- 추가
                onTagClick = {},
                onFilterIconClick = {},
                onShowOnlySelectedToggle = {} // <-- 추가
            )
            Spacer(Modifier.height(20.dp))
            CompactTagFilterView(
                specialAiTags = specialTags,
                personTags = personTags,
                generalTags = generalTags,
                selectedTags = emptyList(),
                isSelectionMode = false, // <-- 추가
                isShowingOnlySelected = false, // <-- 추가
                onTagClick = {},
                onFilterIconClick = {},
                onShowOnlySelectedToggle = {} // <-- 추가
            )
        }
    }
}
