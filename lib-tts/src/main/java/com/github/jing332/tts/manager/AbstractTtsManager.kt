package com.github.jing332.tts.manager

import com.github.jing332.tts.AudioDecodingError
import com.github.jing332.tts.AudioSourceError
import com.github.jing332.tts.ConfigEmptyError
import com.github.jing332.tts.GetBgmError
import com.github.jing332.tts.InitializationError
import com.github.jing332.tts.ManagerContext
import com.github.jing332.tts.RepoInitError
import com.github.jing332.tts.RequestError
import com.github.jing332.tts.TtsError
import com.github.jing332.tts.manager.event.EventType
import com.github.jing332.tts.manager.event.IEventListener
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayInputStream
import java.io.InputStream

abstract class AbstractTtsManager() : ITtsManager {
    private val logger: KLogger
        get() = context.logger

    private val eventListener: IEventListener?
        get() = context.eventListener


    abstract val context: ManagerContext

    abstract val textProcessor: ITextProcessor
    abstract val ttsRequester: ITtsRequester
    abstract val resultProcessor: IResultProcessor
    abstract val repo: ITtsRepository
    abstract val bgmPlayer: IBgmPlayer

    private var isInitialized: Boolean = false
    private var maxSampleRate: Int = 16000
    private var mAllTts: Map<Long, TtsConfiguration> = mapOf()
        set(value) {
            field = value
            maxSampleRate =
                mAllTts.values.maxByOrNull { it.audioFormat.sampleRate }?.audioFormat?.sampleRate
                    ?: 16000
        }

    /**
     * @return true if error
     */
    private inline fun <R> catchError(message: () -> String, block: () -> R): Boolean {
        try {
            block()
            return false
        } catch (e: Throwable) {
            onEvent(EventType.Error(Exception(message())))
        }
        return true
    }

    private fun onEvent(event: EventType) {
        eventListener?.onEvent(event)
    }

    private suspend fun textProcess(
        channel: Channel<Any>,
        params: SystemParams,
        forceConfigId: Long?
    ): List<TextSegment> {
        val processRet = textProcessor.process(params.text, forceConfigId)
        processRet.onSuccess { list ->
            return list
        }.onFailure { err ->
            channel.send(err)
        }

        return emptyList()
    }

    private suspend fun requestAndProcess(
        channel: Channel<Any>,
        params: SystemParams,
        config: TtsConfiguration,

        retries: Int = 0,
        maxRetries: Int = context.cfg.maxRetryTimes(),
    ) {
        suspend fun retry() {
            return if (config.standbyInfo?.config != null && config.standbyInfo.tryTimesWhenTrigger > retries) {
                onEvent(EventType.StandbyTts(params, config.standbyInfo.config))
                requestAndProcess(
                    channel,
                    params,
                    config.standbyInfo.config,
                    retries + 1,
                    maxRetries
                )
            } else
                requestAndProcess(channel, params, config, retries + 1, maxRetries)
        }

        if (retries > maxRetries) {
            onEvent(EventType.RequestCountEnded)
            return
        }

        onEvent(EventType.Request(params, config, retries))

        val time = System.currentTimeMillis()
        val result = withTimeoutOrNull(context.cfg.requestTimeout()) {
            ttsRequester.request(params, config)
        }
        if (result == null) { // timed out
            onEvent(EventType.RequestTimeout(params, config))
            return retry()
        }

        result.onSuccess { ret ->
            ret.onStream { stream ->
                var size: Int = 0
                val niceStream: InputStream =
                    if (context.cfg.streamPlayEnabled()) stream
                    else stream.use {
                        val bytes = it.readBytes()
                        size = bytes.size
                        ByteArrayInputStream(bytes)
                    }

                onEvent(
                    EventType.RequestSuccess(
                        timeCost = System.currentTimeMillis() - time,
                        size = size,
                        params = params,
                        config = config
                    )
                )

                resultProcessor.processStream(
                    ins = niceStream,
                    tts = config,
                    targetSampleRate = maxSampleRate,
                    callback = { pcmAudio -> channel.send(pcmAudio) }
                ).onFailure { e ->
                    when (e) {
                        is AudioDecodingError -> {
                            onEvent(
                                EventType.AudioDecodingError(params, config, e.cause)
                            )
                        }

                        is AudioSourceError -> onEvent(
                            EventType.AudioSourceError(params, config, e.cause)
                        )
                    }

                    return retry()
                }
            }.onCallback {
                channel.send(
                    DirectPlayCallbackWithConfig(
                        fragment = TextSegment(params.text, config), callback = it
                    )
                )
            }
        }.onFailure {
            when (it) {
                is RequestError -> {
                    onEvent(EventType.RequestError(params, config, it.cause))
                }

                is InitializationError -> {
                    onEvent(
                        EventType.RequestError(
                            params,
                            config,
                            Exception(it.reason)
                        )
                    )
                }
            }
            return retry()
        }
    }

    private suspend fun internalSynthesize(
        params: SystemParams, callback: SynthesisCallback, forceConfigId: Long?
    ): Result<Unit, TtsError> = coroutineScope {
        if (mAllTts.isEmpty()) {
            return@coroutineScope Err(ConfigEmptyError)
        }
        callback.onSynthesizeStart(maxSampleRate)

        val channel = Channel<Any>(Channel.UNLIMITED)
        launch(Dispatchers.IO + CoroutineName("TtsManager")) {
            val list = textProcess(channel, params, forceConfigId)
            for (fragment in list) {
                requestAndProcess(channel, params.copy(text = fragment.text), fragment.tts)
            }
            logger.debug { "channel.close()..." }
            channel.close()
        }

        for (data in channel) {
            when (data) {
                is ByteArray -> {
                    callback.onSynthesizeAvailable(data)
                }

                is DirectPlayCallbackWithConfig -> try {
                    onEvent(EventType.DirectPlay(data.fragment))
                    data.callback.play()
                } catch (e: Exception) {
                    onEvent(EventType.DirectPlayError(data.fragment, e))
                } finally {
                    logger.debug { "direct play done" }
                }

                is TtsError -> {
                    return@coroutineScope Err(data)
                }
            }
        }
        logger.debug { "channel closed" }

        Ok(Unit)
    }

    private val mutex = Mutex()

    override val isSynthesizing: Boolean
        get() = mutex.isLocked


    override suspend fun synthesize(
        params: SystemParams, forceConfigId: Long?, callback: SynthesisCallback
    ): Result<Unit, TtsError> = mutex.withLock {
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
            bgmPlayer.stop()
        }
    }

    override suspend fun  init(): Result<Unit, TtsError> = mutex.withLock {
        try {
            repo.init()
        } catch (e: Exception) {
            return Err(RepoInitError(e))
        }

        mAllTts = repo.getAllTts()
        if (mAllTts.isEmpty()) return Err(ConfigEmptyError)

        textProcessor.init(context.androidContext, mAllTts).onFailure {
            return Err(it)
        }

        val bgmList = mutableListOf<BgmSource>()
        try {
            repo.getAllBgm().forEach { bgm ->
                bgm.musicList.forEach {
                    bgmList.add(BgmSource(path = it, volume = bgm.volume))
                }
            }
        } catch (e: Exception) {
            return Err(GetBgmError(e))
        }

        bgmPlayer.setPlayList(list = bgmList)
        isInitialized = true
        return Ok(Unit)
    }


    override suspend fun destroy() {
        repo.destroy()
        ttsRequester.destroy()
        bgmPlayer.destroy()
    }


    class DirectPlayCallbackWithConfig(
        val fragment: TextSegment,
        val callback: ITtsRequester.ISyncPlayCallback,
    )
}