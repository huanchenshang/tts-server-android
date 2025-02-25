package com.github.jing332.compose.widgets

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dokar.sheets.BottomSheetValue
import com.dokar.sheets.m3.BottomSheet
import com.dokar.sheets.m3.BottomSheetDefaults
import com.dokar.sheets.rememberBottomSheetState


@Composable
fun AppBottomSheet(
    value: BottomSheetValue = BottomSheetValue.Peeked,
    onDismissRequest: () -> Unit = {},
    onCollapse: () -> Boolean = { true },
    content: @Composable () -> Unit,
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

    val cornerRadius by animateIntAsState(
        targetValue = if (state.value != BottomSheetValue.Peeked) 0 else 24,
        label = "cornerRadius"
    )

    val backgroundColor = BottomSheetDefaults.backgroundColor
    BottomSheet(
        modifier = Modifier.fillMaxSize(),
        state = state,
        content = content,
        behaviors = BottomSheetDefaults.dialogSheetBehaviors(
            extendsIntoStatusBar = false,
            extendsIntoNavigationBar = false,
            navigationBarColor = backgroundColor,
        ),
        shape = RoundedCornerShape(cornerRadius.dp),

        )
}