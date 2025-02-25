package com.github.jing332.compose.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun InitSystemNavigation(color: Color = MaterialTheme.colorScheme.surface) {
    val uiController = rememberSystemUiController()

    LaunchedEffect(color) {
        uiController.setNavigationBarColor(color)
    }
}