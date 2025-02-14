package com.github.jing332.tts.manager

import com.github.jing332.common.utils.StringUtils
import com.github.jing332.tts.ConfigType
import com.github.jing332.tts.ManagerContext
import com.github.jing332.tts.error.RequesterError
import com.github.jing332.tts.error.SynthesisError
import com.github.jing332.tts.error.TextProcessorError
import com.github.jing332.tts.manager.event.ErrorEvent
import com.github.jing332.tts.manager.event.Event
import com.github.jing332.tts.manager.event.NormalEvent
import com.github.jing332.tts.speech.EmptyInputStream
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import kotlin.math.pow

abstract class AbstractTtsManager() : ITtsManager {
    private val logger: KLogger
        get() = context.logger


    abstract val context: ManagerContext

    abstract val textProcessor: ITextProcessor
    abstract val ttsRequester: ITtsRequester
    abstract val streamProcessor: IResultProcessor
    abstract val repo: ITtsRepository
    abstract val bgmPlayer: IBgmPlayer

    var isInitialized: Boolean = false
        private set

    private var maxSampleRate: Int = 16000
    private var mAllTts: Map<Long, TtsConfiguration> = mapOf()
        set(value) {
            field = value
            maxSampleRate =
                mAllTts.values.maxByOrNull { it.audioFormat.sampleRate }?.audioFormat?.sampleRate
                    ?: 16000
        }

    private fun event(event: Event) {
        context.event?.dispatch(event)
    }

    /**
     * @return null means presetConfigId is not found from database
     */
    private suspend fun textProcess(
        channel: Channel<ChannelPayload>,
        params: SystemParams,
        presetConfigId: Long?,
    ): List<TextSegment>? {
        textProcessor
            .process(params.text, presetConfigId)
            .onSuccess { list -> return list.filterNot { StringUtils.isSilent(it.text) } }
            .onFailure { err: TextProcessorError ->
                event(ErrorEvent.TextProcessor(err))
                if (err is TextProcessorError.MissingConfig && err.type == ConfigType.PRESET)
                    return null

            }

        return emptyList()
    }

    /**
     * @return null means request failed, [EmptyInputStream] means direct play
     *
     */
    private suspend fun requestInternal(
        request: RequestPayload,
        playCallback: suspend (ITtsRequester.ISyncPlayCallback) -> Unit,
    ): InputStream? {
        val result = withTimeoutOrNull(context.cfg.requestTimeout()) {
            ttsRequester.request(request.params, request.config)
        }
        if (result == null) { // timed out
            event(ErrorEvent.RequestTimeout(request))
            return null
        }

        result.onSuccess { resp ->
            resp.onCallback {
                playCallback(it)
                return EmptyInputStream
            }.onStream { ins ->
                return ins
            }
        }.onFailure {
            when (it) {
                is RequesterError.RequestError -> {
                    event(ErrorEvent.Request(request, it.error))
                }

                is RequesterError.StateError -> {
                    event(ErrorEvent.Request(request, IllegalStateException(it.message)))
                }
            }

            return null
        }

        return null
    }


    private suspend fun requestAndProcess(
        channel: Channel<ChannelPayload>,
        params: SystemParams,
        config: TtsConfiguration,
        retries: Int = 0,
        maxRetries: Int = context.cfg.maxRetryTimes(),
    ) {
        val request = RequestPayload(params, config)
        suspend fun retry() {
            return if (config.standbyConfig != null && context.cfg.toggleTry() > retries) {
                event(NormalEvent.StandbyTts(request.copy(config = config.standbyConfig)))
                requestAndProcess(channel, params, config.standbyConfig, 0, maxRetries)
            } else {
                val next = retries + 1
                // 2^[next] * 100ms
                val ms = Double.fromBits(2).pow(next.coerceAtMost(5)) * 100
                delay(ms.toLong())
                requestAndProcess(channel, params, config, next, maxRetries)
            }
        }

        if (retries > maxRetries) {
            event(NormalEvent.RequestCountEnded)
            return
        }

        logger.debug { "start request: $retries, ${params}, ${config}" }
        event(NormalEvent.Request(request, retries))

        val stream =
            requestInternal(request, playCallback = {
                logger.debug { "send direct play callback..." }
                channel.send(ChannelPayload.DirectPlayCallback(request, it))
            })

        if (stream == null) return retry() // request failed
        else if (stream is EmptyInputStream) return // direct play

        streamProcessor.processStream(
            stream,
            request,
            maxSampleRate,
            callback = { pcm -> channel.send(ChannelPayload.Bytes(pcm)) }
        ).onFailure { e ->
            event(ErrorEvent.ResultProcessor(request, e))
            return retry()
        }
    }

    private suspend fun internalSynthesize(
        params: SystemParams, callback: SynthesisCallback, presetConfigId: Long?,
    ): Result<Unit, SynthesisError> = coroutineScope {
        if (mAllTts.isEmpty()) {
            return@coroutineScope Err(SynthesisError.ConfigEmpty)
        }
        logger.debug { "onSynthesizeStart: sampleRate=${maxSampleRate}" }
        callback.onSynthesizeStart(maxSampleRate)

        val channel = Channel<ChannelPayload>(Channel.UNLIMITED)
        launch(Dispatchers.IO + CoroutineName("TtsManager")) {
            val list = textProcess(channel, params, presetConfigId)
            if (list == null)
                channel.send(ChannelPayload.NotFoundPresetConfig)
            else
                for (fragment in list) {
                    requestAndProcess(channel, params.copy(text = fragment.text), fragment.tts)
                }

            logger.debug { "channel.close()..." }
            channel.close()
        }

        try {
            for (payload in channel) {
                when (payload) {
                    is ChannelPayload.Bytes -> callback.onSynthesizeAvailable(payload.data)

                    is ChannelPayload.DirectPlayCallback -> try {
                        event(NormalEvent.DirectPlay(payload.request))
                        payload.callback.play()
                    } catch (e: Exception) {
                        event(ErrorEvent.DirectPlay(payload.request, e))
                    } finally {
                        logger.debug { "direct play done" }
                    }

                    is ChannelPayload.NotFoundPresetConfig -> {
                        return@coroutineScope Err(SynthesisError.NotFoundPresetConfig)
                    }

                    else -> logger.error { "unknown data: $payload" }
                }
            }
        } finally {
            logger.debug { "channel closed" }
        }

        Ok(Unit)
    }

    private val mutex = Mutex()

    override val isSynthesizing: Boolean
        get() = mutex.isLocked


    override suspend fun synthesize(
        params: SystemParams, forceConfigId: Long?, callback: SynthesisCallback,
    ): Result<Unit, SynthesisError> = mutex.withLock {
        logger.atTrace {
            message = "synthesize"
            payload = mapOf(
                "forceConfigId" to forceConfigId,
                "params" to params,
            )
        }

        bgmPlayer.play()
        try {
            internalSynthesize(params, callback, forceConfigId)
        } finally {
            logger.debug { "synthesize done" }
            bgmPlayer.stop()
        }
    }

    override suspend fun init() = mutex.withLock {
        try {
            repo.init()
        } catch (e: Exception) {
            event(ErrorEvent.Repository(e))
            return@withLock
        }

        mAllTts = repo.getAllTts()
        if (mAllTts.isEmpty()) {
            event(ErrorEvent.ConfigEmpty)
            return@withLock
        }

        textProcessor.init(context.androidContext, mAllTts).onFailure {
            event(ErrorEvent.TextProcessor(it))
            return@withLock
        }

        streamProcessor.init(context.androidContext)

        val bgmList = mutableListOf<BgmSource>()
        try {
            repo.getAllBgm().forEach { bgm ->
                bgm.musicList.forEach {
                    bgmList.add(BgmSource(path = it, volume = bgm.volume))
                }
            }
            bgmPlayer.setPlayList(list = bgmList)
        } catch (e: Exception) {
            event(ErrorEvent.BgmLoading(e))
            return@withLock
        }

        isInitialized = true
    }


    override suspend fun destroy() = mutex.withLock {
        isInitialized = false
        repo.destroy()
        ttsRequester.destroy()
        streamProcessor.destroy()
        bgmPlayer.destroy()
    }

    sealed interface ChannelPayload {
        data class Bytes(val data: ByteArray) : ChannelPayload
        data class DirectPlayCallback(
            val request: RequestPayload,
            val callback: ITtsRequester.ISyncPlayCallback,
        ) : ChannelPayload

        data object NotFoundPresetConfig : ChannelPayload
    }
}