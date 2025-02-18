package com.github.jing332.tts.speech.plugin

import android.content.Context
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.speech.ITtsService
import com.github.jing332.tts.speech.plugin.engine.TtsPluginEngineV2
import java.io.InputStream

open class PluginTtsService(
    val context: Context,
    val plugin: Plugin,
) : ITtsService<PluginTtsSource>() {

    private var mEngine: TtsPluginEngineV2? = null

    var engine: TtsPluginEngineV2?
        get() = mEngine
        set(value) {
            mEngine = value
        }

    override var state: EngineState = EngineState.Uninitialized()

    override suspend fun getStream(params: SystemParams, source: PluginTtsSource): InputStream {
        val speed = if (source.speed == 0f) params.speed else source.speed
        val volume = if (source.volume == 0f) params.volume else source.volume
        val pitch = if (source.pitch == 0f) params.pitch else source.pitch

        return mEngine?.getAudio(
            text = params.text,
            locale = source.locale,
            voice = source.voice,
            rate = speed,
            volume = volume,
            pitch = pitch
        ) ?: throw IllegalStateException("Engine not initialized: ${plugin.pluginId}")
    }

    override suspend fun onInit() {
        state = EngineState.Initializing
        if (mEngine == null)
            mEngine = TtsPluginEngineManager.getEngine(context, plugin)

        state = EngineState.Initialized
    }

    override fun onStop() {
        super.onStop()
        mEngine?.onStop()
    }

    override fun onDestroy() {
        state = EngineState.Uninitialized()
        mEngine?.onStop()
        mEngine = null
    }
}