package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.hutool.core.io.FileUtil
import com.github.jing332.common.LogEntry
import com.github.jing332.common.toLogLevel
import com.github.jing332.common.utils.toHtmlBold
import com.github.jing332.common.utils.toHtmlItalic
import com.github.jing332.common.utils.toHtmlSmall
import com.github.jing332.tts_server_android.constant.AppConst
import com.google.common.io.LineReader
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.RandomAccessFile
import java.nio.file.Path
import java.nio.file.Paths

class TtsLogViewModel : ViewModel() {
    companion object {
        const val TAG = "TtsLogViewModel"
        private val logger = KotlinLogging.logger(TAG)
        val file =
            File(AppConst.externalFilesDir.absolutePath + File.separator + "log" + File.separator + "system_tts.log")
    }

    val logs = mutableStateListOf<LogEntry>()

    fun clear() {
        logs.clear()
        runCatching {
            FileWriter(file, false).use { it.write(CharArray(0)) }
        }.onFailure {
            logger.error(it) { "clear()" }
        }
    }


    private fun toLogEntry(line: String): LogEntry {
        return line.split(" | ").let {
            val time = it[0]
            val level = it[1]
            val msg = it[2]
            LogEntry(
                level.toLogLevel(), if (msg.isBlank()) "" else
                    "${time.toHtmlItalic()} <br>$msg"
            )
        }
    }

    fun logDir(): String {
        return file.absolutePath
    }

    init {
        try {
            viewModelScope.launch(Dispatchers.IO) { pull() }
        } catch (e: Exception) {
        }
    }

    fun add(line: String) {
        try {
            val logEntry = toLogEntry(line)
            logs.add(logEntry)
        } catch (e: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    fun pull() {
        file.reader().use { reader ->
            reader.useLines {
                it.forEach { line ->
                    add(line)
                }
            }
        }

    }
}