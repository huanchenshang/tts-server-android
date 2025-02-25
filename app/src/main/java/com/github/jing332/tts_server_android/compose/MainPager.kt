package com.github.jing332.tts_server_android.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.jing332.tts_server_android.compose.forwarder.systts.SystemTtsForwarderScreen
import com.github.jing332.tts_server_android.compose.settings.SettingsScreen
import com.github.jing332.tts_server_android.compose.systts.MigrationTips
import com.github.jing332.tts_server_android.compose.systts.TtsLogScreen
import com.github.jing332.tts_server_android.compose.systts.list.ListManagerScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MainPager(sharedVM: SharedViewModel) {
    val pagerState = rememberPagerState { PagerDestination.routes.size }
    val scope = rememberCoroutineScope()

    MigrationTips()

    Scaffold(
        bottomBar = {
            NavigationBar {
                for (destination in PagerDestination.routes) {
                    val isSelected = pagerState.currentPage == destination.index
                    NavigationBarItem(
                        selected = isSelected,
                        alwaysShowLabel = false,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(destination.index)
                            }
                        },
                        icon = destination.icon,
                        label = {
                            Text(stringResource(id = destination.strId))
                        }
                    )
                }
            }


        }
    ) { paddingValues ->
        HorizontalPager(
            modifier = Modifier
                .padding(bottom = paddingValues.calculateBottomPadding())
                .fillMaxSize(),
            state = pagerState,
            userScrollEnabled = false
        ) { index ->
            when (index) {
                PagerDestination.SystemTts.index -> ListManagerScreen(sharedVM)
                PagerDestination.SystemTtsLog.index -> TtsLogScreen()
                PagerDestination.Settings.index -> SettingsScreen()
                PagerDestination.SystemTtsForwarder.index -> SystemTtsForwarderScreen()
            }
        }
    }
}