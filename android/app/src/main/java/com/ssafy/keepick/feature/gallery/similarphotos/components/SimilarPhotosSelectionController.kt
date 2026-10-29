package com.ssafy.keepick.feature.gallery.similarphotos.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SimilarPhotosSelectionController(
    modifier: Modifier = Modifier,
    selectedCount: Int,
    onDeselectAll: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$selectedCount 개 선택됨", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = onDeselectAll,
                enabled = selectedCount > 0
            ) {
                Text("선택 해제")
            }

            IconButton(onClick = onDeleteClick, enabled = selectedCount > 0) {
                Icon(Icons.Default.Delete, contentDescription = "삭제")
            }

            IconButton(onClick = onDismissRequest) {
                Icon(Icons.Default.Close, contentDescription = "컨트롤러 숨기기")
            }
        }
    }
}
