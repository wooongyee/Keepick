package com.ssafy.keepick.core.ui

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.lerp

@OptIn(ExperimentalSharedTransitionApi::class)
val detailScreenTransform = BoundsTransform { _, _ ->
    tween<Rect>(durationMillis = 300)
}

@OptIn(ExperimentalSharedTransitionApi::class)
val halfStartTransform_150 = BoundsTransform { initialBounds, targetBounds ->
    keyframes {
        // 1. 전체 애니메이션 시간을 300ms로 설정
        durationMillis = 150

        // 2. "가상 시작 지점"을 가로, 세로 모두 50% 지점으로 계산합니다.
        val startWidth = lerp(initialBounds.width, targetBounds.width, 0.5f)
        val startLeft = lerp(initialBounds.left, targetBounds.left, 0.5f)
        val startHeight = lerp(initialBounds.height, targetBounds.height, 0.5f)
        val startTop = lerp(initialBounds.top, targetBounds.top, 0.5f)

        // 계산된 값으로 가상 시작 지점의 Rect를 생성합니다.
        val virtualStartRect = Rect(
            left = startLeft,
            top = startTop,
            right = startLeft + startWidth,
            bottom = startTop + startHeight
        )

        // 3. 시간대별 애니메이션 상태를 정의합니다.
        //    중간 경유지 없이 가상 시작점에서 바로 최종 목적지로 이동합니다.
        virtualStartRect at 0          // 0ms: 가로, 세로 50% 지점에서 시작
        targetBounds at durationMillis // 300ms: 최종 상태로 종료
    }
}
@OptIn(ExperimentalSharedTransitionApi::class)
val flickerFreeTransform = BoundsTransform { initialBounds, targetBounds ->
    keyframes {
        durationMillis = 300

        // 가상 시작점 계산 로직은 그대로 둡니다.
        val startWidth = lerp(initialBounds.width, targetBounds.width, 0.3f)
        val startLeft = lerp(initialBounds.left, targetBounds.left, 0.3f)
        val startHeight = lerp(initialBounds.height, targetBounds.height, 0.4f)
        val startTop = lerp(initialBounds.top, targetBounds.top, 0.3f)

        val virtualStartRect = Rect(
            left = startLeft,
            top = startTop,
            right = startLeft + startWidth,
            bottom = startTop + startHeight
        )

        // 시간대별 애니메이션 상태 재정의
        // 1. 애니메이션의 실제 시작점을 initialBounds로 지정합니다.
        initialBounds at 0

        // 2. 계산된 '가상 시작점'을 아주 짧은 시간(예: 80ms) 뒤의 키프레임으로 설정합니다.
        //    이렇게 하면 initialBounds에서 virtualStartRect까지 부드럽게 연결됩니다.
        virtualStartRect at 120

        // 3. 최종 목적지로 애니메이션을 계속 진행합니다.
        targetBounds at durationMillis
    }
}
