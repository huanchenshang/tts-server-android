package com.github.jing332.tts_server_android.compose.systts.plugin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.jing332.common.utils.StringUtils.sizeToReadable
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.script.runtime.console.ConsoleImpl
import com.github.jing332.script.runtime.console.GlobalConsole
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
import com.github.jing332.tts_server_android.app
import com.github.jing332.tts_server_android.conf.PluginConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PluginEditorViewModel(app: Application) : AndroidViewModel(app) {
    companion object {
        private const val TAG = "PluginEditViewModel"
    }

    private var mEngine: TtsPluginUiEngineV2? = null

    val engine: TtsPluginUiEngineV2
        get() = mEngine ?: throw IllegalStateException("Engine is null")

    val pluginSource: PluginTtsSource
        get() = engine.source

    val plugin: Plugin
        get() = engine.plugin

    private val _updateCodeLiveData = MutableLiveData<String>()

    val codeLiveData: LiveData<String>
        get() = _updateCodeLiveData

    val console: ConsoleImpl = GlobalConsole.get()

    fun init(plugin: Plugin, defaultCode: String) {
        plugin.apply { if (code.isEmpty()) code = defaultCode }

        updatePlugin(plugin)
        updateSource(PluginTtsSource())

        _updateCodeLiveData.postValue(plugin.code)
    }

    // Update the `ttsrv.tts` in JS
    fun updateSource(source: PluginTtsSource) {
        engine.source = source
    }

    fun updatePlugin(plugin: Plugin) {
        // Create new engine and set console, source
        mEngine = TtsPluginUiEngineV2(app, plugin).also { new ->
            new.console = console

            mEngine?.let { old -> new.source = old.source }
        }
    }

    fun updateCode(code: String) {
        updatePlugin(plugin.copy(code = code))
    }

    fun save(): Plugin {
        engine.eval()
        return engine.plugin
    }


//    fun clearPluginCache() {
//        val file = File("${app.externalCacheDir!!.absolutePath}/${plugin.pluginId}")
//        file.deleteRecursively()
//    }

    private fun evalInfo(): Boolean {
        val plugin = try {
            engine.eval()
            engine.plugin
        } catch (e: Exception) {
            writeErrorLog(e)
            return false
        }
        console.debug(plugin.toString().replace(", ", "\n"))
        return true
    }

    fun debug(code: String) {
        updateCode(code)
        if (!evalInfo()) return
        viewModelScope.launch(Dispatchers.IO) {
            console.debug("")
            kotlin.runCatching {
                val sampleRate = engine.getSampleRate(pluginSource.locale, pluginSource.voice)
                console.debug("Sample rate: $sampleRate")
            }.onFailure {
                writeErrorLog(it)
            }

            runCatching {
                val isNeedDecode =
                    engine.isNeedDecode(pluginSource.locale, pluginSource.voice)
                console.debug("Need decode: $isNeedDecode")
            }.onFailure {
                writeErrorLog(it)
            }

            kotlin.runCatching {
                engine.onLoad()
                val audio = engine.getAudio(
                    text = PluginConfig.textParam.value,
                    locale = pluginSource.voice,
                    voice = pluginSource.locale
                )
                val bytes = audio.readBytes()
                console.info(
                    "Audio size: ${
                        bytes.size.toLong().sizeToReadable()
                    }"
                )
            }.onFailure {
                writeErrorLog(it)
            }
        }
    }

    private fun writeErrorLog(t: Throwable) {
//        val errStr = if (t is ScriptException) {
//            "${t.lineNumber}Line: ${t.rootCause?.message ?: t}"
//        } else {
//            t.message + "(${t.readableString})"
//        }
        console.error(t.message)
    }
}