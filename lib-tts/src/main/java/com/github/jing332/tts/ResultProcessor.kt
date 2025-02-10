package com.github.jing332.tts

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessingPipeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import com.github.jing332.common.audio.AudioDecoder.Companion.readPcmChunk
import com.github.jing332.common.audio.exo.ExoAudioDecoder
import com.github.jing332.tts.manager.IPcmAudioCallback
import com.github.jing332.tts.manager.IResultProcessor
import com.github.jing332.tts.manager.TtsConfiguration
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.nio.ByteBuffer

internal class ResultProcessor(
    private val context: ManagerContext,
) : IResultProcessor {
    private val mDecoder by lazy { ExoAudioDecoder(context.androidContext) }

    private suspend fun internalProcessStream(
        ins: InputStream,
        tts: TtsConfiguration,
        callback: IPcmAudioCallback
    ) {
        if (tts.audioFormat.isNeedDecode) {
            mDecoder.callback = ExoAudioDecoder.Callback { byteBuffer ->
                val buffer = ByteArray(byteBuffer.remaining())
                byteBuffer.get(buffer)
                runBlocking {
                    callback.onPcmData(buffer)
                }
            }

            if (context.cfg.streamPlayEnabled)
                mDecoder.doDecode(ins)
            else
                ins.use {
                    mDecoder.doDecode(ins.readBytes())
                }


        } else {
            ins.readPcmChunk { callback.onPcmData(it) }
        }

    }

    @OptIn(UnstableApi::class)
    private fun silenceSkippingAudioProcessor(
    ): SilenceSkippingAudioProcessor {
        val p = SilenceSkippingAudioProcessor(
            SilenceSkippingAudioProcessor.DEFAULT_MINIMUM_SILENCE_DURATION_US,
            SilenceSkippingAudioProcessor.DEFAULT_SILENCE_RETENTION_RATIO,
            SilenceSkippingAudioProcessor.DEFAULT_MAX_SILENCE_TO_KEEP_DURATION_US,
            SilenceSkippingAudioProcessor.DEFAULT_MIN_VOLUME_TO_KEEP_PERCENTAGE,
            SilenceSkippingAudioProcessor.DEFAULT_SILENCE_THRESHOLD_LEVEL
        )
        p.setEnabled(true)
        return p
    }

    @OptIn(UnstableApi::class)
    override suspend fun processStream(
        ins: InputStream,
        tts: TtsConfiguration,
        targetSampleRate: Int,
        callback: IPcmAudioCallback
    ) {
        val processor = AudioProcessingPipeline(
            ImmutableList.of(
                silenceSkippingAudioProcessor()
            )
        )

        processor.configure(
            AudioProcessor.AudioFormat(
                tts.audioFormat.sampleRate,
                1,
                C.ENCODING_PCM_16BIT
            )
        )

        processor.flush()

        val sonic =
            if (tts.audioParams.isDefaultValue && tts.audioFormat.sampleRate == targetSampleRate) null
            else com.github.jing332.common.audio.Sonic(tts.audioFormat.sampleRate, 1).apply {
                speed = if (tts.audioParams.speed >= 0) 1f else tts.audioParams.speed
                volume = if (tts.audioParams.volume >= 0) 1f else tts.audioParams.volume
                pitch = if (tts.audioParams.pitch >= 0) 1f else tts.audioParams.pitch

                rate = tts.audioFormat.sampleRate.toFloat() / targetSampleRate.toFloat()
            }

        suspend fun sonic(pcm: ByteArray) {
            sonic?.apply {
                writeBytesToStream(pcm, pcm.size)
                callback.onPcmData(sonic.readBytesFromStream(sonic.samplesAvailable()))
            }
        }

        suspend fun handle(pcm: ByteArray?) {
            suspend fun read() {
                val outBuffer = processor.output
                val bytes = ByteArray(outBuffer.remaining())
                outBuffer.get(bytes)
                sonic(bytes)
            }

            if (context.cfg.silenceSkipEnabled) {
                if (pcm != null) sonic(pcm)
            } else if (pcm == null) {
                processor.queueEndOfStream()
                read()
            } else {
                val inBuffer = ByteBuffer.wrap(pcm)
                while (inBuffer.hasRemaining()) {
                    processor.queueInput(inBuffer)
                    read()
                }
            }
        }

        internalProcessStream(
            ins, tts
        ) { pcm ->
            handle(pcm = pcm)
        }

    }


}