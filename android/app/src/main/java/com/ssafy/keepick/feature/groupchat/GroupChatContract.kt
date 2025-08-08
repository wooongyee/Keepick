package com.ssafy.keepick.feature.groupchat

import java.util.UUID

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

enum class BottomSheetContent {
    NONE, CHAT, SETTINGS
}

data class GroupChatState(
    val isActive: Boolean = false,
    val groupId: String? = null,
    val size: GroupChatSize = GroupChatSize.HIDDEN,
    val participants: List<Participant> = emptyList(),
    val emojiEvents: Map<String, EmojiEvent> = emptyMap(),
    /**
     * [추가] 현재 표시 중인 바텀시트 콘텐츠 상태. NONE은 닫힌 상태를 의미합니다.
     */
    val bottomSheetContent: BottomSheetContent = BottomSheetContent.NONE,
) {
    val screenSharingParticipant: Participant?
        get() = participants.find { it.isScreenSharing }
}


sealed class GroupChatSideEffect {
    data class ShowToast(val message: String) : GroupChatSideEffect()
}
