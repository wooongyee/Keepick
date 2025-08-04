package com.ssafy.keepick.app

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.navercorp.nid.NaverIdLoginSDK
import com.ssafy.keepick.BuildConfig
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
            clientName = "Keepick"
        )
    }
}
