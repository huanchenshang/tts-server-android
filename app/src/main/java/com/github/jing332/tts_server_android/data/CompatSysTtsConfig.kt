package com.github.jing332.tts_server_android.data

import com.github.jing332.tts_server_android.App
import com.github.jing332.tts_server_android.util.FileUtils
import kotlinx.serialization.decodeFromString
import java.io.File

/* 旧配置 已弃用*/
@kotlinx.serialization.Serializable
data class CompatSysTtsConfig(
    var list: ArrayList<CompatSysTtsConfigItem>,
    var isSplitSentences: Boolean = true,
    var isMultiVoice: Boolean = false,
    var isReplace: Boolean = false,
    var timeout: Int = 5000,
    var minDialogueLength: Int = 0
) {
    companion object {
        private val filepath by lazy { "${App.context.filesDir.absolutePath}/system_tts_config.json" }
        fun read(): CompatSysTtsConfig? {
            return try {
                val file = File(filepath)
                if (!FileUtils.fileExists(file)) return null

                val str = File(filepath).readText()
                App.jsonBuilder.decodeFromString<CompatSysTtsConfig>(str)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        /**
         * return 是否成功
         */
        fun deleteConfigFile(): Boolean {
            return try {
                File(filepath).delete()
            } catch (e: Exception) {
                return false
            }
        }
    }
}