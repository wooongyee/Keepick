package com.ssafy.keepick.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    onKakaoLoginClick: () -> Unit,
    onNaverLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
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

        // 3. 배경 이미지 위에 올라갈 UI 콘텐츠
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
}


@Preview(showBackground = true, name = "Login Screen Preview")
@Composable
private fun LoginScreenPreview() {
    LoginScreen(
        onKakaoLoginClick = {},
        onNaverLoginClick = {}
    )
}
