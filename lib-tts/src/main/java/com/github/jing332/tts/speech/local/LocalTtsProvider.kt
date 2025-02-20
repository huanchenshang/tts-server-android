package com.github.jing332.tts.speech.local

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.source.LocalTtsParameter
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.tts.exception.EngineException
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.speech.TextToSpeechProvider
import com.github.jing332.tts.synthesizer.SystemParams
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.InputStream
import java.util.Locale
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class LocalTtsProvider(
    private val context: Context,
    private val engine: String
) : TextToSpeechProvider<LocalTtsSource>() {
    companion object {
        const val TAG = "LocalTtsService"
        private val logger = KotlinLogging.logger(TAG)
    }

    private val saveDir by lazy { context.cacheDir.absolutePath + "/local_tts_audio" }

    private var tts: TextToSpeech? = null

    private val mutex by lazy { Mutex() }
    private var mContinuation: Continuation<Unit>? = null
    private var currentTaskId = ""

    private suspend fun <R> withLock(block: suspend () -> R) = coroutineScope {
        mutex.withLock { block() }
    }

    private suspend fun initLocalEngine(engine: String) = withLock {
        if (state !is EngineState.Uninitialized)
            return@withLock

        var initContinuation: Continuation<Unit>? = null
        state = EngineState.Initializing
        tts = TextToSpeech(
            context,
            { status ->
                if (status == TextToSpeech.SUCCESS) {
                    state = EngineState.Initialized
                } else {
                    state =
                        EngineState.Uninitialized(reason = EngineException("Local TTS engine init failed"))
                }
                initContinuation?.resume(Unit)
            },
            engine
        )
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                logger.debug { "TextToSpeech onStart $utteranceId" }
            }

            override fun onDone(utteranceId: String?) {
                logger.debug { "TextToSpeech onDone $utteranceId" }
                if (utteranceId == currentTaskId) {
                    mContinuation?.resume(Unit)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
            }


        })

        suspendCancellableCoroutine<Unit> {
            initContinuation = it
        }
    }

    private fun stopLocalEngine() {
        tts?.stop()
        mContinuation?.context?.cancel()
    }

    private fun destroyLocalEngine() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun setEnginePlayParams(
        engine: TextToSpeech,
        locale: String,
        voice: String,
        extraParams: List<LocalTtsParameter>?,
        params: AudioParams,
    ): Bundle {
        engine.apply {
            if (locale.isNotEmpty())
                language = Locale.forLanguageTag(locale)

            if (voice.isNotEmpty())
                voices?.forEach {
                    if (it.name == voice) {
                        logger.debug { "setVoice: ${it.name}" }
                        this.voice = it
                    }
                }

            logger.debug { "setParams: $params" }
            setSpeechRate(params.speed)
            setPitch(params.pitch)
            return Bundle().apply {
                if (params.volume != LocalTtsSource.VOLUME_FOLLOW) {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, params.volume)
                }
                extraParams?.forEach { it.putValueFromBundle(this) }
            }
        }
    }

    suspend fun getAudioFile(
        source: LocalTtsSource,
        text: String,
        params: AudioParams,
    ): File = withLock {
        currentTaskId = SystemClock.elapsedRealtime().toString()

        File(saveDir).apply { if (!exists()) mkdirs() }
        val file = File("$saveDir/$engine.wav")
        logger.debug { "synthesizeToFile: $file" }
        val bundle =
            setEnginePlayParams(tts!!, source.locale, source.voice, source.extraParams, params)
        tts?.synthesizeToFile(text, bundle, file, currentTaskId)

        suspendCancellableCoroutine<Unit> {
            mContinuation = it
            it.invokeOnCancellation {
                kotlin.runCatching {
                    file.delete()
                }
            }
        }

        return@withLock file
    }

    private suspend fun directPlay(
        source: LocalTtsSource,
        text: String,
        params: AudioParams,
    ) = coroutineScope {
        currentTaskId = SystemClock.elapsedRealtime().toString()
        val bundle =
            setEnginePlayParams(tts!!, source.locale, source.voice, source.extraParams, params)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, currentTaskId)

        suspendCancellableCoroutine<Unit> {
            mContinuation = it

            it.invokeOnCancellation {
                stopLocalEngine()
            }
        }
    }


    private suspend fun init(source: LocalTtsSource, params: SystemParams): AudioParams {
        initLocalEngine(engine)
        val speed = if (source.speed == LocalTtsSource.SPEED_FOLLOW) params.speed else source.speed
        val pitch = if (source.pitch == LocalTtsSource.PITCH_FOLLOW) params.pitch else source.pitch
        val volume =
            if (source.volume == LocalTtsSource.VOLUME_FOLLOW) params.volume else source.volume

        return AudioParams(speed = speed, pitch = pitch, volume = volume)
    }


    override suspend fun getStream(params: SystemParams, source: LocalTtsSource): InputStream {
        return getAudioFile(source, params.text, init(source, params)).inputStream()
    }

    override suspend fun syncPlay(params: SystemParams, source: LocalTtsSource) {
        directPlay(source, params.text, init(source, params))
    }

    override var state: EngineState = EngineState.Uninitialized()

    override fun isSyncPlay(source: LocalTtsSource): Boolean {
        return source.isDirectPlayMode
    }


    override suspend fun onInit() {
        initLocalEngine(engine)
        state = EngineState.Initialized
    }

    override fun onStop() {
    }

    override fun onDestroy() {
        destroyLocalEngine()
    }
}