package com.ssafy.keepick.feature.group.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupAlbumTabView(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = Color.Black,
        indicator = {
            if (selectedTabIndex < tabs.size) {
                HorizontalDivider(
                    modifier = Modifier
                        // tabIndicatorOffset에는 'selectedTabIndex' (Int)를 직접 전달합니다.
                        .tabIndicatorOffset(selectedTabIndex)
                        .height(3.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
                    color = Color.Black
                )
            }
        },
        divider = {
            HorizontalDivider(color = Color.Transparent)
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        // 선택된 탭은 Bold, 아니면 Normal
                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                        // 선택되지 않은 탭은 회색으로 처리
                        color = if (selectedTabIndex == index) Color.Black else Color.Gray
                    )
                },
                unselectedContentColor = Color.Gray
            )
        }
    }
}

