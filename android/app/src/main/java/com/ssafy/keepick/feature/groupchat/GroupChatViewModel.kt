package com.ssafy.keepick.feature.groupchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.container
import javax.inject.Inject

// 1. Orbit MVI를 위한 State와 SideEffect 정의
data class GroupChatState(
    val isActive: Boolean = false,
    val groupId: String? = null,
    val size: GroupChatSize = GroupChatSize.HIDDEN,
    // val participants: List<Participant> = emptyList(), // 추후 확장
)

sealed class GroupChatSideEffect {
    data class ShowToast(val message: String) : GroupChatSideEffect()
}
@HiltViewModel
class GroupChatViewModel @Inject constructor() :
    ViewModel(), ContainerHost<GroupChatState, GroupChatSideEffect> {

    override val container =
        viewModelScope.container<GroupChatState, GroupChatSideEffect>(GroupChatState())

    fun startGroupChat(groupId: String) = intent {
        if (state.isActive) {
            postSideEffect(GroupChatSideEffect.ShowToast("이미 다른 그룹채팅에 참여 중입니다."))
            return@intent
        }
        // ... WebRTC 연결 로직 ...
        reduce {
            state.copy(
                isActive = true,
                groupId = groupId,
                size = GroupChatSize.FULLSCREEN
            )
        }
    }

    /**
     * [개선] UI 상태를 축소하는 로직.
     * FULLSCREEN -> FLOATING
     * FLOATING -> ICON
     */
    fun minimize() = intent {
        val nextSize = when (state.size) {
            GroupChatSize.FULLSCREEN -> GroupChatSize.FLOATING
            GroupChatSize.FLOATING -> GroupChatSize.ICON
            else -> state.size // 변경 없음
        }
        reduce { state.copy(size = nextSize) }
    }

    /**
     * [개선] UI 상태를 확대하는 로직.
     * ICON -> FLOATING
     * FLOATING -> FULLSCREEN
     */
    fun expand() = intent {
        val nextSize = when (state.size) {
            GroupChatSize.ICON -> GroupChatSize.FLOATING
            GroupChatSize.FLOATING -> GroupChatSize.FULLSCREEN
            else -> state.size // 변경 없음
        }
        reduce { state.copy(size = nextSize) }
    }

    /**
     * [개선] 뒤로가기 버튼 처리를 위한 전용 로직.
     * 축소 로직과 동일하지만, ICON 상태에서 종료되는 점이 다름.
     */
    fun handleBackPress() = intent {
        when (state.size) {
            GroupChatSize.FULLSCREEN -> reduce { state.copy(size = GroupChatSize.FLOATING) }
            GroupChatSize.FLOATING -> reduce { state.copy(size = GroupChatSize.ICON) }
            GroupChatSize.ICON -> { }// 아이콘 모드에서는 종료
            else -> { /* No-op */ }
        }
    }

    fun endGroupChat() = intent {
        // ... WebRTC 연결 해제 로직 ...
        reduce { GroupChatState() } // 모든 상태 초기화
    }

    // 기존 setUiSize는 private으로 만들거나 제거해도 좋습니다.
    // private fun setUiSize(size: GroupChatSize) = intent { reduce { state.copy(size = size) } }
}
