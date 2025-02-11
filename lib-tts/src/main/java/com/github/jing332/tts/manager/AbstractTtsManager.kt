package com.github.jing332.tts.manager

import com.github.jing332.tts.AudioDecodingError
import com.github.jing332.tts.AudioStreamError
import com.github.jing332.tts.ConfigEmptyError
import com.github.jing332.tts.GetBgm
import com.github.jing332.tts.InitializationError
import com.github.jing332.tts.ManagerContext
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
            eventListener?.onEvent(EventType.Error(Exception(message())))
        }
        return true
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
        maxRetries: Int = context.cfg.maxRetryTimes,
    ) {
        suspend fun retry() {
            return if (config.standbyInfo?.config != null && config.standbyInfo.tryTimesWhenTrigger > retries) {
                eventListener?.onEvent(EventType.StandbyTts(params, config))
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
            eventListener?.onEvent(EventType.RequestTimesEnded)
            return
        }

        val time = System.currentTimeMillis()
        val result = withTimeoutOrNull(context.cfg.requestTimeout) {
            ttsRequester.request(params, config)
        }
        if (result == null) { // timed out
            eventListener?.onEvent(EventType.RequestTimeout(params, config))
            return retry()
        }

        result.onSuccess { ret ->
            eventListener?.onEvent(
                EventType.RequestSuccess(
                    timeCost = System.currentTimeMillis() - time,
                    params = params,
                    config = config
                )
            )
            ret.onStream {
                resultProcessor.processStream(
                    ins = it,
                    tts = config,
                    targetSampleRate = maxSampleRate,
                    callback = { pcmAudio -> channel.send(pcmAudio) }
                ).onFailure { e ->
                    when (e) {
                        is AudioDecodingError -> {
                            eventListener?.onEvent(
                                EventType.AudioDecodingError(params, config, e.cause)
                            )
                        }

                        is AudioStreamError -> eventListener?.onEvent(
                            EventType.AudioStreamError(params, config, e.cause)
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
                    eventListener?.onEvent(EventType.RequestError(params, config, it.cause))
                }

                is InitializationError -> {
                    eventListener?.onEvent(
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
        params: SystemParams, callback: SynthesisCallback, forceConfigId: Long? = null
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
            channel.close()
        }

        for (data in channel) {
            when (data) {
                is ByteArray -> {
                    callback.onSynthesizeAvailable(data)
                }

                is DirectPlayCallbackWithConfig -> try {
                    eventListener?.onEvent(EventType.DirectPlay(data.fragment))
                    data.callback.play()
                } catch (e: Exception) {
                    eventListener?.onEvent(EventType.DirectPlayError(data.fragment, e))
                }

                is TtsError -> {
                    return@coroutineScope Err(data)
                }
            }
        }

        Ok(Unit)
    }

    private val mutex = Mutex()

    override val isSynthesizing: Boolean
        get() = mutex.isLocked

    override suspend fun synthesize(
        params: SystemParams, forceConfigId: Long?, callback: SynthesisCallback
    ): Result<Unit, TtsError> = mutex.withLock {
        bgmPlayer.onPlay()
        try {
            internalSynthesize(params, callback)
        } finally { // job cancelled
            bgmPlayer.onStop()
        }
    }

    override fun init(): Result<Unit, TtsError> {
        mAllTts = repo.getAllTts()
        if (mAllTts.isEmpty()) return Err(ConfigEmptyError)

        textProcessor.init(context.androidContext, mAllTts).onFailure {
            return Err(it)
        }

        val bgmList = mutableListOf<Pair<String, Float>>()
        try {
            repo.getAllBgm().forEach { bgm ->
                bgm.value.musicList.forEach { path ->
                    bgmList.add(path to bgm.value.volume / 1000f)
                }
            }
        } catch (e: Exception) {
            return Err(GetBgm(e))
        }

        bgmPlayer.setPlayList(list = bgmList)

        return Ok(Unit)
    }


    override fun destroy() {
        ttsRequester.onDestroy()
        bgmPlayer.onDestroy()
    }


    class DirectPlayCallbackWithConfig(
        val fragment: TextSegment,
        val callback: ITtsRequester.ISyncPlayCallback,
    )
}