package com.ssafy.keepick.feature.tier.tiermaker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
internal fun InsertionIndicator() {
    Box(
        modifier = Modifier
            .width(3.dp)
            .fillMaxHeight(0.8f)
            .background(
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(1.5.dp)
            )
    )
}


