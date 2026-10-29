package com.ssafy.keepick.feature.group.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupTopBar(
    groupName: String,
    onAddClick: () -> Unit,
    onVideoClick: () -> Unit,
    onGroupSelectClick: () -> Unit,
    onBackClick: () -> Unit
) {
    // TopAppBar 대신 CenterAlignedTopAppBar를 직접 사용합니다.
    CenterAlignedTopAppBar(
        title = {
            Box {

                Row(
                    modifier = Modifier.clickable { onGroupSelectClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = groupName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "그룹 선택")
                }
            }
        },
        actions = {
            IconButton(onClick = onAddClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "추가")
            }
            IconButton(onClick = onVideoClick) {
                Icon(imageVector = Icons.Outlined.Videocam, contentDescription = "영상통화")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White
        )
    )
}
