package com.github.jing332.compose.widgets

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.dokar.sheets.BottomSheetValue
import com.dokar.sheets.m3.BottomSheet
import com.dokar.sheets.rememberBottomSheetState

@Composable
fun AppBottomSheet(
    value: BottomSheetValue = BottomSheetValue.Peeked,
    onValueChange: (BottomSheetValue) -> Boolean,
    content: @Composable () -> Unit,
) {
    val state = rememberBottomSheetState(confirmValueChange = onValueChange)
    LaunchedEffect(value) {
        when (value) {
            BottomSheetValue.Collapsed -> if (state.value != BottomSheetValue.Collapsed) state.collapse()
            BottomSheetValue.Expanded -> if (state.value != BottomSheetValue.Expanded) state.expand()
            BottomSheetValue.Peeked -> if (state.value != BottomSheetValue.Peeked) state.peek()
        }
    }
    BottomSheet(
        modifier = Modifier.fillMaxSize(),
        state = state,
        content = content,
    )
}

@Composable
fun AppBottomSheet(
    value: BottomSheetValue = BottomSheetValue.Peeked,
    onDismissRequest: () -> Unit = {},
    onCollapse: () -> Boolean = { true },
    content: @Composable () -> Unit
) {
    val state = rememberBottomSheetState(confirmValueChange = {
        if (it == BottomSheetValue.Collapsed) {
            onDismissRequest()
            return@rememberBottomSheetState onCollapse()
        }

        true
    })
    LaunchedEffect(value) {
        when (value) {
            BottomSheetValue.Collapsed -> state.collapse()
            BottomSheetValue.Expanded -> state.expand()
            BottomSheetValue.Peeked -> state.peek()
        }
    }
    BottomSheet(
        modifier = Modifier.fillMaxSize(),
        state = state,
        content = content,
    )
}