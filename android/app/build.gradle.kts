import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val properties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

fun Properties.getAndCheck(propertyKey: String): String {
    return getProperty(propertyKey) ?: throw GradleException("'$propertyKey' not found in local.properties")
}


android {
    namespace = "com.ssafy.keepick"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ssafy.keepick"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "BASE_URL", "\"${properties.getAndCheck("BASE_URL")}\"")
        buildConfigField("String", "KAKAO_CLIENT_ID", "\"${properties.getAndCheck("KAKAO_CLIENT_ID")}\"")
        buildConfigField("String", "NAVER_CLIENT_ID", "\"${properties.getAndCheck("NAVER_CLIENT_ID")}\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"${properties.getAndCheck("NAVER_CLIENT_SECRET")}\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${properties.getAndCheck("GOOGLE_CLIENT_ID")}\"")

        manifestPlaceholders["KAKAO_CLIENT_ID"] = properties.getProperty("KAKAO_CLIENT_ID", "")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel & Coroutines
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Hilt (의존성 주입)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)

    // Orbit MVI
    implementation(libs.orbit.core)
    implementation(libs.orbit.viewmodel)
    implementation(libs.orbit.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Network (Retrofit & OkHttp)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.naver.login)
    implementation(libs.kakao.sdk.user)

    // Image Loading (Coil)
    implementation(libs.coil.compose)

    // 테스트 의존성
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Android 테스트
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)

    // 네트워크 테스트
    testImplementation(libs.okhttp.mockwebserver)

    // 디버그 구성
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
