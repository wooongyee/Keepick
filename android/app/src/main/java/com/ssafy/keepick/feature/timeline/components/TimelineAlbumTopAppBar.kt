package com.ssafy.keepick.feature.timeline.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineAlbumTopAppBar(
    isInEditMode: Boolean,
    isMomentExpanded: Boolean,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onAddMomentClick: () -> Unit,
    onSaveClick: () -> Unit,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = Color.Transparent, // 기본값을 투명으로 변경
        navigationIconContentColor = Color.White,
        titleContentColor = Color.White,
        actionIconContentColor = Color.White
    )
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            if (!isMomentExpanded) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기"
                    )
                }
            }
        },
        actions = {
            if (isMomentExpanded) {
                // 확장 시에는 아무 작업 버튼도 표시하지 않음 (TimelineMomentPage에서 자체 처리)
            } else if (isInEditMode) {
                // 앨범 수정 모드
                TextButton(onClick = onSaveClick) {
                    Text("저장", fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "완료"
                    )
                }
            } else {
                // 앨범 기본 모드
                IconButton(onClick = onAddMomentClick) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "모먼트 추가"
                    )
                }
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "수정"
                    )
                }
            }
        },
        colors = colors
    )
}
