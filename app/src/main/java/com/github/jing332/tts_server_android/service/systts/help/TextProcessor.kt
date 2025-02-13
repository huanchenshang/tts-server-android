package com.github.jing332.tts_server_android.service.systts.help

import android.content.Context
import com.github.jing332.common.utils.StringUtils
import com.github.jing332.database.constants.ReplaceExecution
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import com.github.jing332.tts.ConfigEmptyError
import com.github.jing332.tts.ForceConfigIdNotFound
import com.github.jing332.tts.HandleTextError
import com.github.jing332.tts.NoMatchingConfigFound
import com.github.jing332.tts.SpeechRuleNotFound
import com.github.jing332.tts.TextProcessorError
import com.github.jing332.tts.TtsError
import com.github.jing332.tts.manager.ITextProcessor
import com.github.jing332.tts.manager.TextSegment
import com.github.jing332.tts.manager.TtsConfiguration
import com.github.jing332.tts_server_android.conf.SystemTtsConfig
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.random.Random

class TextProcessor : ITextProcessor {
    companion object {
        private val logger = KotlinLogging.logger { this::class.java.name }
    }

    private val isMultiVoice: Boolean
        get() = SystemTtsConfig.isMultiVoiceEnabled.value

    private val isSplitSentence: Boolean
        get() = SystemTtsConfig.isSplitEnabled.value

    private val isReplaceEnabled: Boolean
        get() = SystemTtsConfig.isReplaceEnabled.value

    private lateinit var engine: SpeechRuleEngine
    private val textReplacer = TextReplacer()

    private var singleVoice: TtsConfiguration? = null
    private var configs: List<TtsConfiguration> = emptyList()
    private var speechRules: List<SpeechRuleInfo> = emptyList()
    private val random by lazy { Random(System.currentTimeMillis()) }

    /**
     * [ConfigEmptyError]
     */
    override fun init(
        context: Context,
        configs: Map<Long, TtsConfiguration>
    ): com.github.michaelbull.result.Result<Unit, TtsError> {
        if (isMultiVoice) {
            val ruleId = configs.values.toList().component1().speechInfo.tagRuleId
            val speechRule =
                dbm.speechRuleDao.getByRuleId(ruleId) ?: return Err(SpeechRuleNotFound(ruleId))
            engine = SpeechRuleEngine(context, speechRule).apply { eval() }
            this.configs =
                configs.entries.map { it.value.copy(speechInfo = it.value.speechInfo.copy(configId = it.key)) }
            speechRules = this.configs.map { it.speechInfo }
        } else {
            this.configs = configs.values.toList()
            if (this.configs.isEmpty()) {
                return Err(ConfigEmptyError)
            } else
                singleVoice = this.configs.random(random)
        }

        textReplacer.load()
        return Ok(Unit)
    }

    private fun splitText(text: String): List<String> {
        return if (!isSplitSentence) listOf(text)
        else if (isMultiVoice) {
            try {
                engine.splitText(text).map { it.toString() }
            } catch (_: NoSuchMethodException) {
                StringUtils.splitSentences(text)
            }
        } else {
            StringUtils.splitSentences(text)
        }
    }

    /**
     * [ForceConfigIdNotFound] forceConfigId not fount in configs
     *
     * [HandleTextError] An error occurred in JavaScript
     *
     * [NoMatchingConfigFound] No matching config found
     */
    override fun process(
        text: String,
        forceConfigId: Long?
    ): com.github.michaelbull.result.Result<List<TextSegment>, TextProcessorError> {
        val resultList = mutableListOf<TextSegment>()
        val replacedText = textReplacer.replace(text, ReplaceExecution.BEFORE)

        fun add(vararg fragments: TextSegment) {
            fragments.forEach { f ->
                resultList.add(
                    TextSegment(text = textReplacer.replace(f.text, ReplaceExecution.AFTER), f.tts)
                )
            }
        }

        fun splitAndAdd(text: String, config: TtsConfiguration) {
            splitText(text).forEach {
                add(TextSegment(text = it, tts = config))
            }
        }

        if (forceConfigId != null) {
            val config = configs.find { it.speechInfo.configId == forceConfigId }
            if (config == null) {
                return Err(ForceConfigIdNotFound)
            } else
                splitAndAdd(text, config)
        } else if (isMultiVoice) {
            val fragments = try {
                engine.handleText(replacedText, speechRules)
            } catch (e: Exception) {
                return Err(HandleTextError(e))
            }
            fragments.forEach { txtWithTag ->
                if (txtWithTag.text.isNotBlank()) {
                    val sameTagList = configs.filter { it.speechInfo.tag == txtWithTag.tag }
                    val configFromId = sameTagList.find { it.speechInfo.configId == txtWithTag.id }

                    // Exact match ID > random match in tag > random match in all
                    val config = configFromId
                        ?: sameTagList.randomOrNull(random)
                        ?: singleVoice
                        ?: return Err(NoMatchingConfigFound)
                    splitAndAdd(txtWithTag.text, config)
                }
            }
        } else {
            val singleVoice = singleVoice ?: return Err(NoMatchingConfigFound)
            splitAndAdd(replacedText, singleVoice)
        }

        return Ok(resultList)
    }
}