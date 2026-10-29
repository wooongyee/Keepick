package com.ssafy.keepick.feature.mypage.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ssafy.keepick.feature.mypage.DisplayFriend
import com.ssafy.keepick.feature.mypage.EmptyContentView
import com.ssafy.keepick.feature.mypage.SectionHeader


@Composable
fun FriendListContent(friends: List<DisplayFriend>) {
    if (friends.isEmpty()) {
        EmptyContentView(message = "친구가 없습니다.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item { SectionHeader(title = "내 친구 (${friends.size})") }
        items(items = friends, key = { it.name }) { friend ->
            FriendListItem(friend = friend)
        }
    }
}

@Composable
fun FriendListItem(friend: DisplayFriend) {
    FriendListItemBase(
        imageUrl = friend.imageUrl,
        name = friend.name,
        subtitle = friend.subtitle,
        actions = null
    )
}

@Composable
fun FriendListItemBase(
    modifier: Modifier = Modifier,
    imageUrl: String,
    name: String,
    subtitle: String?,
    actions: (@Composable () -> Unit)?
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "$name 프로필 사진",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        if (actions != null) {
            actions()
        }
    }
}
