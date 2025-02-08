package com.github.jing332.tts.speech.plugin

import android.content.Context
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.speech.ITtsService
import com.github.jing332.tts.speech.plugin.engine.TtsPluginEngineV2
import kotlinx.coroutines.runInterruptible
import java.io.InputStream

open class PluginTtsService(
    val context: Context,
    val pluginId: String
) : ITtsService<PluginTtsSource>() {

    private var mEngine: TtsPluginEngineV2? = null

    override var state: EngineState = EngineState.Uninitialized()

    open fun getPlugin(id: String): Plugin = dbm.pluginDao.getByPluginId(pluginId)
        ?: throw IllegalStateException("Plugin not found: $pluginId")

    override suspend fun getStream(params: SystemParams, source: PluginTtsSource): InputStream =
        runInterruptible {
            val speed = if (source.speed == 0f) params.speed else source.speed
            val volume = if (source.volume == 0f) params.volume else source.volume
            val pitch = if (source.pitch == 0f) params.pitch else source.pitch

            mEngine?.getAudio(
                text = params.text,
                locale = source.locale,
                voice = source.voice,
                rate = speed,
                volume = volume,
                pitch = pitch
            ) ?: throw IllegalStateException("Engine not initialized: $pluginId")
        }

    override suspend fun onInit() {
        state = EngineState.Initializing
        mEngine = TtsPluginEngineManager.getEngine(context, getPlugin(pluginId))

        state = EngineState.Initialized
    }

    override fun onDestroy() {
        state = EngineState.Uninitialized()

    }
}