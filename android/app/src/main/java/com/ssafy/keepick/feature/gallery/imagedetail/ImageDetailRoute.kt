package com.ssafy.keepick.feature.gallery.imagedetail

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * ImageDetailScreen으로의 진입점 역할을 하는 Route 컴포저블입니다.
 * ViewModel과 UI(Screen)를 연결합니다.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ImageDetailRoute(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedVisibilityScope,
    onNavigateBack: () -> Unit,
    viewModel: ImageDetailViewModel = hiltViewModel()
) {
    val state by viewModel.container.stateFlow.collectAsStateWithLifecycle()

    ImageDetailScreen(
        sharedTransitionScope = sharedTransitionScope,
        animatedContentScope = animatedContentScope,
        state = state,
        onNavigateBack = onNavigateBack,
        onPageChanged = viewModel::onPageChanged
    )
}
