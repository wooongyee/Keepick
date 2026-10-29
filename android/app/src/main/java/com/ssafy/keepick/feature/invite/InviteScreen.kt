// /feature/invite/InviteScreen.kt

package com.ssafy.keepick.feature.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// 1. ViewModel에서 분리된 UI 상태 정의
sealed class InviteUiState {
    data object Loading : InviteUiState()
    data class Success(val groupName: String) : InviteUiState()
    data class Error(val message: String) : InviteUiState()
}

@Composable
fun InviteRoute(
    inviteCode: String?,
    onNavigateToMain: () -> Unit
) {
    // 2. ViewModel 대신 Composable 내부에서 UI 상태를 직접 관리
    var uiState by remember { mutableStateOf<InviteUiState>(InviteUiState.Loading) }

    // 3. 재시도 로직을 트리거하기 위한 상태 변수
    var retryTrigger by remember { mutableStateOf(0) }

    // 4. 이 화면이 처음 그려지거나, 재시도 시 API 호출을 시뮬레이션
    LaunchedEffect(inviteCode, retryTrigger) {
        if (inviteCode == null) {
            uiState = InviteUiState.Error("초대 코드가 올바르지 않습니다.")
            return@LaunchedEffect
        }

        uiState = InviteUiState.Loading
        delay(1500) // 네트워크 통신 시간 흉내

        // --- 더미 데이터 기반의 성공/실패 처리 로직 ---
        when (inviteCode) {
            "valid-code" -> { // 성공 케이스 테스트용 코드
                uiState = InviteUiState.Success("KeePick 개발팀")
            }
            "error-code" -> { // 서버 에러 케이스 테스트용 코드
                uiState = InviteUiState.Error("서버에서 오류가 발생했습니다. 잠시 후 다시 시도해주세요.")
            }
            else -> { // 유효하지 않은 코드 케이스
                uiState = InviteUiState.Error("존재하지 않거나 만료된 초대 코드입니다.")
            }
        }
    }

    // 5. 계산된 UI 상태를 기반으로 실제 화면을 렌더링
    InviteScreen(
        uiState = uiState,
        onConfirm = onNavigateToMain,
        onRetry = {
            // 재시도 트리거 값을 변경하여 LaunchedEffect를 다시 실행시킴
            retryTrigger++
        }
    )
}

@Composable
private fun InviteScreen(
    uiState: InviteUiState,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // uiState에 따라 분기하여 다른 UI를 렌더링합니다.
        when (uiState) {
            is InviteUiState.Loading -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("초대를 확인하고 있습니다...")
            }

            is InviteUiState.Success -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "성공 아이콘",
                    tint = Color(0xFF4CAF50), // 초록색
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "'${uiState.groupName}' 그룹에\n성공적으로 참여했습니다!",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onConfirm) {
                    Text("그룹으로 이동")
                }
            }

            is InviteUiState.Error -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "에러 아이콘",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "초대를 수락하지 못했습니다",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRetry) {
                        Text("다시 시도")
                    }
                    Button(onClick = onConfirm) {
                        Text("메인 화면으로")
                    }
                }
            }
        }
    }
}
