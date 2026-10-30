package com.ssafy.keepick.feature.tier.tiermaker.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.ssafy.keepick.feature.group.DisplayGroupAlbum

@Composable
fun BattleDialog(
    newPhoto: DisplayGroupAlbum,
    opponentPhoto: DisplayGroupAlbum,
    onResult: (newPhotoWon: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("티어 배틀", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("더 높은 순위에 두고 싶은 추억을 선택해주세요", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BattleItem(photo = opponentPhoto, title = "기존 추억", onSelect = { onResult(false) })
                    Text("VS", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    BattleItem(photo = newPhoto, title = "새로운 추억", onSelect = { onResult(true) })
                }

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("취소")
                }
            }
        }
    }
}

@Composable
private fun BattleItem(
    photo: DisplayGroupAlbum,
    title: String,
    onSelect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clickable(onClick = onSelect)
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = photo.thumbnailUrl),
            contentDescription = photo.title,
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                .background(Color.White),
            contentScale = ContentScale.Crop
        )
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = photo.title, fontSize = 12.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
    }
}
