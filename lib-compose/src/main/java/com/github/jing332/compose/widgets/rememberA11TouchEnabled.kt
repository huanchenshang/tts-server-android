package com.github.jing332.compose.widgets

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberA11TouchEnabled(): Boolean {
    val context = LocalContext.current
    val accessibilityManager = remember {
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    }

    var allyTouchEnabled by remember { mutableStateOf(accessibilityManager.isTouchExplorationEnabled) }

    LaunchedEffect(Unit) {
        accessibilityManager.addTouchExplorationStateChangeListener(object :
            AccessibilityManager.TouchExplorationStateChangeListener {
            override fun onTouchExplorationStateChanged(enabled: Boolean) {
                allyTouchEnabled = enabled
            }
        })
    }

    return allyTouchEnabled
}