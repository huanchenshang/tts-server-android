package com.github.jing332.tts.speech.plugin.engine

import android.content.Context
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.script.annotation.ScriptInterface
import com.github.jing332.script.rhino.RhinoScriptEngine
import com.github.jing332.script.runtime.console.ConsoleImpl
import com.github.jing332.script.simple.CompatScriptRuntime
import com.github.jing332.script.source.toScriptSource
import com.github.jing332.tts.speech.EmptyInputStream
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.typedarrays.NativeInt8Array
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

open class TtsPluginEngineV2(val context: Context, val plugin: Plugin) {
    companion object {
        const val OBJ_PLUGIN_JS = "PluginJS"

        const val FUNC_GET_AUDIO = "getAudio"
        const val FUNC_GET_AUDIO_V2 = "getAudioV2"
        const val FUNC_ON_LOAD = "onLoad"
        const val FUNC_ON_STOP = "onStop"
    }

    var console: ConsoleImpl
        get() = engine.runtime.console as ConsoleImpl
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

    protected val pluginJsObj: ScriptableObject by lazy {
        (engine.get(OBJ_PLUGIN_JS) as? ScriptableObject)
            ?: throw IllegalStateException("$OBJ_PLUGIN_JS not found")
    }

    protected var engine: RhinoScriptEngine = RhinoScriptEngine(runtime)

    open fun execute(script: String): Any? = engine.execute(script.toScriptSource())

    @Suppress("UNCHECKED_CAST")
    fun eval() {
        execute(plugin.code)

        pluginJsObj.apply {
            plugin.name = get("name").toString()
            plugin.pluginId = get("id").toString()
            plugin.author = get("author").toString()

            try {
                plugin.defVars = get("vars") as Map<String, Map<String, String>>
            } catch (_: NullPointerException) {
                plugin.defVars = emptyMap()
            } catch (t: Throwable) {
                plugin.defVars = emptyMap()

                throw ClassCastException("\"vars\" bad format").initCause(t)
            }

            plugin.version = try {
                (get("version") as Double).toInt()
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

            is Undefined -> null

            else -> throw IllegalArgumentException("getAudio() return type not suppport: ${result.javaClass.name}")
        }
    }

    private fun getAudioV2(request: Map<String, Any>): InputStream {
        val pos = PipedOutputStream()
        val pis = PipedInputStream(pos)
        val callback = object {
            @ScriptInterface
            fun write(data: ByteArray) {
                pos.write(data)
            }

            @ScriptInterface
            fun close() {
                pos.close()
            }
        }

        val result = engine.invokeMethod(pluginJsObj, FUNC_GET_AUDIO_V2, callback, request)
            ?: throw NoSuchMethodException("getAudioV2() not found")

        return handleAudioResult(result) ?: pis
    }

    fun getAudio(
        text: String,
        locale: String,
        voice: String,
        rate: Float = 1f,
        volume: Float = 1f,
        pitch: Float = 1f
    ): InputStream {
        val result = try {
            engine.invokeMethod(
                pluginJsObj,
                FUNC_GET_AUDIO,
                text,
                locale,
                voice,
                (rate * 50f).toInt(),
                (volume * 50f).toInt(),
                (pitch * 50f).toInt()
            )
        } catch (_: NoSuchMethodException) {
            val request = mapOf(
                "text" to text,
                "locale" to locale,
                "voice" to voice,
                "rate" to rate,
                "volume" to volume,
                "pitch" to pitch
            )
            getAudioV2(request)
        }

        return handleAudioResult(result) ?: EmptyInputStream
    }

}