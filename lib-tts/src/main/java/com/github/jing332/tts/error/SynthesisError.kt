package com.github.jing332.tts.error

sealed interface SynthesisError {
    data object ConfigEmpty : SynthesisError
    data class TextHandle(val err: TextProcessorError) : SynthesisError
}