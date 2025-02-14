package com.github.jing332.tts

data class TtsManagerConfig(
    var requestTimeout: () -> Long = { 8000 },
    var maxRetryTimes: () -> Int = { 1 },
    var toggleTry: () -> Int = { 1 },
    var streamPlayEnabled: () -> Boolean = { true },
    var silenceSkipEnabled: () -> Boolean = { false },

    var bgmShuffleEnabled: () -> Boolean = { false },
    var bgmVolume: () -> Float = { 1f },
) {
}