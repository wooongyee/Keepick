package com.ssafy.keepick.core.auth.di

import android.app.Activity
import com.ssafy.keepick.core.auth.AuthManager
import com.ssafy.keepick.core.auth.KakaoAuthManager
import com.ssafy.keepick.core.auth.NaverAuthManager
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object ActivityAuthModule {

    @Provides
    @ActivityScoped
    fun provideKakaoAuthManager(activity: Activity): KakaoAuthManager = KakaoAuthManager(activity)

    @Provides
    @ActivityScoped
    fun provideNaverAuthManager(activity: Activity): NaverAuthManager = NaverAuthManager(activity)

    @Provides
    @ActivityScoped
    fun provideAuthManager(
        kakaoAuthManager: KakaoAuthManager,
        naverAuthManager: NaverAuthManager
    ): AuthManager = AuthManager(kakaoAuthManager, naverAuthManager)
}

@EntryPoint
@InstallIn(ActivityComponent::class)
interface AuthEntryPoint {
    fun getAuthManager(): AuthManager
}
