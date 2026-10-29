package com.ssafy.keepick.feature.timeline.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ssafy.keepick.feature.timeline.TimelineMoment

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return (1 - fraction) * start + fraction * stop
}

private fun lerp(start: Dp, stop: Dp, fraction: Float): Dp {
    return Dp(lerp(start.value, stop.value, fraction))
}

@Composable
fun TimelineMomentItem(
    moment: TimelineMoment,
    isExpanded: Boolean,
    initialBounds: Rect,
    onRegisterBounds: (Rect) -> Unit,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isInEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onSaveClick: () -> Unit,
    isAiSuggesting: Boolean,
    onAiSuggestClick: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    bottomContentPadding: Int,
    onPhotoIdDropped: (String, Int) -> Unit
) {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalContext.current.resources.displayMetrics.widthPixels }
    val screenHeightPx = with(density) { LocalContext.current.resources.displayMetrics.heightPixels }
    val animProgress = remember { Animatable(if (isExpanded) 1f else 0f) }

    LaunchedEffect(isExpanded) {
        animProgress.animateTo(
            targetValue = if (isExpanded) 1f else 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
    }

    val currentRect = remember(animProgress.value, initialBounds) {
        val x = lerp(initialBounds.left, 0f, animProgress.value)
        val y = lerp(initialBounds.top, 0f, animProgress.value)
        val width = lerp(initialBounds.width, screenWidthPx.toFloat(), animProgress.value)
        val height = lerp(initialBounds.height, screenHeightPx.toFloat(), animProgress.value)
        Rect(x, y, x + width, y + height)
    }

    val cornerRadius = lerp(24.dp, 0.dp, animProgress.value)

    Box(
        modifier = modifier
            .onGloballyPositioned { onRegisterBounds(it.boundsInWindow()) }
            .graphicsLayer {
                translationX = currentRect.left
                translationY = currentRect.top
            }
            .size(
                width = with(density) { currentRect.width.toDp() },
                height = with(density) { currentRect.height.toDp() }
            )
            .clip(RoundedCornerShape(cornerRadius))
    ) {
        TimelineMomentPage(
            moment = moment,
            isInEditMode = isInEditMode,
            onToggleEditMode = onToggleEditMode,
            onSaveClick = onSaveClick,
            onAiSuggestClick = onAiSuggestClick,
            onTitleChanged = onTitleChanged,
            onDescriptionChanged = onDescriptionChanged,
            isAiSuggesting = isAiSuggesting,
            onDismiss = onDismiss,
            bottomContentPadding = bottomContentPadding,
            onPhotoIdDropped = onPhotoIdDropped,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = animProgress.value }
        )
    }
}
