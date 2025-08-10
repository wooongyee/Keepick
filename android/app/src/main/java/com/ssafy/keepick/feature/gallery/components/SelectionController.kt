package com.ssafy.keepick.feature.gallery.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SelectionController(
    modifier: Modifier = Modifier,
    selectedCount: Int,
    totalCount: Int,
    onDeselectAll: () -> Unit,
    onCreateTierAlbumClick: () -> Unit,
    onCreateTimelineAlbumClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissRequest: () -> Unit // 컨트롤러 숨기기 요청 콜백 추가
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$selectedCount 개 선택됨", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))

                // --- 핵심 수정 사항: 버튼 로직을 '선택 해제'로 단순화 ---
                TextButton(
                    onClick = onDeselectAll,
                    enabled = selectedCount > 0 // 선택된 항목이 있을 때만 활성화
                ) {
                    Text("선택 해제") // 텍스트 고정
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제")
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "컨트롤러 숨기기")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // 하단: 앨범 생성 액션
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCreateTierAlbumClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("티어 앨범 만들기")
                }
                Button(
                    onClick = onCreateTimelineAlbumClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("타임라인 앨범 만들기")
                }
            }
        }
    }
}
