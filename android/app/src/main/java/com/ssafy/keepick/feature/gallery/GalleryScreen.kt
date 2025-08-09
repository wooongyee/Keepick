// feature/gallery/GalleryScreen.kt
package com.ssafy.keepick.feature.gallery

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.feature.gallery.components.GalleryImageItem
import com.ssafy.keepick.feature.gallery.model.GalleryImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    images: List<GalleryImage>,
    allTags: List<String>,
    selectedTag: String,
    isSelectionMode: Boolean,
    selectedImageIds: List<String>,
    onTagSelect: (String) -> Unit,
    onImageClick: (String) -> Unit,
    onImageLongClick: (String) -> Unit,
    onClearSelection: () -> Unit,
    onCreateAlbumClick: () -> Unit,
    onUploadClick: () -> Unit, // TopBar의 + 버튼과 연결될 콜백
) {
    Scaffold(
        floatingActionButton = {
            if (isSelectionMode) {
                FloatingActionButton(onClick = onCreateAlbumClick) {
                    Icon(Icons.Default.PhotoAlbum, contentDescription = "앨범 만들기")
                }
            }
        }
    ) { paddingValues ->
        if (images.isEmpty() && selectedTag == "전체") {
            EmptyGalleryContent(onUploadClick = onUploadClick, modifier = Modifier.padding(paddingValues))
        } else {
            Column(modifier = Modifier.padding(paddingValues)) {
                // 해시태그 필터 영역
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allTags) { tag ->
                        FilterChip(
                            selected = tag == selectedTag,
                            onClick = { onTagSelect(tag) },
                            label = { Text(tag) }
                        )
                    }
                }

                // 이미지 그리드 영역
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(images, key = { it.id }) { image ->
                        GalleryImageItem(
                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = spring<IntOffset>(
                                        stiffness = Spring.StiffnessMediumLow,
                                        visibilityThreshold = IntOffset.VisibilityThreshold
                                    )
                            ),
                            image = image,
                            isSelected = image.id in selectedImageIds,
                            onClick = { onImageClick(image.id) },
                            onLongClick = { onImageLongClick(image.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGalleryContent(onUploadClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AddAPhoto,
            contentDescription = "사진 없음",
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "갤러리가 비어있어요",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "첫 사진을 업로드 해보세요!",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onUploadClick) {
            Text("사진 업로드하기")
        }
    }
}
