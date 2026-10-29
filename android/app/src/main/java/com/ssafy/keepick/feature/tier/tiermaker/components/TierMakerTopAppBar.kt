package com.ssafy.keepick.feature.tier.tiermaker.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TierMakerTopAppBar(
    title: String,
    isBattleMode: Boolean,
    onNavigateBack: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onBattleModeToggle: (Boolean) -> Unit
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "뒤로가기"
                )
            }
        },
        actions = {
            IconToggleButton(checked = isBattleMode, onCheckedChange = onBattleModeToggle) {
                Icon(
                    Icons.Default.Bolt,
                    "정밀 배틀",
                    tint = if (isBattleMode) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
            IconButton(onClick = onShareClick) { Icon(Icons.Default.IosShare, "공유") }
            IconButton(onClick = onSaveClick) { Icon(Icons.Default.BookmarkBorder, "저장") }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}
