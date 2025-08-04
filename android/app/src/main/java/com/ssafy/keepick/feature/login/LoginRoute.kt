package com.ssafy.keepick.feature.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginRoute(
    onNavigateToHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "로그인 화면",
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onNavigateToHome) {
            Text("홈으로 이동")
        }
    }
}
