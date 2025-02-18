package com.github.jing332.tts.speech.plugin.engine

import android.content.Context
import android.widget.LinearLayout
import com.github.jing332.common.utils.dp
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.script.toMap
import io.github.oshai.kotlinlogging.KotlinLogging
import org.mozilla.javascript.NativeMap
import org.mozilla.javascript.ScriptableObject
import java.util.Locale

class TtsPluginUiEngineV2(context: Context, plugin: Plugin) : TtsPluginEngineV2(context, plugin) {
    companion object {
        private val logger = KotlinLogging.logger("TtsPluginUiEngineV2")

        const val FUNC_SAMPLE_RATE = "getAudioSampleRate"
        const val FUNC_IS_NEED_DECODE = "isNeedDecode"

        const val FUNC_LOCALES = "getLocales"
        const val FUNC_VOICES = "getVoices"

        const val FUNC_ON_LOAD_UI = "onLoadUI"
        const val FUNC_ON_LOAD_DATA = "onLoadData"
        const val FUNC_ON_VOICE_CHANGED = "onVoiceChanged"

        const val OBJ_UI_JS = "EditorJS"
    }

    fun dp(px: Int): Int {
        return px.dp
    }

    private val editUiJsObject: ScriptableObject by lazy {
        engine.get(OBJ_UI_JS) as? ScriptableObject
            ?: throw IllegalStateException("$OBJ_UI_JS not found")
    }

    override fun execute(script: String): Any? {
        return super.execute(PackageImporter.default + script)
    }


    fun getSampleRate(locale: String, voice: String): Int? {
        runtime.console.debug("getSampleRate($locale, $voice)")

        return engine.invokeMethod(
            editUiJsObject,
            FUNC_SAMPLE_RATE,
            locale,
            voice
        )?.run {
            return if (this is Int) this
            else (this as Double).toInt()
        }
    }

    fun isNeedDecode(locale: String, voice: String): Boolean {
        runtime.console.debug("isNeedDecode($locale, $voice)")

        return try {
            engine.invokeMethod(editUiJsObject, FUNC_IS_NEED_DECODE, locale, voice)?.run {
                if (this is Boolean) this
                else (this as Double).toInt() == 1
            } ?: true
        } catch (_: NoSuchMethodException) {
            true
        }
    }

    fun getLocales(): Map<String, String> {
        return engine.invokeMethod(editUiJsObject, FUNC_LOCALES).run {
            when (this) {
                is List<*> -> this.associate {
                    it.toString() to Locale.forLanguageTag(it.toString()).run {
                        this.getDisplayName(this)
                    }
                }

                is Map<*, *> -> {
                    this.map { (key, value) ->
                        key.toString() to value.toString()
                    }.toMap()
                }

                else -> emptyMap()
            }
        }
    }

    fun getVoices(locale: String): Map<String, String> {
        return engine.invokeMethod(editUiJsObject, FUNC_VOICES, locale).run {
            when (this) {
                is Map<*, *> -> {
                    this.map { (key, value) ->
                        key.toString() to value.toString()
                    }.toMap()
                }

                is NativeMap -> {
                    this.toMap<CharSequence, CharSequence>().map { (key, value) ->
                        key.toString() to value.toString()
                    }.toMap()
                }

                else -> emptyMap()
            }
        }
    }

    fun onLoadData() {
        runtime.console.debug("onLoadData()...")

        try {
            engine.invokeMethod(editUiJsObject, FUNC_ON_LOAD_DATA)
        } catch (_: NoSuchMethodException) {
        }
    }

    fun onLoadUI(context: Context, container: LinearLayout) {
        runtime.console.debug("onLoadUI()...")
        try {
            engine.invokeMethod(
                editUiJsObject,
                FUNC_ON_LOAD_UI,
                context,
                container
            )
        } catch (_: NoSuchMethodException) {
        }
    }

    fun onVoiceChanged(locale: String, voice: String) {
        runtime.console.debug("onVoiceChanged($locale, $voice)")

        try {
            engine.invokeMethod(
                editUiJsObject,
                FUNC_ON_VOICE_CHANGED,
                locale,
                voice
            )
        } catch (_: NoSuchMethodException) {
        }
    }
}