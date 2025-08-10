package com.ssafy.keepick.feature.gallery.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.keepick.core.designsystem.theme.KeepickTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFilterSheetContent(
    modifier: Modifier = Modifier,
    initialSelectedTags: List<String>,
    specialAiTags: List<String>,
    personTags: List<String>,
    generalTags: List<String>,
    onApply: (List<String>) -> Unit,
) {
    val selectedTags = remember { mutableStateListOf<String>().apply { addAll(initialSelectedTags) } }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val exclusiveGroup = listOf("유사한 사진", "흐린 사진")

    // --- 핵심 수정 사항: ViewModel과 동일한 선택 로직을 여기에 구현 ---
    val onTagClick: (String) -> Unit = { tag ->
        when {
            // '유사한 사진' 또는 '흐린 사진' 같은 단독 선택 태그를 클릭한 경우
            tag in exclusiveGroup -> {
                // 이미 해당 태그가 단독으로 선택된 상태이면 선택 해제, 아니면 해당 태그만 선택
                val newSelection = if (selectedTags == listOf(tag)) emptyList() else listOf(tag)
                selectedTags.clear()
                selectedTags.addAll(newSelection)
            }
            // 그 외 다중 선택 가능한 일반 태그를 클릭한 경우
            else -> {
                // 기존에 선택된 단독 선택 태그가 있다면 제거
                selectedTags.removeAll(exclusiveGroup)

                if (selectedTags.contains(tag)) {
                    selectedTags.remove(tag)
                } else {
                    if (selectedTags.size < 5) {
                        selectedTags.add(tag)
                    } else {
                        // 5개 초과 시 스낵바 표시
                        scope.launch {
                            snackbarHostState.showSnackbar("필터는 최대 5개까지 선택할 수 있습니다.")
                        }
                    }
                }
            }
        }
    }
    val buttonText = if (selectedTags.isEmpty()) "적용하기" else "${selectedTags.size}개 필터 적용하기"

    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // 일반적인 TopAppBar 높이
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "필터 선택",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                TextButton(
                    onClick = { selectedTags.clear() },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text("초기화")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // AI 추천 필터 섹션
                TagSection(
                    title = "AI 추천 필터",
                    icon = Icons.Default.Star,
                    tags = specialAiTags,
                    selectedTags = selectedTags,
                    onTagClick = onTagClick
                )

                // 2. 섹션 구분선 추가
                Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))

                // 4. 인물별 필터 섹션 (빈 목록 처리 추가)
                if (personTags.isNotEmpty()) {
                    TagSection(
                        title = "인물별 보기",
                        icon = Icons.Default.Face,
                        tags = personTags,
                        selectedTags = selectedTags,
                        onTagClick = onTagClick
                    )
                } else {
                    EmptyTagSection(title = "인물별 보기", icon = Icons.Default.Face, message = "분석된 인물 태그가 없습니다.")
                }

                Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))

                // 일반 태그 섹션
                GeneralTagSection(
                    tags = generalTags,
                    selectedTags = selectedTags,
                    onTagClick = onTagClick
                )
                Spacer(Modifier.height(16.dp))
            }

            Button(
                onClick = { onApply(selectedTags.toList()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(buttonText) // 동적 텍스트 적용
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

/**
 * 태그 목록이 비어있을 때 표시할 섹션
 */
@Composable
private fun EmptyTagSection(title: String, icon: ImageVector, message: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/**
 * AI 추천, 인물별 보기와 같이 가로로 스크롤되는 태그 섹션
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagSection(
    title: String,
    icon: ImageVector,
    tags: List<String>,
    selectedTags: List<String>,
    onTagClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags) { tag ->
                FilterChip(
                    selected = tag in selectedTags,
                    onClick = { onTagClick(tag) },
                    label = { Text(tag) }
                )
            }
        }
    }
}

/**
 * 여러 줄로 표시될 수 있는 일반 태그 섹션
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GeneralTagSection(
    tags: List<String>,
    selectedTags: List<String>,
    onTagClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("일반 태그", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tags.forEach { tag ->
                FilterChip(
                    selected = tag in selectedTags,
                    onClick = { onTagClick(tag) },
                    label = { Text(tag) }
                )
            }
        }
    }
}

/**
 * 기본 상태 미리보기
 * 모든 종류의 태그가 표시되는 일반적인 경우를 시뮬레이션합니다.
 */
@Preview(showBackground = true, name = "Default Preview")
@Composable
fun TagFilterSheetContent_DefaultPreview() {
    // 샘플 데이터 정의
    val specialTags = listOf("유사한 사진", "흐린 사진")
    val personTags = listOf("김민수", "이영희", "박철수", "고양이")
    val generalTags = listOf("풍경", "음식", "바다", "산", "도시", "노을", "강아지", "여행")
    val initialSelection = listOf("흐린 사진", "이영희")

    KeepickTheme { // 앱의 전체 테마로 감싸주면 더 정확한 미리보기가 가능합니다.
        TagFilterSheetContent(
            modifier = Modifier.fillMaxHeight(0.7f),
            initialSelectedTags = initialSelection,
            specialAiTags = specialTags,
            personTags = personTags,
            generalTags = generalTags,
            onApply = {} // 프리뷰에서는 실제 동작이 필요 없으므로 비워둡니다.
        )
    }
}

/**
 * 인물 태그가 없는 상태 미리보기
 * AI가 인물을 분석하지 못했거나 없는 경우를 시뮬레이션합니다.
 */
@Preview(showBackground = true, name = "No Person Tags Preview")
@Composable
fun TagFilterSheetContent_NoPersonTagsPreview() {
    // 샘플 데이터 정의
    val specialTags = listOf("유사한 사진", "흐린 사진")
    val personTags = emptyList<String>() // 인물 태그가 없는 상황
    val generalTags = listOf("풍경", "음식", "바다", "산", "도시")
    val initialSelection = listOf("풍경")

    KeepickTheme {
        TagFilterSheetContent(
            modifier = Modifier.fillMaxHeight(0.7f),
            initialSelectedTags = initialSelection,
            specialAiTags = specialTags,
            personTags = personTags, // 비어있는 리스트 전달
            generalTags = generalTags,
            onApply = {}
        )
    }
}
