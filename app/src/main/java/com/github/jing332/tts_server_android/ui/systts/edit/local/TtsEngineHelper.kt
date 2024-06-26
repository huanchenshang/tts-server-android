package com.github.jing332.tts_server_android.ui.systts.edit.local

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.*

class TtsEngineHelper(val context: Context, val scope: CoroutineScope) {
    companion object {
        private const val INIT_STATUS_WAITING = -2
    }

    private var tts: TextToSpeech? = null


    /**
     * @return 是否成功
     */
    suspend fun setEngine(name: String): Boolean = coroutineScope {
        shutdown()

        var status = INIT_STATUS_WAITING
        tts = TextToSpeech(context, { status = it }, name)

        while (isActive) {
            if (status == TextToSpeech.SUCCESS) break
            else if (status != INIT_STATUS_WAITING) {
                tts = null
                return@coroutineScope false // 初始化失败
            }

            try {
                delay(100)
            } catch (_: CancellationException) {
            }
        }
        if (!isActive) { // 取消了
            tts = null
            return@coroutineScope false
        }

        return@coroutineScope true
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }

    val voices: List<Voice>
        get() = try {
            tts?.voices?.toList()!!
        } catch (e: NullPointerException) {
            emptyList()
        }

    val locales: List<Locale>
        get() = try {
            tts!!.availableLanguages.toList().sortedBy { it.toString() }
        } catch (e: NullPointerException) {
            emptyList()
        }

}