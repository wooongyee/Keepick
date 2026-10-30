package com.ssafy.keepick.core.network.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.ssafy.keepick.BuildConfig
import com.ssafy.keepick.core.auth.TokenManager
import com.ssafy.keepick.core.network.interceptor.AuthInterceptor
import com.ssafy.keepick.core.network.util.NullOnEmptyConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val baseUrl = BuildConfig.BASE_URL.toHttpUrl()
    private val json = Json { ignoreUnknownKeys = true }
    private val contentType = "application/json".toMediaType()

    @Singleton
    @Provides
    @Named("default")
    fun provideDefaultOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            this.setLevel(HttpLoggingInterceptor.Level.BODY)
        }
        return OkHttpClient.Builder()
            .connectTimeout(5000, TimeUnit.MILLISECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Singleton
    @Provides
    @Named("default")
    fun provideDefaultRetrofit(
        @Named("default") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(NullOnEmptyConverterFactory)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    // --- 인증 헤더 추가 클라이언트 ---
    @Singleton
    @Provides
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    @Singleton
    @Provides
    @Named("authorized")
    fun provideAuthorizedOkHttpClient(
        @Named("default") okHttpClient: OkHttpClient, // 기본 설정 재사용
        authInterceptor: AuthInterceptor
    ): OkHttpClient {
        return okHttpClient.newBuilder()
            .addInterceptor(authInterceptor)
            .build()
    }

    @Singleton
    @Provides
    @Named("authorized")
    fun provideAuthorizedRetrofit(
        @Named("authorized") okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(NullOnEmptyConverterFactory)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
}
