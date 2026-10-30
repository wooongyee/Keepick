package com.ssafy.keepick.feature.gallery.similarphotos

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SimilarPhotosRoute(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: SimilarPhotosViewModel = hiltViewModel(),
    onNavigateToImageDetail: (sharedKey:String, imageId: String, clusterId: String?) -> Unit,
    onNavigateBack: () -> Unit
) {
    // 1. Orbit MVI 패턴에 맞게 container.stateFlow를 구독합니다.
    val uiState by viewModel.container.stateFlow.collectAsState()

    SimilarPhotosScreen(
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        state = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToImageDetail = onNavigateToImageDetail // Screen에 전달
    )
}
