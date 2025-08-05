package com.ssafy.keepick.feature.group.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GroupTopBar(
    groupName: String,
    onAddClick: () -> Unit,
    onVideoClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("그룹", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.clickable { /* TODO: 그룹 선택 드롭다운 */ },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(groupName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "그룹 선택")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "추가",
                modifier = Modifier.clickable { onAddClick() }
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Icon(
                imageVector = Icons.Outlined.Videocam,
                contentDescription = "영상통화",
                modifier = Modifier.clickable { onVideoClick() }
            )
        }
    }
}
