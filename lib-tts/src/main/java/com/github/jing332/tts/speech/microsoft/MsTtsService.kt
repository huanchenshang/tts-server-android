package com.github.jing332.tts.speech.microsoft

import com.github.jing332.common.utils.CoroutineExtension
import com.github.jing332.database.entities.systts.source.MsTtsSource
import com.github.jing332.lib_gojni.TtsGoNative
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.speech.ITtsService
import java.io.ByteArrayInputStream
import java.io.InputStream

class MsTtsService : ITtsService<MsTtsSource>() {
    companion object {
        private val mTts by lazy { EdgeTtsWS() }
    }

    private fun Float.convertToInt(): Int = ((this - 1f) * 100).toInt()

    override suspend fun getStream(params: SystemParams, source: MsTtsSource): InputStream {
        val speed =
            (if (MsTtsSource.SPEED_FOLLOW == source.speed) params.speed else source.speed).convertToInt()
        val pitch =
            (if (MsTtsSource.PITCH_FOLLOW == source.pitch) params.pitch else source.pitch).convertToInt()
        val volume =
            (if (MsTtsSource.VOLUME_FOLLOW == source.volume) params.volume else source.volume).convertToInt()

        return if (source.api == MsTtsSource.API_EDGE_NATIVE) {
            val bytes =
                TtsGoNative.getAudio(
                    params.text,
                    voice = source.voice,
                    format = source.format,
                    rate = speed,
                    pitch = pitch,
                    volume = volume
                )
            ByteArrayInputStream(bytes)
        } else {
            CoroutineExtension.onCanceled {
                mTts.cancelConnect()
            }
            val ins = mTts.getAudio(
                text = params.text,
                voice = source.voice,
                format = source.format,
                rate = speed,
                pitch = pitch,
                volume = volume
            )

            return ins
        }
    }

    override var state: EngineState = EngineState.Initialized

    override suspend fun onInit() {
    }

    override fun onStop() {
    }

    override fun onDestroy() {
    }
}