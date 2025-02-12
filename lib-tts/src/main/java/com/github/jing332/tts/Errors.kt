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
data class RequestError(val cause: Throwable) : RequesterError
data class InitializationError(val reason: String) : RequesterError

sealed interface ResultProcessorError : TtsError

data class AudioSourceError(val cause: Throwable) : ResultProcessorError
data class AudioDecodingError(val cause: Throwable) : ResultProcessorError
