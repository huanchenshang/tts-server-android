package com.github.jing332.tts.error

sealed interface SynthesisError {
    data object ConfigEmpty: SynthesisError
    data object NotFoundPresetConfig: SynthesisError
}