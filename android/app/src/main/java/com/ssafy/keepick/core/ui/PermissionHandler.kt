package com.ssafy.keepick.core.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * 안드로이드 버전에 맞는 이미지 읽기 권한을 반환하는 유틸리티
 */
private val storagePermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/**
 * 메인 화면 등에서 진입 시 갤러리 권한을 요청하는 Composable.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestStoragePermissionOnStart() {
    val permissionState = rememberPermissionState(permission = storagePermission)

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }
}

/**
 * 권한이 거부되었을 때 표시할 다이얼로그.
 * @param onConfirm 설정 화면으로 이동하는 버튼 클릭 시 호출
 * @param onDismiss 다이얼로그를 닫을 때 호출
 */
@Composable
fun PermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("권한 필요") },
        text = { Text("그룹 프로필 이미지를 설정하려면, 갤러리 접근 권한이 필요합니다. 설정 화면에서 권한을 허용해주세요.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("설정으로 이동")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

/**
 * 앱 설정 화면을 여는 함수.
 */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

/**
 * 이미지 선택을 위한 Photo Picker 런처를 생성하고,
 * 권한 상태에 따라 갤러리를 열거나 권한 요청 다이얼로그를 표시하는 로직을 포함하는 유틸리티 Composable.
 *
 * @param onImageSelected 이미지가 선택되었을 때 URI를 반환하는 콜백.
 * @return 갤러리를 실행하는 람다 함수 `() -> Unit`.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberGalleryLauncher(onImageSelected: (Uri?) -> Unit): () -> Unit {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionState = rememberPermissionState(permission = storagePermission)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = onImageSelected
    )

    if (showPermissionDialog) {
        PermissionRationaleDialog(
            onConfirm = {
                openAppSettings(context)
                showPermissionDialog = false
            },
            onDismiss = { showPermissionDialog = false }
        )
    }

    return {
        when {
            // 권한이 이미 허용된 경우
            permissionState.status.isGranted -> {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            // 권한이 거부되었지만, 시스템 권한 요청 창을 다시 띄울 수 있는 경우
            permissionState.status.shouldShowRationale -> {
                permissionState.launchPermissionRequest()
            }
            // 권한이 영구적으로 거부된 경우
            else -> {
                showPermissionDialog = true
            }
        }
    }
}

