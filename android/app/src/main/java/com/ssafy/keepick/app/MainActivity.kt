package com.ssafy.keepick.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ssafy.keepick.app.navigation.KeepickNavHost
import com.ssafy.keepick.core.designsystem.theme.KeepickTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeepickTheme {
                KeepickNavHost()
            }
        }
    }
}

