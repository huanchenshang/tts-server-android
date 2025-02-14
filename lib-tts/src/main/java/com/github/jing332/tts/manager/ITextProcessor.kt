package com.github.jing332.tts.manager

import android.content.Context
import com.github.michaelbull.result.Result

interface ITextProcessor {
    fun init(context: Context, configs: Map<Long, TtsConfiguration>): Result<Unit, com.github.jing332.tts.error.TextProcessorError>

    fun process(text: String, forceConfigId: Long? = null): Result<List<TextSegment>, com.github.jing332.tts.error.TextProcessorError>
}