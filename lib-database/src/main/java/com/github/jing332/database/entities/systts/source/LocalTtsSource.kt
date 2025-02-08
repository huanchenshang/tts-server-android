package com.github.jing332.database.entities.systts.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("local")
data class LocalTtsSource(
    val engine: String = "",
    override val locale: String = "",
    override val voice: String = "",

    val speed: Float = SPEED_FOLLOW,
    val pitch: Float = PITCH_FOLLOW,
    val volume: Float = VOLUME_FOLLOW,

    val extraParams: MutableList<LocalTtsParameter>? = null,
    val isDirectPlayMode: Boolean = true
) : ITtsSource() {
    companion object {
        const val SPEED_FOLLOW = 0f
        const val PITCH_FOLLOW = 0f
        const val VOLUME_FOLLOW = 0f
    }

    override fun getKey(): String = engine

    override fun isSyncPlayMode(): Boolean = isDirectPlayMode
}