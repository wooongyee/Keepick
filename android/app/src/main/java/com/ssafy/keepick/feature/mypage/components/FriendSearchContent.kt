package com.ssafy.keepick.feature.mypage.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ssafy.keepick.R
import com.ssafy.keepick.feature.mypage.DisplayFriend
import com.ssafy.keepick.feature.mypage.DummyFriend
import com.ssafy.keepick.feature.mypage.EmptyContentView
import com.ssafy.keepick.feature.mypage.SectionHeader
import com.ssafy.keepick.feature.mypage.toDisplay

@Composable
fun FriendSearchContent(searchText: String) {
    val context = LocalContext.current
    val searchResults = remember(searchText) {
        if (!searchText.contains("없는")) {
            listOf(
                DummyFriend("검색된 친구1", R.drawable.dummy_profile, "공통 친구 1명").toDisplay(context)
            )
        } else { emptyList() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (searchResults.isNotEmpty()) {
            item { SectionHeader(title = "검색 결과") }
            items(items = searchResults, key = { "search_${it.name}" }) { friend ->
                FriendSearchResultItem(friend = friend, onAddFriend = {})
            }
        } else {
            item { EmptyContentView(message = "검색 결과가 없습니다.") }
        }
    }
}

@Composable
fun FriendSearchTextField(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 이제 이 컴포저블은 Column으로 감싸거나 검색 결과 로직을 가질 필요가 없습니다.
    // 오직 OutlinedTextField UI와, 그 값을 변경하는 이벤트만 부모에게 전달합니다.
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChanged, // 텍스트 변경 시, 부모에게 새 텍스트를 알림
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("이름 또는 아이디로 검색") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "검색 아이콘") },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedBorderColor = Color.Black
        )
    )
}


@Composable
fun FriendSearchResultItem(friend: DisplayFriend, onAddFriend: () -> Unit) {
    var isRequested by remember { mutableStateOf(false) }

    FriendListItemBase(
        imageUrl = friend.imageUrl,
        name = friend.name,
        subtitle = friend.subtitle
    ) {
        TextButton(
            onClick = {
                if (!isRequested) {
                    onAddFriend()
                    isRequested = true
                }
            },
            enabled = !isRequested,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (isRequested) Color.Gray else Color(0xFF8A2BE2),
                disabledContentColor = Color.Gray
            )
        ) {
            if (isRequested) {
                Icon(Icons.Default.Check, contentDescription = "요청됨", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("요청됨")
            } else {
                Icon(Icons.Default.Add, contentDescription = "추가", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("추가")
            }
        }
    }
}
