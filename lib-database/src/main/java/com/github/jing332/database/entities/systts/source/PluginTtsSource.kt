package com.github.jing332.database.entities.systts.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@SerialName("plugin")
data class PluginTtsSource(
    override val locale: String = "",
    override val voice: String = "",
    val pluginId: String = "",
    val speed: Float = 1f,
    val volume: Float = 1f,
    val pitch: Float = 1f,
    val data: Map<String, String> = mutableMapOf(),

    ) : ITtsSource() {

    override fun getKey(): String {
        // 防止 CachedEngineManager 创建单例 Engine
        return pluginId
    }
}