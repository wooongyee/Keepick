
package com.ssafy.keepick.feature.highlight.album

// 화면의 상태를 정의합니다.
data class HighlightAlbumState(
    val shakeDetected: Boolean = false,
    val isGiftBoxOpened: Boolean = false
)

// 사이드 이펙트를 정의합니다. (현재는 사용하지 않음)
sealed class HighlightAlbumSideEffect
