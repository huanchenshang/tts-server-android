package com.github.jing332.server.forwarder

import kotlinx.serialization.Serializable

@Serializable
data class TtsParams(
    val text: String,
    val engine: String = "",
    val voice: String = "",
    val speed: Int = 50,
    val pitch: Int = 100
)