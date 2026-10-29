package com.ssafy.keepick.feature.gallery.components

import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoPickerBottomSheet(
    localImageUris: List<Uri>,
    onDismissRequest: () -> Unit,
    onImagesSelected: (List<Uri>) -> Unit,
) {
    val selectedImageUris = remember { mutableStateListOf<Uri>() }

    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
        Text(
            "업로드할 사진 선택",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(localImageUris, key = { it }) { uri ->
                // ▼ 핵심 수정: GalleryImageItem 대신 LocalImagePickerItem 사용
                LocalImagePickerItem(
                    uri = uri,
                    isSelected = uri in selectedImageUris,
                    onClick = {
                        if (uri in selectedImageUris) {
                            selectedImageUris.remove(uri)
                        } else {
                            selectedImageUris.add(uri)
                        }
                    }
                )
            }
        }
        Button(
            onClick = {
                onImagesSelected(selectedImageUris.toList())
                onDismissRequest() // 업로드 후 BottomSheet 닫기
            },
            enabled = selectedImageUris.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("${selectedImageUris.size}개 사진 업로드")
        }
    }
}
@Composable
private fun LocalImagePickerItem(
    modifier: Modifier = Modifier,
    uri: Uri,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "로컬 이미지",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 선택됨 오버레이
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "선택됨",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )
            }
        }
    }
}
