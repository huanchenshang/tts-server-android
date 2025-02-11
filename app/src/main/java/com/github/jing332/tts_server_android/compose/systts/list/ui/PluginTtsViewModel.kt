package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.app.Application
import android.content.Context
import android.widget.LinearLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.drake.net.utils.withIO
import com.drake.net.utils.withMain
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts.speech.plugin.TtsPluginEngineManager
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
import com.github.jing332.tts_server_android.app
import io.github.oshai.kotlinlogging.KotlinLogging

class PluginTtsViewModel(app: Application) : AndroidViewModel(app) {
    companion object {
        private val logger = KotlinLogging.logger { PluginTtsViewModel::class.java.name }
    }

    lateinit var engine: TtsPluginUiEngineV2

    var onGetPlugin: (String) -> Plugin = { id: String ->
        dbm.pluginDao.getByPluginId(id)
            ?: throw IllegalStateException("Plugin $id not found from database")
    }

    private fun initEngine(source: PluginTtsSource) {
        if (this::engine.isInitialized) return

        val plugin = onGetPlugin(source.pluginId)
        engine = TtsPluginEngineManager.getEngine(app, plugin)
        engine.source = source
    }

    var isLoading by mutableStateOf(true)

    val locales = mutableStateListOf<Pair<String, String>>()
    val voices = mutableStateListOf<Pair<String, String>>()

    suspend fun load(context: Context, source: PluginTtsSource, linearLayout: LinearLayout) =
        withIO {
            isLoading = true
            try {
                initEngine(source)
                 engine.onLoadData()

                withMain {
                    engine.onLoadUI(context, linearLayout)
                }

                updateLocales()
                updateVoices(source.locale)
            } catch (t: Throwable) {
                throw t
            } finally {
                isLoading = false
            }
        }

    private fun updateLocales() {
        locales.clear()
        locales.addAll(engine.getLocales().toList())
        logger.debug { "updateLocales: ${locales.joinToString { it.first + " - " + it.second }}" }
    }

    fun updateVoices(locale: String) {
        voices.clear()
        voices.addAll(engine.getVoices(locale).toList())
        logger.debug { "updateVoices(${locale}): ${voices.joinToString { it.first + " - " + it.second }}" }
    }

    fun updateCustomUI(locale: String, voice: String) {
        try {
            engine.onVoiceChanged(locale, voice)
        } catch (_: NoSuchMethodException) {
        }
    }
}