package com.github.jing332.database.entities.systts

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class AudioParams(
    var speed: Float = FOLLOW_GLOBAL_VALUE,

    var volume: Float = FOLLOW_GLOBAL_VALUE,

    var pitch: Float = FOLLOW_GLOBAL_VALUE
) : Parcelable {
    companion object {
        const val FOLLOW_GLOBAL_VALUE = 0f
    }

    val isDefaultValue: Boolean
        get() = speed == 1f && volume == 1f && pitch == 1f

    fun copyIfFollow(followSpeed: Float, followVolume: Float, followPitch: Float): AudioParams {
        return AudioParams(
            if (speed == FOLLOW_GLOBAL_VALUE) followSpeed else speed,
            if (volume == FOLLOW_GLOBAL_VALUE) followVolume else volume,
            if (pitch == FOLLOW_GLOBAL_VALUE) followPitch else pitch
        )
    }

    fun reset(v: Float) {
        speed = v
        volume = v
        pitch = v
    }

}
