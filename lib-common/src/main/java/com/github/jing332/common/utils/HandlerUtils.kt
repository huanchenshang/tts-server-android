@file:Suppress("unused")
/* https://github.com/gedoor/legado/blob/master/app/src/main/java/io/legado/app/utils/HandlerUtils.kt */
package com.github.jing332.common.utils

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** This main looper cache avoids synchronization overhead when accessed repeatedly. */
private val mainLooper: Looper = Looper.getMainLooper()

private val mainThread: Thread = mainLooper.thread

private val isMainThread: Boolean inline get() = mainThread === Thread.currentThread()



private val mainHandler by lazy { Handler(mainLooper) }

fun runOnUI(function: () -> Unit) {
    if (isMainThread) {
        function()
    } else {
        mainHandler.post(function)
    }
}

fun CoroutineScope.runOnIO(function: suspend () -> Unit) {
    if (isMainThread) {
        launch(IO) {
            function()
        }
    } else {
        runBlocking { function() }
    }
}