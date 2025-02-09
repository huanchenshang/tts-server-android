package com.github.jing332.tts_server_android.compose.systts.speechrule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.script.runtime.console.ConsoleImpl
import com.github.jing332.tts_server_android.constant.SpeechTarget
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpeechRuleEditViewModel(val app: Application) : AndroidViewModel(app) {
    private val _codeLiveData: MutableLiveData<String> = MutableLiveData()
    val codeLiveData: LiveData<String>
        get() = _codeLiveData

    private lateinit var mSpeechRule: SpeechRule
    private lateinit var mRuleEngine: SpeechRuleEngine

    val speechRule: SpeechRule
        get() = mSpeechRule

    fun init(speechRule: SpeechRule, defaultCode: String) {
        if (speechRule.code.isBlank()) speechRule.code = defaultCode
        updateRule(speechRule) }

    fun updateRule(rule: SpeechRule) {
        mSpeechRule = rule
        _codeLiveData.value = rule.code
        mRuleEngine = SpeechRuleEngine(app, rule)
    }

    fun updateCode(code: String) {
        updateRule(speechRule.copy(code = code))
    }

    fun getConsole(): ConsoleImpl {
        return mRuleEngine.console
    }

    fun evalRuleInfo(code: String) {
        updateCode(code)
        mRuleEngine.evalInfo()
    }

    fun debug(text: String) {
        evalRuleInfo(codeLiveData.value ?: throw IllegalStateException("code is null"))
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                getConsole().info("handleText()...")

                val rules =
                    dbm.systemTtsV2.getEnabledListForSort(SpeechTarget.CUSTOM_TAG).map {
                        (it.config as TtsConfigurationDTO).speechRule.apply { configId = it.id }
                    }
                val list = mRuleEngine.handleText(text, rules)
                try {
                    list.forEach {
                        val texts = mRuleEngine.splitText(it.text)
                        getConsole().info(
                            "\ntag=${it.tag}, id=${it.id}, text=${it.text.trim()}, splittedTexts=${
                                texts.joinToString(" | ").trim()
                            }"
                        )
                    }
                } catch (_: NoSuchMethodException) {
                }
            }.onFailure {
                getConsole().error(it.stackTraceToString())
            }
        }
    }

}