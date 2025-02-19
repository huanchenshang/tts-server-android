package com.github.jing332.tts

import android.R.attr.handle
import android.content.Context
import android.system.Os.pipe
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessingPipeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import com.github.jing332.common.audio.AudioDecoder.Companion.readPcmChunk
import com.github.jing332.common.audio.exo.ExoAudioDecoder
import com.github.jing332.common.utils.rootCause
import com.github.jing332.tts.error.StreamProcessorError
import com.github.jing332.tts.error.StreamProcessorError.AudioDecoding
import com.github.jing332.tts.error.StreamProcessorError.HandleError
import com.github.jing332.tts.manager.IPcmAudioCallback
import com.github.jing332.tts.manager.IResultProcessor
import com.github.jing332.tts.manager.RequestPayload
import com.github.jing332.tts.manager.TtsConfiguration
import com.github.jing332.tts.manager.event.NormalEvent
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.google.common.collect.ImmutableList
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okio.`-DeprecatedOkio`.buffer
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.jvm.Throws
import kotlin.math.max
import kotlin.system.measureTimeMillis

internal class DefaultResultProcessor(
    private val context: ManagerContext,
) : IResultProcessor {
    companion object {
        val logger = KotlinLogging.logger("DefaultResultProcessor")
    }

    private var _decoder: ExoAudioDecoder? = null
    private val mDecoder: ExoAudioDecoder
        get() = _decoder ?: throw IllegalStateException("decoder not init")

    override suspend fun init(context: Context) {
        _decoder = ExoAudioDecoder(context)
    }

    @Throws(ExoPlaybackException::class)
    private suspend fun decode(
        ins: InputStream,
        tts: TtsConfiguration,
        callback: IPcmAudioCallback,
    ) {
        if (tts.audioFormat.isNeedDecode) {
            mDecoder.callback = ExoAudioDecoder.Callback { byteBuffer ->
                val buffer = ByteArray(byteBuffer.remaining())
                byteBuffer.get(buffer)
                runBlocking {
                    callback.onPcmData(buffer)
                }
            }

            if (context.cfg.streamPlayEnabled())
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

    private val sonicAudioProcessor by lazy { com.github.jing332.common.audio.exo.SonicAudioProcessor() }
    private val skipAudioProcessor by lazy { silenceSkippingAudioProcessor() }

    /**
     * @throw [CancellationException]
     */
    @OptIn(UnstableApi::class)
    override suspend fun processStream(
        ins: InputStream,
        request: RequestPayload,
        targetSampleRate: Int,
        callback: IPcmAudioCallback,
    ): Result<Unit, StreamProcessorError> {
        val config = request.config
        logger.debug {
            "req=${request.text}, sampleRate=${config.audioFormat.sampleRate}, targetSampleRate=${targetSampleRate}"
        }

        val sonicEnabled =
            config.audioParams.speed != 1f || config.audioParams.pitch != 1f
                    || config.audioParams.volume != 1f && config.audioFormat.sampleRate != targetSampleRate
        try {
            val stream = getAudioStream(ins, request).onFailure { return Err(it) }.value

            val pipelines = listOf(
                if (context.cfg.silenceSkipEnabled()) skipAudioProcessor else null,
                if (sonicEnabled) sonicAudioProcessor else null
            ).filterNotNull()
            val processor = AudioProcessingPipeline(ImmutableList.copyOf(pipelines))

            if (pipelines.isNotEmpty()) {
                processor.configure(
                    AudioProcessor.AudioFormat(
                        config.audioFormat.sampleRate,
                        1,
                        C.ENCODING_PCM_16BIT
                    )
                )
                if (sonicEnabled)
                    sonicAudioProcessor.apply {
                        speed = if (config.audioParams.speed <= 0f) 1f else config.audioParams.speed
                        volume =
                            if (config.audioParams.volume <= 0f) 1f else config.audioParams.volume
                        pitch = if (config.audioParams.pitch <= 0f) 1f else config.audioParams.pitch
                        rate = config.audioFormat.sampleRate.toFloat() / targetSampleRate.toFloat()
                    }

                processor.flush()
            }


            suspend fun handle(pcm: ByteArray?) {
                if (pcm == null) {
                    processor.queueEndOfStream()
                    readProcessedData(processor, callback)
                } else {
                    if (pipelines.isEmpty()) callback.onPcmData(pcm)

                    val inBuffer = ByteBuffer.wrap(pcm)
                    while (inBuffer.hasRemaining()) {
                        processor.queueInput(inBuffer)
                        readProcessedData(processor, callback)
                    }
                }
            }

            try {
                decode(
                    ins = stream,
                    tts = config,
                    callback = { pcm -> handle(pcm = pcm) })
            } catch (e: ExoPlaybackException) {
                logger.error(e) { "streaming error" }
                return if (e.type == ExoPlaybackException.TYPE_SOURCE)
                    Err(StreamProcessorError.AudioSource(e.rootCause))
                else
                    Err(StreamProcessorError.AudioDecoding(e))
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Err(StreamProcessorError.HandleError(e))
        }

        return Ok(Unit)
    }

    private suspend fun getAudioStream(
        ins: InputStream,
        request: RequestPayload,
    ): Result<InputStream, StreamProcessorError> = if (context.cfg.streamPlayEnabled()) {
        context.event?.dispatch(NormalEvent.HandleStream(request))
        Ok(ins)
    } else {
        try {
            ins.use {
                val bytes: ByteArray
                val cost = measureTimeMillis { bytes = it.readBytes() }
                context.event?.dispatch(NormalEvent.ReadAllFromStream(request, bytes.size, cost))

                Ok(ByteArrayInputStream(bytes))
            }
        } catch (e: Exception) {
            logger.error(e) { "readBytes error" }
            Err(StreamProcessorError.AudioSource(e.cause ?: e))
        }
    }

    private suspend fun readProcessedData(
        processor: AudioProcessingPipeline,
        callback: IPcmAudioCallback,
    ) {
        val outBuffer = processor.output
        val bytes = ByteArray(outBuffer.remaining())
        outBuffer.get(bytes)
        callback.onPcmData(bytes)
    }

    override suspend fun destroy() {
        _decoder?.destroy()
        _decoder = null
    }
}