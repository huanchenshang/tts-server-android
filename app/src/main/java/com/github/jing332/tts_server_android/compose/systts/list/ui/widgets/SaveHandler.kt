package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

internal val LocalSaveCallBack =
    compositionLocalOf<MutableList<SaveCallBack>> { error("No save callbacks") }

internal fun interface SaveCallBack {
    suspend fun onSave(): Boolean
}

@Composable
internal fun rememberSaveCallBacks() = remember { mutableListOf<SaveCallBack>() }

@Composable
internal fun SaveActionHandler(cb: SaveCallBack) {
    val cbs = LocalSaveCallBack.current
    DisposableEffect(Unit) {
        cbs.add(cb)
        onDispose {
            cbs.remove(cb)
        }
    }
}