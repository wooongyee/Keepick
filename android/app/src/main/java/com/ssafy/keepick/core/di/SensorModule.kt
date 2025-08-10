package com.ssafy.keepick.core.di

import android.content.Context
import android.hardware.SensorManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    /**
     * SensorManager의 싱글턴 인스턴스를 제공합니다.
     * Hilt가 ApplicationContext를 주입해주면, 이를 사용해 시스템 서비스를 가져옵니다.
     */
    @Provides
    @Singleton
    fun provideSensorManager(@ApplicationContext context: Context): SensorManager {
        return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
}
