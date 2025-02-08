package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.content.Context
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.source.MsTtsSource
import com.github.jing332.tts_server_android.R

class MsTtsDescriptor(val context: Context, val systemTts: SystemTtsV2) :
    TtsItemDescriptor<MsTtsSource>(systemTts.config) {
    override val type: String
        get() = "Edge"

    override val name: String
        get() = systemTts.displayName

    override val desc: String
        get() {
            val strFollow by lazy { context.getString(R.string.follow) }

            val rateStr =
                if (source.speed == MsTtsSource.SPEED_FOLLOW) strFollow else source.speed
            val pitchStr =
                if (source.pitch == MsTtsSource.PITCH_FOLLOW) strFollow else source.pitch

            return context.getString(
                R.string.systts_play_params_description,
                "<b>${rateStr}</b>",
                "<b>${source.volume}</b>",
                "<b>${pitchStr}</b>"
            )
        }

    override val bottom: String
        get() = source.format
}