package com.ssafy.keepick.feature.groupchat

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * GroupChatViewModel을 제공하기 위한 CompositionLocal.
 *
 * staticCompositionLocalOf: 제공되는 값이 거의 변경되지 않을 것으로 예상될 때 사용합니다.
 * 이 경우 ViewModel 인스턴스 자체는 변경되지 않으므로 성능상 이점이 있습니다.
 * 만약 값이 자주 바뀐다면 compositionLocalOf를 사용해야 합니다.
 *
 * 기본값을 제공하지 않고 에러를 발생시키도록 하여, Provider가 반드시 설정되도록 강제합니다.
 */
val LocalGroupChatViewModel = staticCompositionLocalOf<GroupChatViewModel> {
    error("No GroupChatViewModel provided")
}
