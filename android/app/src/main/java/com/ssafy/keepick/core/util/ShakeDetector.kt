package com.ssafy.keepick.core.util

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.pow
import kotlin.math.sqrt

class ShakeDetector(
    private val onShake: () -> Unit // 흔들림이 감지됐을 때 호출할 콜백 함수
) : SensorEventListener {

    private var lastUpdate: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f

    companion object {
        // 이 값보다 움직임 속도가 빠르면 '흔들림'으로 간주합니다. 기기에 따라 조정이 필요할 수 있습니다.
        private const val SHAKE_THRESHOLD_SPEED = 1000
        // 이 시간(밀리초) 간격으로 센서 값을 확인합니다.
        private const val UPDATE_INTERVAL_MS = 200
    }

    /**
     * 가속도 센서 리스너를 등록하고 감지를 시작합니다.
     */
    fun start(sensorManager: SensorManager) {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            lastUpdate = System.currentTimeMillis()
        }
    }

    /**
     * 센서 리스너를 해제하여 배터리 소모를 방지합니다.
     */
    fun stop(sensorManager: SensorManager) {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // 가속도 센서 이벤트가 아니면 무시
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastUpdate) > UPDATE_INTERVAL_MS) {
            val diffTime = (currentTime - lastUpdate)
            lastUpdate = currentTime

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // 이전 값과 현재 값의 차이를 이용해 속도를 계산합니다.
            val speed = sqrt((x - lastX).pow(2) + (y - lastY).pow(2) + (z - lastZ).pow(2)) / diffTime * 10000

            if (speed > SHAKE_THRESHOLD_SPEED) {
                // 흔들림이 감지되면 콜백 함수를 호출합니다.
                onShake()
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 정확도 변경은 사용하지 않으므로 비워둡니다.
    }
}
