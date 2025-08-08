package com.ssafy.keepick.feature.groupchat

// 그룹챗 UI의 크기 상태 정의
enum class GroupChatSize {
    HIDDEN,
    ICON,       // 가장 작은 아이콘 모드
    FLOATING,   // 중간 크기 플로팅 모드
    FULLSCREEN  // 전체 화면 모드
}

// 그룹챗의 전체 상태를 나타내는 데이터 클래스
data class GroupChatUiState(
    val isActive: Boolean = false,
    val groupId: String? = null,
    val size: GroupChatSize = GroupChatSize.HIDDEN,
    // val participants: List<Participant> = emptyList(), // 참여자 정보 등 추가
)
