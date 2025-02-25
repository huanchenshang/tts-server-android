package com.github.jing332.tts_server_android.compose.forwarder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.github.jing332.tts_server_android.R

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun BasicForwarderScreen(
    topBar: @Composable () -> Unit,
    configScreen: @Composable () -> Unit,
    onGetUrl: () -> String,
) {
    val pages = remember { listOf(R.string.log, R.string.web) }
    val state = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = topBar,
        bottomBar = {
//            NavigationBar {
//                pages.forEachIndexed { index, strId ->
//                    val selected = state.currentPage == index
//                    NavigationBarItem(
//                        selected = selected,
//                        onClick = {
//                            scope.launch {
//                                state.animateScrollToPage(index)
//                            }
//                        },
//                        icon = {
//                            if (index == 0)
//                                Icon(Icons.Default.TextSnippet, null)
//                            else
//                                Icon(painter = painterResource(R.drawable.ic_web), null)
//                        },
//                        label = { Text(stringResource(id = strId)) }
//                    )
//                }
//            }
        }) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            configScreen()
        }

        /*
        HorizontalPager(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = state,
            userScrollEnabled = false
        ) {
            when (it) {
                0 -> configScreen()
                1 -> {
                    val webState = rememberWebViewState(url = onGetUrl())
                    val navigator = rememberWebViewNavigator()
                    LocalBroadcastReceiver(intentFilter = IntentFilter(SysTtsForwarderService.ACTION_ON_STARTING)) {
                        navigator.loadUrl(onGetUrl())
                    }

                    WebScreen(
                        modifier = Modifier.fillMaxSize(),
                        state = webState,
                        navigator = navigator
                    )
                }
            }
        }*/
    }

}