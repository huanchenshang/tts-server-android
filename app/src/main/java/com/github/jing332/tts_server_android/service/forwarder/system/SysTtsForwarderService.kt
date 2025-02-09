@file:Suppress("OVERRIDE_DEPRECATION")

package com.github.jing332.tts_server_android.service.forwarder.system

import android.speech.tts.TextToSpeech
import com.github.jing332.common.LogLevel
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.server.forwarder.Engine
import com.github.jing332.server.forwarder.SystemTtsForwardServer
import com.github.jing332.server.forwarder.TtsParams
import com.github.jing332.server.forwarder.Voice
import com.github.jing332.tts.CachedEngineManager
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts.speech.local.LocalTtsService
import com.github.jing332.tts_server_android.App
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.SystemTtsForwarderConfig
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.help.LocalTtsEngineHelper
import com.github.jing332.tts_server_android.service.forwarder.AbsForwarderService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import java.io.OutputStream

class SysTtsForwarderService(
    override val port: Int = SystemTtsForwarderConfig.port.value,
    override val isWakeLockEnabled: Boolean = SystemTtsForwarderConfig.isWakeLockEnabled.value
) :
    AbsForwarderService(
        "SysTtsForwarderService",
        id = 1221,
        actionLog = ACTION_ON_LOG,
        actionStarting = ACTION_ON_STARTING,
        actionClosed = ACTION_ON_CLOSED,
        notificationChanId = "systts_forwarder_status",
        notificationChanTitle = R.string.forwarder_systts,
        notificationIcon = R.drawable.ic_baseline_compare_arrows_24,
        notificationTitle = R.string.forwarder_systts,
    ) {
    companion object {
        const val TAG = "SysTtsServerService"
        const val ACTION_ON_CLOSED = "ACTION_ON_CLOSED"
        const val ACTION_ON_STARTING = "ACTION_ON_STARTING"
        const val ACTION_ON_LOG = "ACTION_ON_LOG"

        val isRunning: Boolean
            get() = instance?.isRunning == true

        var instance: SysTtsForwarderService? = null
    }

    private var mServer: SystemTtsForwardServer? = null
    private var mLocalTTS: LocalTtsService? = null
    private val mLocalTtsHelper by lazy { LocalTtsEngineHelper(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    private fun getEngine(name: String): LocalTtsService {
        val cacheEngine = CachedEngineManager.getEngine(
            this@SysTtsForwarderService,
            LocalTtsSource(engine = name)
        ) ?: throw IllegalArgumentException("Engine not found: $name")
        return cacheEngine as LocalTtsService
    }

    override fun initServer() {
    }

    override fun startServer() {
        mServer = SystemTtsForwardServer(port, object : SystemTtsForwardServer.Callback {
            override fun log(level: Int, message: String) {
                sendLog(level, message)
            }
            override suspend fun tts(output: OutputStream, params: TtsParams) {
                val source = LocalTtsSource(
                    engine = params.engine,
                    voice = params.voice,
                )
                val e: LocalTtsService =
                    CachedEngineManager.getEngine(this@SysTtsForwarderService, source)
                            as? LocalTtsService
                        ?: throw IllegalArgumentException("Engine not found: ${params.engine}")
                if (e.state is EngineState.Uninitialized)
                    runBlocking { e.onInit() }

                val stream = runBlocking {
                    e.getStream(
                        source = source,
                        params = SystemParams(
                            text = params.text,
                            speed = (params.speed + 100) / 100f,
                            pitch = params.pitch / 100f
                        )
                    )
                }

                stream.use {
                    it.copyTo(output)
                }
            }

            override suspend fun voices(engine: String): List<Voice> {
                val ok = mLocalTtsHelper.setEngine(engine)
                if (!ok) throw IllegalStateException(getString(R.string.systts_engine_init_failed_timeout))

                return mLocalTtsHelper.voices.map {
                    Voice(
                        name = it.name,
                        locale = it.locale.toLanguageTag(),
                        localeName = it.locale.getDisplayName(it.locale),
                        features = it.features?.toList()
                    )
                }
            }

            override suspend fun engines(): List<Engine> =
                getSysTtsEngines().map { Engine(name = it.name, it.label) }


        }).apply { start(wait = true) }
    }

    override fun closeServer() {
        mServer?.let {
            it.stop()
            mLocalTTS?.onDestroy()
            mLocalTTS = null
        }
    }

    private fun getSysTtsEngines(): List<TextToSpeech.EngineInfo> {
        val tts = TextToSpeech(App.context, null)
        val engines = tts.engines
        tts.shutdown()
        return engines
    }

}