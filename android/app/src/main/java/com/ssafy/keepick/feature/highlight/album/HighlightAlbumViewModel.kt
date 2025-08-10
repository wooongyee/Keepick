package com.ssafy.keepick.feature.highlight.album

import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import com.ssafy.keepick.core.util.ShakeDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class HighlightAlbumViewModel @Inject constructor(
    // [수정] Hilt를 통해 SensorManager를 직접 주입받습니다.
    private val sensorManager: SensorManager
) : ViewModel(), ContainerHost<HighlightAlbumState, HighlightAlbumSideEffect> {

    override val container = container<HighlightAlbumState, HighlightAlbumSideEffect>(
        HighlightAlbumState()
    )

    private val shakeDetector by lazy {
        ShakeDetector { onShakeDetected() }
    }

    private fun onShakeDetected() = intent {
        // 이미 상자가 열렸거나, 흔들림이 감지된 상태면 중복 실행 방지
        if (state.isGiftBoxOpened || state.shakeDetected) return@intent
        reduce { state.copy(shakeDetected = true) }
    }

    fun onGiftBoxOpened() = intent {
        reduce { state.copy(isGiftBoxOpened = true) }
    }

    fun startShakeDetection() = intent {
        if (state.isGiftBoxOpened) return@intent // 이미 열렸으면 감지 시작 안 함
        shakeDetector.start(sensorManager)
    }

    fun stopShakeDetection() {
        shakeDetector.stop(sensorManager)
    }

    /**
     * ViewModel이 소멸될 때 호출됩니다.
     * 여기서 센서 리스너를 해제하여 메모리 누수 및 불필요한 배터리 소모를 방지합니다.
     */
    override fun onCleared() {
        super.onCleared()
        stopShakeDetection()
    }
}
