package com.ssafy.keepick.feature.groupchat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FloatingHeader(onMaximizeClick: () -> Unit, onIconifyClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Green))
        Spacer(Modifier.width(8.dp))
        Text("가족 모임 그룹챗", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.Fullscreen, "전체화면", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp).clickable(onClick = onMaximizeClick))
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Default.Close, "아이콘으로", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp).clickable(onClick = onIconifyClick))
    }
}

@Composable
fun FullscreenHeader(onBackClick: () -> Unit, onInviteClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = Color.White) }
        Text("그룹챗", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onInviteClick) { Icon(Icons.Default.PersonAdd, "초대하기", tint = Color.White) }
    }
}
