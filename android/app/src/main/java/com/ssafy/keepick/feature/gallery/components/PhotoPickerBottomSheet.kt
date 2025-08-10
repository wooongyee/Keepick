package com.ssafy.keepick.feature.gallery.components

import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssafy.keepick.feature.gallery.model.GalleryImage
import com.ssafy.keepick.feature.gallery.model.ImageStatus

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoPickerBottomSheet(
    localImageUris: List<Uri>, // String 대신 Uri 타입을 받도록 수정
    onDismissRequest: () -> Unit,
    onImagesSelected: (List<Uri>) -> Unit, // Uri 타입을 반환하도록 수정
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
                val dummyImage = remember(uri) {
                    GalleryImage(id = uri.toString(), imageUrl = uri.toString(), "", emptyList(), ImageStatus.COMPLETE)
                }
                GalleryImageItem(
                    image = dummyImage,
                    isSelected = uri in selectedImageUris,
                    onClick = {
                        if (uri in selectedImageUris) {
                            selectedImageUris.remove(uri)
                        } else {
                            selectedImageUris.add(uri)
                        }
                    },
                    onLongClick = {}
                )
            }
        }
        Button(
            onClick = {
                onImagesSelected(selectedImageUris.toList())
            },
            enabled = selectedImageUris.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("${selectedImageUris.size}개 사진 업로드")
        }
    }
}
