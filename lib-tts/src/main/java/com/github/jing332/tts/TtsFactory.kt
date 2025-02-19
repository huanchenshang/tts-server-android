package com.github.jing332.tts

import android.content.Context
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.source.ITtsSource
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.speech.ITtsService
import com.github.jing332.tts.speech.local.LocalTtsService
import com.github.jing332.tts.speech.plugin.PluginTtsService

@Suppress("UNCHECKED_CAST")
object TtsFactory {
    fun createEngine(context: Context, source: ITtsSource): ITtsService<ITtsSource>? {
        return when (source) {
            is LocalTtsSource -> LocalTtsService(context, source.engine)
            is PluginTtsSource -> {
                PluginTtsService(
                    context,
                    source.plugin ?: dbm.pluginDao.getEnabled(source.pluginId) ?: return null
                )
            }

            else -> null
        } as ITtsService<ITtsSource>?
    }
}