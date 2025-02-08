package com.github.jing332.tts.manager

import android.content.Context
import com.github.jing332.tts.TextProcessorError
import com.github.jing332.tts.TtsError
import com.github.michaelbull.result.Result

interface ITextProcessor {
    fun init(context: Context, configs: Map<Long, TtsConfiguration>): Result<Unit, TtsError>

    fun process(text: String, forceConfigId: Long? = null): Result<List<TextFragment>, TextProcessorError>
}