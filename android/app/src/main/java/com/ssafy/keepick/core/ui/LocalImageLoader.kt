package com.ssafy.keepick.core.ui

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 기기에 저장된 모든 이미지의 URI를 비동기적으로 불러오는 Composable 유틸리티.
 * 권한이 허용된 경우에만 이미지를 로드합니다.
 *
 * @return 로드된 이미지 URI 목록 (List<Uri>)
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberLocalImages(): List<Uri> {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(permission = storagePermission)
    val (imageUris, setImageUris) = remember { mutableStateOf<List<Uri>>(emptyList()) }

    // 권한 상태가 변경되거나, 권한이 허용되면 이미지 로드
    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            val uris = withContext(Dispatchers.IO) {
                val imageList = mutableListOf<Uri>()
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        imageList.add(contentUri)
                    }
                }
                imageList
            }
            setImageUris(uris)
        }
    }

    return imageUris
}
