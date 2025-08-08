package com.ssafy.keepick.feature.groupchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

@HiltViewModel
class GroupChatViewModel @Inject constructor(

) : ViewModel(), ContainerHost<GroupChatState, GroupChatSideEffect> {

    override val container =
        viewModelScope.container<GroupChatState, GroupChatSideEffect>(GroupChatState())

    fun startGroupChat(groupId: String) = intent {
        if (state.isActive && state.groupId == groupId) {
            return@intent
        }
        if (state.isActive) {
            postSideEffect(GroupChatSideEffect.ShowToast("이미 다른 그룹채팅에 참여 중입니다."))
            return@intent
        }

        // 더미 데이터
        val initialParticipants = listOf(
            Participant(id = "user_me", name = "나", isMe = true),
            Participant(name = "Jaewoong", isScreenSharing = true),
            Participant(name = "Pikachu"),
            Participant(id = "tt", name = "tt"),
            Participant(name = "t2", isScreenSharing = true),
            Participant(name = "t3"),
            Participant(name = "t5"),
        )

        reduce {
            state.copy(
                isActive = true,
                groupId = groupId,
                size = GroupChatSize.FULLSCREEN,
                participants = initialParticipants
            )
        }

        // 시뮬레이션 로직은 채팅이 시작된 후에 실행되도록 이 안으로 이동
        viewModelScope.launch {
            delay(2000)
            addNewParticipant()
            delay(2000)
            addNewParticipant()
            delay(2000)
            addNewParticipant()
        }
        viewModelScope.launch {
            delay(8000)
            triggerEmoji("user_me", "👍")
        }
    }

    private fun addNewParticipant() = intent {
        val newUser = Participant(name = "Newbie")
        reduce {
            state.copy(participants = state.participants + newUser)
        }
    }

    fun onToggleMic() = intent {
        reduce {
            state.copy(
                participants = state.participants.map { p ->
                    if (p.isMe) p.copy(isMuted = !p.isMuted) else p
                }
            )
        }
    }

    /**
     * [추가] 내 참가자 정보의 isCameraOff 상태를 토글합니다.
     */
    fun onToggleCamera() = intent {
        reduce {
            state.copy(
                participants = state.participants.map { p ->
                    if (p.isMe) p.copy(isCameraOff = !p.isCameraOff) else p
                }
            )
        }
    }


    /**
     * [수정] 채팅 UI 바텀시트를 토글합니다.
     */
    fun onShowChat() = intent {
        reduce {
            if (state.bottomSheetContent == BottomSheetContent.CHAT) {
                // 이미 채팅창이 열려있으면 닫습니다.
                state.copy(bottomSheetContent = BottomSheetContent.NONE)
            } else {
                // 다른 창이 열려있거나 닫혀있으면 채팅창을 엽니다.
                state.copy(bottomSheetContent = BottomSheetContent.CHAT)
            }
        }
    }

    /**
     * [추가] 설정 UI 바텀시트를 엽니다. (핸들 드래그를 통해 호출)
     */
    fun onShowSettings() = intent {
        reduce {
            if (state.bottomSheetContent != BottomSheetContent.SETTINGS) {
                state.copy(bottomSheetContent = BottomSheetContent.SETTINGS)
            } else {
                state.copy(bottomSheetContent = BottomSheetContent.NONE)
            }
        }
    }

    /**
     * [추가] 바텀시트를 닫습니다.
     */
    fun hideBottomSheet() = intent {
        reduce {
            state.copy(bottomSheetContent = BottomSheetContent.NONE)
        }
    }

    /**
     * [추가] 화면 공유 시작/종료 토글 (설정 UI용)
     */
    fun onToggleScreenShare() = intent {
        val me = state.participants.find { it.isMe } ?: return@intent
        val isSomeoneElseSharing = state.screenSharingParticipant != null && !me.isScreenSharing

        if (isSomeoneElseSharing) {
            postSideEffect(GroupChatSideEffect.ShowToast("다른 참여자가 화면을 공유하고 있습니다."))
            return@intent
        }

        reduce {
            state.copy(
                participants = state.participants.map {
                    if (it.isMe) it.copy(isScreenSharing = !it.isScreenSharing) else it
                }
            )
        }
    }

    private fun triggerEmoji(participantId: String, emoji: String) = intent {
        reduce {
            state.copy(emojiEvents = state.emojiEvents + (participantId to EmojiEvent(emoji)))
        }
    }

    fun onEmojiShown(participantId: String, eventId: String) = intent {
        val currentEvent = state.emojiEvents[participantId]
        if (currentEvent != null && currentEvent.id == eventId) {
            reduce { state.copy(emojiEvents = state.emojiEvents - participantId) }
        }
    }

    // --- UI 이벤트 핸들러 ---

    fun minimize() = intent {
        val nextSize = when (state.size) {
            GroupChatSize.FULLSCREEN -> GroupChatSize.FLOATING
            GroupChatSize.FLOATING -> GroupChatSize.ICON
            else -> state.size
        }
        reduce { state.copy(size = nextSize) }
    }

    fun expand() = intent {
        val nextSize = when (state.size) {
            GroupChatSize.ICON -> GroupChatSize.FLOATING
            GroupChatSize.FLOATING -> GroupChatSize.FULLSCREEN
            else -> state.size
        }
        reduce { state.copy(size = nextSize) }
    }

    fun handleBackPress() = intent {
        if (state.size == GroupChatSize.FULLSCREEN) {
            reduce { state.copy(size = GroupChatSize.FLOATING) }
        }
    }

    fun endGroupChat() = intent {
        // ... WebRTC 연결 해제 등 로직 ...
        reduce { GroupChatState() } // 상태를 초기값으로 리셋
    }

    fun onInvite() = intent {
        postSideEffect(GroupChatSideEffect.ShowToast("초대 기능은 준비 중입니다."))
    }
}
