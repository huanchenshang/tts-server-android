package com.github.jing332.tts.speech.plugin

import android.content.Context
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.tts.AbstractCachedManager
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
import java.util.concurrent.TimeUnit

object TtsPluginEngineManager : AbstractCachedManager<String, TtsPluginUiEngineV2>(
    timeout = 1000L * 60L * 10L, // 10 min
    delay = 1000L * 60L * 1L, // 1 min
) {
    fun getEngine(context: Context, plugin: Plugin): TtsPluginUiEngineV2 {
        return cache.get(plugin.pluginId) ?: run {
            val engine = TtsPluginUiEngineV2(context, plugin)
            cache.put(plugin.pluginId, engine)
            engine
        }
    }
}