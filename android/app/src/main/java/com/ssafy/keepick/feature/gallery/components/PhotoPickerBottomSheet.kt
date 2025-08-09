// feature/gallery/components/PhotoPickerBottomSheet.kt (수정)
package com.ssafy.keepick.feature.gallery.components

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

@Composable
fun PhotoPickerBottomSheet(
    localImageUrls: List<String>, // 로컬 이미지 URI 리스트를 직접 받음
    onDismissRequest: () -> Unit,
    onImagesSelected: (List<String>) -> Unit,
) {
    val selectedImageUrls = remember { mutableStateListOf<String>() }

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
            items(localImageUrls, key = { it }) { imageUrl ->
                // GalleryImageItem은 imageUrl만으로도 렌더링 가능하도록 더미 객체 생성
                val dummyImage = remember(imageUrl) {
                    GalleryImage(id = imageUrl, imageUrl = imageUrl, "", emptyList(), ImageStatus.COMPLETE)
                }
                GalleryImageItem(
                    image = dummyImage,
                    isSelected = imageUrl in selectedImageUrls,
                    onClick = {
                        if (imageUrl in selectedImageUrls) {
                            selectedImageUrls.remove(imageUrl)
                        } else {
                            selectedImageUrls.add(imageUrl)
                        }
                    },
                    onLongClick = {}
                )
            }
        }
        Button(
            onClick = {
                onImagesSelected(selectedImageUrls.toList())
                onDismissRequest()
            },
            enabled = selectedImageUrls.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("${selectedImageUrls.size}개 사진 업로드")
        }
    }
}
