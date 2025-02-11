package com.github.jing332.tts

import android.content.Context
import com.github.jing332.common.utils.StringUtils
import com.github.jing332.tts.manager.ITextProcessor
import com.github.jing332.tts.manager.TextSegment
import com.github.jing332.tts.manager.TtsConfiguration
import com.github.michaelbull.result.Ok

internal class TextProcessor(val context: ManagerContext) : ITextProcessor {
    private var configs: Map<Long, TtsConfiguration> = mapOf()
    override fun init(
        context: Context,
        configs: Map<Long, TtsConfiguration>
    ): com.github.michaelbull.result.Result<Unit, TtsError> {
        this.configs = configs
        return Ok(Unit)
    }

    override fun process(
        text: String,
        forceConfigId: Long?
    ): com.github.michaelbull.result.Result<List<TextSegment>, TextProcessorError> {
        return StringUtils.splitSentences(text).map {
            TextSegment(text = it, tts = configs.values.random())
        }.run { Ok(this) }
    }
}