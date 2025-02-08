package com.github.jing332.tts

sealed interface TtsError

data object ConfigEmptyError : TtsError
data class GetBgm(val cause: Throwable?) : TtsError

sealed interface TextProcessorError : TtsError
data object ForceConfigIdNotFound : TextProcessorError
data class SpeechRuleNotFound(val ruleId: String) : TextProcessorError
data class HandleTextError(val cause: Throwable) : TextProcessorError
data object NoMatchingConfigFound : TextProcessorError

sealed interface RequesterError : TtsError
data object TimesLimitExceeded : RequesterError
