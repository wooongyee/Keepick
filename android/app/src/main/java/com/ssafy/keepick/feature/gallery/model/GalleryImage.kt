package com.ssafy.keepick.feature.gallery.model

import android.content.Context
import android.net.Uri

data class GalleryImage(
    val id: String,
    val imageUrl: String,
    val date: String,
    val tags: List<String>,
    val status: ImageStatus,
)

enum class ImageStatus {
    UPLOADING, // 업로드 중 (UI에 즉시 표시되지만 아직 서버 전송 전)
    ANALYZING, // 서버에서 AI 분석 중
    COMPLETE,  // 처리 완료
}

fun Int.toDrawableUri(context: Context): String {
    return Uri.parse("android.resource://${context.packageName}/$this").toString()
}
