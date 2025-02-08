package com.github.jing332.tts.speech.local

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.github.jing332.common.utils.SyncLock
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.source.LocalTtsParameter
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.tts.exception.EngineException
import com.github.jing332.tts.exception.EngineStopException
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.speech.ITtsService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class LocalTtsService(
    private val context: Context,
    private val engine: String
) : ITtsService<LocalTtsSource>() {
    companion object {
        const val TAG = "LocalTtsEngine"
        private val logger = KotlinLogging.logger { LocalTtsService::class.java.name }
    }

    private val saveDir by lazy { context.cacheDir.absolutePath + "/local_tts_audio" }

    private var tts: TextToSpeech? = null

    private val mWaiter = SyncLock()
    private var currentTaskId = ""

    private suspend fun initLocalEngine(engine: String) {
        if (state !is EngineState.Uninitialized)
            return

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
                mWaiter.cancel()
            },
            engine
        )
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {

            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "tts done")
                if (utteranceId == currentTaskId) {
                    mWaiter.cancel()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
            }


        })
        mWaiter.await()
    }

    private fun stopLocalEngine() {
        tts?.stop()
        mWaiter.cancel(CancellationException(EngineStopException()))
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
            locale.let { language = Locale.forLanguageTag(it) }

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
    ): File = coroutineScope {
        currentTaskId = SystemClock.elapsedRealtime().toString()

        File(saveDir).apply { if (!exists()) mkdirs() }
        val file = File("$saveDir/$engine.wav")
        Log.d(TAG, "synthesizeToFile: ${file.absolutePath}")
        val bundle =
            setEnginePlayParams(tts!!, source.locale, source.voice, source.extraParams, params)
        tts?.synthesizeToFile(text, bundle, file, currentTaskId)
        try {
            mWaiter.await()
        } catch (e: CancellationException) {
            if (e.cause is EngineStopException)
                kotlin.runCatching {
                    file.delete()
                }
        }

        return@coroutineScope file
    }

    private suspend fun directPlay(
        source: LocalTtsSource,
        text: String,
        params: AudioParams,
    ) {
        currentTaskId = SystemClock.elapsedRealtime().toString()
        val bundle =
            setEnginePlayParams(tts!!, source.locale, source.voice, source.extraParams, params)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, currentTaskId)
        await()
    }

    private suspend fun await() {
        try {
            mWaiter.await()
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                stopLocalEngine()
            }
            throw e
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