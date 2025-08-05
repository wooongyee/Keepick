package com.ssafy.keepick.app

import android.app.Application
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.kakao.sdk.common.util.Utility
import com.navercorp.nid.NaverIdLoginSDK
import com.ssafy.keepick.BuildConfig
import com.ssafy.keepick.core.util.TAG
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KeepickApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_CLIENT_ID)
        NaverIdLoginSDK.initialize(
            context = this,
            clientId = BuildConfig.NAVER_CLIENT_ID,
            clientSecret = BuildConfig.NAVER_CLIENT_SECRET,
            clientName = "keepick"
        )

        val keyHash = Utility.getKeyHash(this)
        Log.d(TAG, "Key Hash: $keyHash")
    }
}
