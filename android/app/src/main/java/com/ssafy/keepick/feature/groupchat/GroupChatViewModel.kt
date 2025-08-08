package com.ssafy.keepick.feature.groupchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.keepick.feature.groupchat.state.EmojiEvent
import com.ssafy.keepick.feature.groupchat.state.GroupChatSideEffect
import com.ssafy.keepick.feature.groupchat.state.GroupChatSize
import com.ssafy.keepick.feature.groupchat.state.GroupChatState
import com.ssafy.keepick.feature.groupchat.state.Participant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

@HiltViewModel
class GroupChatViewModel @Inject constructor() :
    ViewModel(), ContainerHost<GroupChatState, GroupChatSideEffect> {

    // Orbit 컨테이너 설정
    override val container =viewModelScope.container<GroupChatState, GroupChatSideEffect>(GroupChatState())

    fun startGroupChat(groupId: String) = intent {
        if (state.isActive && state.groupId == groupId) {
            // 이미 같은 채팅방에 참여 중이면 아무것도 하지 않음
            return@intent
        }
        if (state.isActive) {
            postSideEffect(GroupChatSideEffect.ShowToast("이미 다른 그룹채팅에 참여 중입니다."))
            return@intent
        }

        // 실제 앱에서는 이 groupId를 사용해 서버로부터 참가자 목록 등을 가져옵니다.
        val initialParticipants = listOf(
            Participant(id = "user_me", name = "나", isMe = true),
            Participant(name = "Jaewoong", isScreenSharing = true),
            Participant(name = "Pikachu"),
            Participant(id = "tt", name = "tt"),
            Participant(name = "t2", isScreenSharing = true),
            Participant(name = "t3"),
        )

        reduce {
            state.copy(
                isActive = true,
                groupId = groupId, // <<< 전달받은 groupId를 상태에 저장
                size = GroupChatSize.FULLSCREEN,
                participants = initialParticipants
            )
        }

        // 시뮬레이션 로직은 채팅이 시작된 후에 실행되도록 이 안으로 이동
        viewModelScope.launch {
            delay(5000)
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
     * [추가] 채팅 UI를 보여주기 위한 이벤트. (현재는 Toast 메시지로 대체)
     */
    fun onShowChat() = intent {
        postSideEffect(GroupChatSideEffect.ShowToast("채팅 기능은 준비 중입니다."))
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
