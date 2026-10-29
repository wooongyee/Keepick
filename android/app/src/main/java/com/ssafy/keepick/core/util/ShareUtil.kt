package com.ssafy.keepick.core.util

import android.content.Context
import android.content.Intent

fun shareInviteLink(context: Context, inviteCode: String) {
    // 하드코딩된 URL 대신, 정의한 상수를 사용합니다.
    val inviteUrl = "${AppConstants.FRONTEND_URL}/invite/$inviteCode"
    val shareText = "[KeePick] 그룹에 초대합니다!\n아래 링크를 클릭하여 그룹에 참여하세요:\n$inviteUrl"

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    val chooser = Intent.createChooser(intent, "초대 링크 공유")
    context.startActivity(chooser)
}
