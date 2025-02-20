package com.github.jing332.tts

typealias ValueProvider<T> = () -> T

data class SynthesizerConfig(
    var requestTimeout: ValueProvider<Long> = { 8000 },
    var maxRetryTimes: ValueProvider<Int> = { 1 },
    var toggleTry: ValueProvider<Int> = { 1 },
    var streamPlayEnabled: ValueProvider<Boolean> = { true },
    var silenceSkipEnabled: ValueProvider<Boolean> = { false },

    var bgmShuffleEnabled: ValueProvider<Boolean> = { false },
    var bgmVolume: ValueProvider<Float> = { 1f },

    var provider: ValueProvider<Int> = { 0 },
)