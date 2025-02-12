package com.github.jing332.tts_server_android.compose.systts

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.github.jing332.common.LogEntry
import com.github.jing332.common.toArgb
import com.github.jing332.common.utils.ClipboardUtils
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.ComposeExtensions.toAnnotatedString
import com.github.jing332.tts_server_android.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogScreen(
    modifier: Modifier,
    list: List<LogEntry>,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val context = LocalContext.current
    Box(modifier) {
        val isAtBottom by remember {
            derivedStateOf {
                val layoutInfo = lazyListState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (layoutInfo.totalItemsCount <= 0) {
                    true
                } else {
                    val lastVisibleItem = visibleItemsInfo.last()
                    lastVisibleItem.index > layoutInfo.totalItemsCount - 5
                }
            }
        }

        LaunchedEffect(list.size) {
            if (isAtBottom && list.isNotEmpty())
                scope.launch {
                    lazyListState.animateScrollToItem(list.size - 1)
                }
        }

        if (list.isEmpty())
            Box(Modifier.align(Alignment.Center)) {
                Text(text = stringResource(R.string.empty_list), style = MaterialTheme.typography.displaySmall)
            }

        val darkTheme = isSystemInDarkTheme()
        LazyColumn(Modifier.fillMaxSize(), state = lazyListState) {
            itemsIndexed(list, key = { index, _ -> index }) { _, log ->
                val style = MaterialTheme.typography.bodyMedium
                val spanned = remember {
                    HtmlCompat.fromHtml(log.message, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        .toAnnotatedString()
                }

                SelectionContainer {
                    Text(
                        text = spanned,
                        color = Color(log.level.toArgb(isDarkTheme = darkTheme)),
                        style = style,
                        lineHeight = style.lineHeight * 0.75f,
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {
                                    println("onClick")
                                },
                                onLongClick = {
                                    view.isHapticFeedbackEnabled = true
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    ClipboardUtils.copyText("tts-server-log", spanned.text)
                                    context.toast(R.string.copied)
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(48.dp), visible = !isAtBottom
        ) {
            FloatingActionButton(
                shape = CircleShape,
                onClick = {
                    scope.launch {
                        kotlin.runCatching {
                            lazyListState.scrollToItem(list.size - 1)
                        }
                    }
                }) {
                Icon(
                    Icons.Default.KeyboardDoubleArrowDown,
                    stringResource(id = R.string.move_to_bottom)
                )
            }
        }
    }
}