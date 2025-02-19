package com.github.jing332.tts.speech.plugin.engine

import android.content.Context
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.script.engine.RhinoScriptEngine
import com.github.jing332.script.runtime.console.Console
import com.github.jing332.script.runtime.NativeResponse
import com.github.jing332.script.simple.CompatScriptRuntime
import com.github.jing332.script.source.toScriptSource
import com.github.jing332.tts.speech.EmptyInputStream
import com.github.jing332.tts.speech.plugin.engine.TtsPluginEngineV2.Companion.FUNC_GET_AUDIO
import com.github.jing332.tts.speech.plugin.engine.TtsPluginEngineV2.Companion.FUNC_GET_AUDIO_V2
import com.github.jing332.tts.speech.plugin.engine.TtsPluginEngineV2.Companion.FUNC_ON_LOAD
import com.github.jing332.tts.speech.plugin.engine.TtsPluginEngineV2.Companion.FUNC_ON_STOP
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.typedarrays.NativeInt8Array
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

open class TtsPluginEngineV2(val context: Context, var plugin: Plugin) {
    companion object {
        const val OBJ_PLUGIN_JS = "PluginJS"

        const val FUNC_GET_AUDIO = "getAudio"
        const val FUNC_GET_AUDIO_V2 = "getAudioV2"
        const val FUNC_ON_LOAD = "onLoad"
        const val FUNC_ON_STOP = "onStop"
    }

    var console: Console
        get() = engine.runtime.console
        set(value) {
            engine.runtime.console = value
        }

    protected val ttsrv = TtsEngineContext(
        tts = PluginTtsSource(),
        userVars = plugin.userVars,
        context = context,
        engineId = plugin.pluginId
    )
    val runtime = CompatScriptRuntime(ttsrv)

    var source: PluginTtsSource
        get() = ttsrv.tts
        set(value) {
            ttsrv.tts = value
        }

    protected val pluginJsObj: ScriptableObject
        get() = (engine.get(OBJ_PLUGIN_JS) as? ScriptableObject)
            ?: throw IllegalStateException("Object $OBJ_PLUGIN_JS not found")


    protected var engine: RhinoScriptEngine = RhinoScriptEngine(runtime)

    open protected fun execute(script: String): Any? =
        engine.execute(script.toScriptSource(sourceName = plugin.pluginId))

    @Suppress("UNCHECKED_CAST")
    fun eval() {
        execute(plugin.code)

        pluginJsObj.apply {
            plugin.name = get("name").toString()
            plugin.pluginId = get("id").toString()
            plugin.author = get("author").toString()
            plugin.iconUrl = get("iconUrl")?.toString() ?: ""

            try {
                plugin.defVars = get("vars") as Map<String, Map<String, String>>
            } catch (_: NullPointerException) {
                plugin.defVars = emptyMap()
            } catch (t: Throwable) {
                plugin.defVars = emptyMap()

                throw ClassCastException("\"vars\" bad format").initCause(t)
            }

            plugin.version = try {
                org.mozilla.javascript.Context.toNumber(get("version")).toInt()
            } catch (e: Exception) {
                -1
            }
        }
    }


    fun onLoad(): Any? {
        return try {
            engine.invokeMethod(pluginJsObj, FUNC_ON_LOAD)
        } catch (_: NoSuchMethodException) {
        }
    }

    fun onStop(): Any? {
        return try {
            engine.invokeMethod(pluginJsObj, FUNC_ON_STOP)
        } catch (_: NoSuchMethodException) {
        }
    }

    private fun handleAudioResult(result: Any?): InputStream? {
        if (result == null) return null
        return when (result) {
            is NativeInt8Array -> ByteArrayInputStream(result.toByteArray())

            is InputStream -> result
            is ByteArray -> result.inputStream()
            is PipedOutputStream -> {
                val pis = PipedInputStream(result)
                return pis
            }

            is NativeResponse -> result.rawResponse?.body?.byteStream()

            is Undefined -> null

            else -> throw IllegalArgumentException("getAudio() return type not support: ${result.javaClass.name}")
        }
    }

    private val mMutex by lazy { Mutex() } // stream lock
    private suspend fun getAudioV2(request: Map<String, Any>): InputStream {
        val ins = JsBridgeInputStream()
        val callback = ins.getCallback(mMutex)

        val result = runInterruptible {
            engine.invokeMethod(pluginJsObj, FUNC_GET_AUDIO_V2, request, callback)
                ?: throw NoSuchMethodException("getAudioV2() not found")
        }
        return handleAudioResult(result) ?: ins
    }

    suspend fun getAudio(
        text: String,
        locale: String,
        voice: String,
        rate: Float = 1f,
        volume: Float = 1f,
        pitch: Float = 1f,
    ): InputStream {
        val r = (rate * 50f).toInt()
        val v = (volume * 50f).toInt()
        val p = (pitch * 50f).toInt()
        val result = try {
            runInterruptible {
                engine.invokeMethod(
                    pluginJsObj,
                    FUNC_GET_AUDIO,
                    text,
                    locale,
                    voice,
                    r,
                    v,
                    p
                )
            }
        } catch (_: NoSuchMethodException) {
            val request = mapOf(
                "text" to text,
                "locale" to locale,
                "voice" to voice,
                "rate" to r,
                "speed" to r,
                "volume" to v,
                "pitch" to p
            )
            getAudioV2(request)
        }

        return handleAudioResult(result) ?: EmptyInputStream
    }

}