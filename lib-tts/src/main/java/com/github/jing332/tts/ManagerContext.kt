package com.github.jing332.tts

import android.content.Context
import com.github.jing332.tts.manager.event.IEventListener
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

data class ManagerContext(
    var androidContext: Context,
    var logger: KLogger = KotlinLogging.logger { "tts-default" },
    var cfg: TtsManagerConfig = TtsManagerConfig(),
    var eventListener: IEventListener? = null
) {
}