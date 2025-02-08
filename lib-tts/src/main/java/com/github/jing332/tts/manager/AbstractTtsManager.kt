package com.github.jing332.tts.manager

import com.github.jing332.tts.ConfigEmptyError
import com.github.jing332.tts.GetBgm
import com.github.jing332.tts.ManagerContext
import com.github.jing332.tts.TimesLimitExceeded
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

    private suspend fun textProcess(
        channel: Channel<Any>,
        params: SystemParams,
        forceConfigId: Long?
    ): List<TextFragment> {
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
        config: TtsConfiguration
    ) {
        ttsRequester.request(params, config)
            .onSuccess { ret ->
                ret.onStream {
                    try {
                        resultProcessor.processStream(
                            ins = it,
                            tts = config,
                            targetSampleRate = maxSampleRate
                        ) { pcmAudio -> channel.send(pcmAudio) }
                    } catch (e: Exception) {
                        eventListener?.onEvent(EventType.ResultProcessorError(params, config, e))
                    }
                }.onCallback {
                    channel.send(
                        DirectPlayCallbackWithConfig(
                            fragment = TextFragment(params.text, config), callback = it
                        )
                    )
                }
            }.onFailure {
                if (it is TimesLimitExceeded) {
                    eventListener?.onEvent(EventType.TimesEnded)
                }
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
        val fragment: TextFragment,
        val callback: ITtsRequester.ISyncPlayCallback,
    )
}