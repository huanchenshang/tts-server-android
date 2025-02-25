package com.github.jing332.tts_server_android.compose.forwarder

import android.content.IntentFilter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.jing332.compose.widgets.LocalBroadcastReceiver
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.service.forwarder.system.SysTtsForwarderService
import com.google.accompanist.web.rememberWebViewNavigator
import com.google.accompanist.web.rememberWebViewState
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
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
        topBar = {
            Column {
                topBar()
                PrimaryTabRow(selectedTabIndex = state.currentPage, tabs = {
                    pages.forEachIndexed { index, strId ->
                        val selected = state.currentPage == index
                        TextButton(onClick = {
                            scope.launch {
                                state.animateScrollToPage(index)
                            }
                        }) { Text(stringResource(id = strId)) }
                    }
                })
            }
        }) { paddingValues ->
        HorizontalPager(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            state = state,
            userScrollEnabled = true
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
        }
    }

}