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
import com.ssafy.keepick.R
import com.ssafy.keepick.core.designsystem.theme.NaverGreen

/**
 * 네이버 로그인 공식 디자인 가이드 문서를 기반으로 제작된 Composable 버튼.
 *
 * @param onClick 버튼 클릭 시 실행될 람다
 * @param modifier Modifier
 * @param iconResId 네이버 'N' 로고의 Vector Drawable 리소스 ID
 */
@Composable
fun NaverLoginButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconResId: Int
) {
    // 가이드라인 색상 정의
    val containerColor = NaverGreen // 네이버 그린
    val contentColor = Color.White       // 심볼 및 레이블 색상

    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp), // 모서리 둥글기는 서비스에 맞게 조절 가능
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
    ) {
        // 아이콘 (N 로고)
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = "네이버 로그인 로고",
            modifier = Modifier.size(24.dp),
            tint = contentColor // 네이버 로고는 배경색에 따라 흰색 또는 검은색
        )

        // 텍스트 (레이블)
        Text(
            text = "네이버 로그인", // 가이드에 따라 텍스트 변경 가능
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Preview(showBackground = true, name = "네이버 로그인 버튼")
@Composable
private fun NaverLoginButtonPreview() {
    NaverLoginButton(
        onClick = {},
        iconResId = R.drawable.naver_logo // 미리보기용 아이콘 (실제 리소스 ID로 교체)
    )
}

