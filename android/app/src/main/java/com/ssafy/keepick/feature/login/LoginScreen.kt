// LoginScreen.kt
package com.ssafy.keepick.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ssafy.keepick.R
import com.ssafy.keepick.feature.login.components.KakaoLoginButton
import com.ssafy.keepick.feature.login.components.NaverLoginButton

@Composable
fun LoginScreen(
    isLoading: Boolean,
    onKakaoLoginClick: () -> Unit,
    onNaverLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center // 로딩 인디케이터를 중앙에 배치하기 위해 변경
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                painter = painterResource(id = R.drawable.login_temp),
                contentDescription = "로그인 배경 이미지",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .padding(top = 120.dp)
                    .fillMaxWidth()
                    .scale(1.8f)
                    .rotate(-30f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "KeePick",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(40.dp))

                KakaoLoginButton(
                    onClick = onKakaoLoginClick,
                    modifier = Modifier.fillMaxWidth(),
                    iconResId = R.drawable.kakao_logo
                )
                Spacer(modifier = Modifier.height(12.dp))
                NaverLoginButton(
                    onClick = onNaverLoginClick,
                    modifier = Modifier.fillMaxWidth(),
                    iconResId = R.drawable.naver_logo
                )

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // 2. isLoading이 true일 때만 로딩 인디케이터를 보여줌
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp)
            )
        }
    }
}


@Preview(showBackground = true, name = "Login Screen Preview (Idle)")
@Composable
private fun LoginScreenIdlePreview() {
    LoginScreen(
        isLoading = false,
        onKakaoLoginClick = {},
        onNaverLoginClick = {}
    )
}

@Preview(showBackground = true, name = "Login Screen Preview (Loading)")
@Composable
private fun LoginScreenLoadingPreview() {
    LoginScreen(
        isLoading = true,
        onKakaoLoginClick = {},
        onNaverLoginClick = {}
    )
}
