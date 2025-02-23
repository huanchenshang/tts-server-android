package com.github.jing332.compose.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppLazyColumnScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    settings: ScrollbarSettings = ScrollbarSettings(
        thumbSelectedColor = MaterialTheme.colorScheme.primary,
        thumbUnselectedColor = MaterialTheme.colorScheme.secondary,
    ),
    content: @Composable () -> Unit,
) {
    LazyColumnScrollbar(state, modifier = modifier, settings = settings, content = content)
}