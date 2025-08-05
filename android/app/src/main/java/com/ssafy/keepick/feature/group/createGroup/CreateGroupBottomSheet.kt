package com.ssafy.keepick.feature.group.createGroup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CreateGroupStep {
    INPUT_INFO,
    INVITE_FRIENDS
}

@Composable
fun CreateGroupBottomSheet(
    currentStep: CreateGroupStep,
    onDismissRequest: () -> Unit,
    onNavigateToPreviousStep: () -> Unit,
    onNavigateToNextStep: () -> Unit,
    onCreateGroup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
    ) {
        CreateGroupTopBar(
            currentStep = currentStep,
            onBackClick = onNavigateToPreviousStep,
            onCloseClick = onDismissRequest
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (currentStep) {
                CreateGroupStep.INPUT_INFO -> InputInfoStep()
                CreateGroupStep.INVITE_FRIENDS -> InviteFriendsStep()
            }
        }

        Button(
            onClick = {
                if (currentStep == CreateGroupStep.INPUT_INFO) {
                    onNavigateToNextStep()
                } else {
                    onCreateGroup()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(if (currentStep == CreateGroupStep.INPUT_INFO) "다음 단계" else "그룹 만들기")
        }
    }
}

@Composable
private fun CreateGroupTopBar(
    currentStep: CreateGroupStep,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp, start = 4.dp, end = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (currentStep == CreateGroupStep.INVITE_FRIENDS) {
            IconButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "이전 단계")
            }
        }
        Text("새 그룹 만들기", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onCloseClick, modifier = Modifier.align(Alignment.CenterEnd)) {
            Icon(Icons.Default.Close, contentDescription = "닫기")
        }
    }
}

