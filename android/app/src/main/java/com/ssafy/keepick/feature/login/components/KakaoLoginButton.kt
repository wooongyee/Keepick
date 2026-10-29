package com.ssafy.keepick.feature.login.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.R // 실제 프로젝트의 R 클래스 경로에 맞게 수정 필요

/**
 * 카카오 로그인 공식 디자인 가이드 문서를 기반으로 제작된 Composable 버튼.
 * '완성형'("카카오 로그인" 텍스트)만 지원합니다.
 *
 * @param onClick 버튼 클릭 시 실행될 람다
 * @param modifier Modifier
 * @param iconResId 카카오 심볼(로고)의 Vector Drawable 리소스 ID
 */
@Composable
fun KakaoLoginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconResId: Int
) {
    // 가이드라인 색상 정의
    val containerColor = Color(0xFFFEE500)
    val symbolColor = Color(0xFF000000)
    // 레이블 색상은 검정색의 85% 투명도
    val labelColor = Color(0xFF000000).copy(alpha = 0.85f)

    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp), // 표준 버튼 높이
        shape = RoundedCornerShape(12.dp), // 표준 모서리 둥글기
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = labelColor // 기본 텍스트 색상
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
    ) {
        // 아이콘 (심볼)
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = "카카오 로그인 심볼",
            modifier = Modifier.size(24.dp),
            tint = symbolColor // 심볼 색상은 가이드에 따라 #000000
        )

        // 텍스트 (레이블) - "카카오 로그인"으로 고정
        Text(
            text = "카카오 로그인",
            modifier = Modifier.weight(1f), // 아이콘을 제외한 나머지 공간을 모두 차지
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = labelColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

// --- 미리보기(Preview)를 위한 코드 ---
@Preview(showBackground = true, name = "카카오 로그인 버튼")
@Composable
private fun KakaoLoginButtonPreview() {
    KakaoLoginButton(
        onClick = {},
        iconResId = R.drawable.kakao_logo // 미리보기용 아이콘 (실제 리소스 ID로 교체)
    )
}
