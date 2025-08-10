package com.ssafy.keepick.feature.gallery.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ssafy.keepick.feature.gallery.model.GalleryImage
import com.ssafy.keepick.feature.gallery.model.ImageStatus

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryImageItem(
    keyPrefix : String,
    modifier: Modifier = Modifier,
    image: GalleryImage,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val uniqueKey = "${keyPrefix}_${image.id}"
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // --- 히어로 애니메이션 적용을 조건부로 처리 ---
        val imageModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            // 스코프가 제공된 경우에만 sharedElement 적용
            with(sharedTransitionScope) {
                Modifier
                    .fillMaxSize()
                    .sharedElement(
                        state = rememberSharedContentState(key = uniqueKey),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            }
        } else {
            // 스코프가 없으면 기본 Modifier 사용
            Modifier.fillMaxSize()
        }

        AsyncImage(
            model = image.imageUrl,
            contentDescription = "갤러리 이미지",
            contentScale = ContentScale.Crop,
            modifier = imageModifier
        )

        // 이미지 상태(업로드 중, 분석 중) 오버레이
        when (image.status) {
            ImageStatus.UPLOADING, ImageStatus.ANALYZING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                }
            }
            else -> { /* COMPLETE 상태에서는 아무것도 표시하지 않음 */ }
        }

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

@Composable
private fun BoxScope.UploadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = "업로드 중",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text("업로드 중...", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun BoxScope.AnalyzingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color.White)
    }
}
