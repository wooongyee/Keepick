package com.ssafy.keepick.feature.mypage

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.keepick.R
import com.ssafy.keepick.core.designsystem.theme.KeepickTheme
import com.ssafy.keepick.feature.mypage.components.EditProfileBottomSheet
import com.ssafy.keepick.feature.mypage.components.FriendListContent
import com.ssafy.keepick.feature.mypage.components.FriendRequestItem
import com.ssafy.keepick.feature.mypage.components.FriendSearchContent
import com.ssafy.keepick.feature.mypage.components.FriendSearchTextField
import com.ssafy.keepick.feature.mypage.components.ProfileSummaryCard
import com.ssafy.keepick.feature.mypage.components.SentRequestListContent

// --- [신규] 탭 종류를 정의하는 Sealed Class ---
sealed class MyPageTab(val title: String) {
    data object FriendList : MyPageTab("친구 목록")
    data object ReceivedRequests : MyPageTab("받은 요청")
    data object SentRequests : MyPageTab("보낸 요청")
}

@Composable
fun MypageScreen(
) {
    val context = LocalContext.current
    val tabs = listOf(MyPageTab.FriendList, MyPageTab.ReceivedRequests, MyPageTab.SentRequests)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showEditProfileSheet by remember { mutableStateOf(false) }

    val profile = remember(context) {
        DummyProfileSummary(
            name = "김싸피",
            imageUrl = R.drawable.dummy_profile,
            friendCount = 4,
            groupCount = 3,
            albumCount = 12
        ).toDisplay(context)
    }
    val receivedRequests = remember(context) { // 받은 요청 데이터
        listOf(
            DummyFriend("김싸피", R.drawable.dummy_profile, "공통 친구 3명 2일전").toDisplay(context),
            DummyFriend("이싸피", R.drawable.dummy_profile, "공통 친구 1명 5일전").toDisplay(context)
        )
    }
    val sentRequests = remember(context) { // 보낸 요청 데이터
        listOf(
            DummyFriend("박싸피", R.drawable.dummy_profile, "1일 전 요청").toDisplay(context),
            DummyFriend("강싸피", R.drawable.dummy_profile, "3일 전 요청").toDisplay(context)
        )
    }
    val myFriends  = remember(context) {
        listOf(
            DummyFriend("정싸피", R.drawable.dummy_profile),
            DummyFriend("최싸피", R.drawable.dummy_profile),
            DummyFriend("조싸피", R.drawable.dummy_profile),
            DummyFriend("윤싸피", R.drawable.dummy_profile),
        ).map { it.toDisplay(context) }
    }

    if (showEditProfileSheet) {
        EditProfileBottomSheet(
            profile = profile,
            onDismissRequest = { showEditProfileSheet = false },
            onSaveClick = { name, newImageUri ->
                // TODO: ViewModel을 통해 실제 프로필 업데이트 API 호출
                showEditProfileSheet = false
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9))
    ) {
        ProfileSummaryCard(
            profile = profile,
            onClick = { showEditProfileSheet = true }
        )

        TabRow(selectedTabIndex = selectedTabIndex, /* ... */) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = tab.title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // 탭에 따라 다른 콘텐츠 표시
        when (tabs[selectedTabIndex]) {
            is MyPageTab.FriendList -> FriendListContent(myFriends)
            is MyPageTab.ReceivedRequests -> ReceivedRequestContent(receivedRequests)
            is MyPageTab.SentRequests -> SentRequestContent(sentRequests)
        }
    }
}

@Composable
fun ReceivedRequestContent(requests: List<DisplayFriend>) {
    if (requests.isEmpty()) {
        EmptyContentView(message = "받은 친구 요청이 없습니다.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader(title = "받은 요청 (${requests.size})") }
        items(items = requests, key = { "received_${it.name}" }) { request ->
            FriendRequestItem(friend = request)
        }
    }
}


@Composable
fun SentRequestContent(sentRequests: List<DisplayFriend>) {
    var searchText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        FriendSearchTextField(
            searchText = searchText,
            onSearchTextChanged = { newText -> searchText = newText },
            modifier = Modifier.padding(16.dp)
        )
        if (searchText.isNotBlank()) {
            FriendSearchContent(searchText = searchText)
        } else {
            SentRequestListContent(sentRequests = sentRequests)
        }
    }
}


@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}




@Composable
fun EmptyContentView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = Color.Gray, fontSize = 16.sp)
    }
}

// --- Preview 수정 ---
@Preview(showBackground = true, name = "마이페이지 화면 미리보기")
@Composable
fun MypageScreenPreview() {
    KeepickTheme {
        MypageScreen()
    }
}

private data class DummyProfileSummary(val name: String, val imageUrl: Int, val friendCount: Int, val groupCount: Int, val albumCount: Int)
data class DummyFriend(val name: String, val imageUrl: Int, val subtitle: String? = null)

// UI에 직접 표시하기 위해 가공된 데이터 모델
data class DisplayProfileSummary(val name: String, val imageUrl: String, val friendCount: Int, val groupCount: Int, val albumCount: Int)
data class DisplayFriend(val name: String, val imageUrl: String, val subtitle: String? = null)

// 리소스 ID를 URI 문자열로 변환하는 확장 함수
private fun Int.toDrawableUri(context: Context): String {
    return Uri.parse("android.resource://${context.packageName}/$this").toString()
}

// 원본 데이터를 UI용 데이터로 변환하는 확장 함수
private fun DummyProfileSummary.toDisplay(context: Context): DisplayProfileSummary {
    return DisplayProfileSummary(
        name = this.name,
        imageUrl = this.imageUrl.toDrawableUri(context),
        friendCount = this.friendCount,
        groupCount = this.groupCount,
        albumCount = this.albumCount
    )
}

fun DummyFriend.toDisplay(context: Context): DisplayFriend {
    return DisplayFriend(
        name = this.name,
        imageUrl = this.imageUrl.toDrawableUri(context),
        subtitle = this.subtitle
    )
}
