package com.github.jing332.tts_server_android.compose.systts.plugin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.jing332.common.utils.sizeToReadable
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.source.PluginTtsSource

import com.github.jing332.script.runtime.console.Console
import com.github.jing332.tts.speech.plugin.engine.TtsPluginUiEngineV2
import com.github.jing332.tts_server_android.app
import com.github.jing332.tts_server_android.conf.PluginConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    val console: Console = Console()

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
        mEngine = mEngine?.also { it.plugin = plugin }
            ?: TtsPluginUiEngineV2(app, plugin).also { it.console = console }
        mEngine?.eval()
    }

    fun updateCode(code: String) {
        updatePlugin(plugin.copy(code = code))
    }

//    fun clearPluginCache() {
//        val file = File("${app.externalCacheDir!!.absolutePath}/${plugin.pluginId}")
//        file.deleteRecursively()
//    }

    private var mDebugJob: Job? = null
    fun debug(code: String) {
        console.info("START\n==========")

        mDebugJob = viewModelScope.launch(Dispatchers.IO) {
            val plugin = try {
                updateCode(code)
                engine.plugin
            } catch (e: Exception) {
                writeErrorLog(e)
                console.info("\n" + "==========\nEND")
                return@launch
            }
            console.debug(plugin.toString().replace(", ", "\n"))

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
                val stream = engine.getAudio(
                    text = PluginConfig.textParam.value,
                    locale = pluginSource.locale,
                    voice = pluginSource.voice
                )

                val bytes = stream.readBytes()
                console.info(
                    "Audio size: ${
                        bytes.size.toLong().sizeToReadable()
                    }"
                )
            }.onFailure {
                writeErrorLog(it)
            }

            console.info("\n" + "==========\nEND")
        }
    }

    fun stopDebug() {
        runCatching {
            mDebugJob?.cancel()
            engine.onStop()
        }
    }

    private fun writeErrorLog(t: Throwable) {
        console.error(t.message)
    }

    override fun onCleared() {
        super.onCleared()
        runCatching {
            mEngine?.onStop()
        }
    }

}