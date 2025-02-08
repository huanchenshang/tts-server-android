package com.github.jing332.tts.speech.plugin

import android.content.Context
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.tts.AbstractCachedManager
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
import java.util.concurrent.TimeUnit

object TtsPluginEngineManager : AbstractCachedManager<String, TtsPluginUiEngineV2>(
    TimeUnit.SECONDS.toMinutes(10),
    TimeUnit.SECONDS.toSeconds(1)
) {
    fun getEngine(context: Context, plugin: Plugin): TtsPluginUiEngineV2 {
        return cache.get(plugin.pluginId) ?: run {
            val engine = TtsPluginUiEngineV2(context, plugin)
            cache.put(plugin.pluginId, engine)
            engine
        }
    }
}