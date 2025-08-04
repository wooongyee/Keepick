package com.ssafy.keepick.feature.home.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepickTopAppBar(
    onNotificationClick: () -> Unit = {}
) {
    TopAppBar(
        title = { Text("KeePick", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        actions = {
            IconButton(onClick = onNotificationClick) {
                Icon(Icons.Filled.Notifications, contentDescription = "알림")
            }
        },
    )
}
