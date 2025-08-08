package com.ssafy.keepick.feature.groupchat.state

import java.util.UUID

// 데이터 모델 (이전과 동일)
data class Participant(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isMe: Boolean = false,
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isScreenSharing: Boolean = false,
)

data class EmojiEvent(val emoji: String, val id: String = UUID.randomUUID().toString())

enum class GroupChatSize {
    FULLSCREEN, FLOATING, ICON, HIDDEN
}

// 1. Orbit MVI를 위한 State 정의
// 이전 GroupChatUiState의 모든 필드를 포함합니다.
data class GroupChatState(
    val isActive: Boolean = false,
    val groupId: String? = null,
    val size: GroupChatSize = GroupChatSize.HIDDEN,
    val participants: List<Participant> = emptyList(),
    val emojiEvents: Map<String, EmojiEvent> = emptyMap(),
) {
    val screenSharingParticipant: Participant?
        get() = participants.find { it.isScreenSharing }
}

// 2. Orbit MVI를 위한 SideEffect 정의
sealed class GroupChatSideEffect {
    data class ShowToast(val message: String) : GroupChatSideEffect()
    // 필요시 data class NavigateToInvite(...) : GroupChatSideEffect() 등 추가 가능
}
