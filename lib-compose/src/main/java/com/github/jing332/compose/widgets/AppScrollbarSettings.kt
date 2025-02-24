package com.github.jing332.compose.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import my.nanihadesuka.compose.InternalLazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSettings


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppLazyColumnScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
    barModifier: Modifier = Modifier,
    settings: ScrollbarSettings = ScrollbarSettings(
        thumbSelectedColor = MaterialTheme.colorScheme.primary,
        thumbUnselectedColor = MaterialTheme.colorScheme.secondary,
        thumbMinLength = 0.5f
    ),
    content: @Composable () -> Unit,
) {

    if (!settings.enabled) content()
    else Box(modifier) {
        content()
        InternalLazyColumnScrollbar(
            state = state,
            settings = settings,
            modifier = barModifier
        )
    }
}