package com.ssafy.keepick.feature.group.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GroupDropdownMenu(
    expanded: Boolean,
    groups: List<String>,
    onGroupSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter // 메뉴가 상단에 위치하도록 정렬
    ) {
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        indication = null, // 클릭 효과 없음
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismissRequest
                    )
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically { fullHeight -> -fullHeight } + expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = slideOutVertically { fullHeight -> -fullHeight } + shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            // 실제 메뉴 콘텐츠
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    // 메뉴 영역 자체는 클릭 이벤트를 소비하여 뒤의 Scrim 클릭이 동작하지 않도록 함
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                shadowElevation = 4.dp
            ) {
                LazyColumn(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(groups) { groupName ->
                        ListItem(
                            headlineContent = { Text(groupName) },
                            modifier = Modifier.clickable { onGroupSelected(groupName) }
                        )
                    }
                }
            }
        }
    }
}
