package com.ssafy.keepick.feature.mypage.components

// --- [핵심] GalleryLauncher.kt에서 가져온 유틸리티 import ---
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ssafy.keepick.core.designsystem.theme.KeepickTheme
import com.ssafy.keepick.core.ui.rememberGalleryLauncher
import com.ssafy.keepick.feature.mypage.DisplayProfileSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileBottomSheet(
    profile: DisplayProfileSummary,
    onDismissRequest: () -> Unit,
    onSaveClick: (name: String, newImageUri: Uri?) -> Unit // 타입을 Uri?로 변경
) {
    var name by remember(profile.name) { mutableStateOf(profile.name) }
    // 갤러리에서 선택된 이미지 URI를 저장할 상태 (타입을 Uri?로 변경)
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- [핵심] 권한 처리 로직이 포함된 갤러리 런처 생성 ---
    val galleryLauncher = rememberGalleryLauncher { uri ->
        selectedImageUri = uri
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("프로필 수정", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            // 프로필 사진 수정 영역
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .clickable { galleryLauncher() }, // 클릭 시 권한 처리 후 갤러리 실행
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    // 선택된 이미지가 있으면 그것을, 없으면 기존 프로필 이미지를 표시
                    model = selectedImageUri ?: profile.imageUrl,
                    contentDescription = "프로필 사진",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // 카메라 아이콘 오버레이
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "사진 변경",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("이름") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 저장 버튼
            Button(
                onClick = { onSaveClick(name, selectedImageUri) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8A2BE2))
            ) {
                Text("저장하기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview
@Composable
fun EditProfileBottomSheetPreview() {
    KeepickTheme {
        Box(Modifier.fillMaxSize()) {
            EditProfileBottomSheet(
                profile = DisplayProfileSummary("김싸피", "", 4, 3, 12),
                onDismissRequest = {},
                onSaveClick = { _, _ ->}
            )
        }
    }
}
