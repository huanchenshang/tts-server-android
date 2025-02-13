package com.github.jing332.tts_server_android.service.systts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioFormat
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import androidx.core.content.ContextCompat
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.registerGlobalReceiver
import com.github.jing332.common.utils.runOnUI
import com.github.jing332.common.utils.sizeToReadable
import com.github.jing332.common.utils.startForegroundCompat
import com.github.jing332.common.utils.toHtmlBold
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts.ConfigEmptyError
import com.github.jing332.tts.ForceConfigIdNotFound
import com.github.jing332.tts.TtsManagerConfig
import com.github.jing332.tts.TtsManagerImpl
import com.github.jing332.tts.manager.ITtsManager
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.manager.event.EventType
import com.github.jing332.tts.manager.event.IEventListener
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.MainActivity
import com.github.jing332.tts_server_android.conf.SysTtsConfig
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.constant.SystemNotificationConst
import com.github.jing332.tts_server_android.service.systts.help.TextProcessor
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale
import kotlin.system.exitProcess


@Suppress("DEPRECATION")
class SystemTtsService : TextToSpeechService(), IEventListener {
    companion object {
        const val TAG = "SystemTtsService"
        private val logger = KotlinLogging.logger(TAG)

        const val ACTION_UPDATE_CONFIG = "on_config_changed"
        const val ACTION_UPDATE_REPLACER = "on_replacer_changed"

        const val ACTION_NOTIFY_CANCEL = "SYS_TTS_NOTIFY_CANCEL"
        const val ACTION_NOTIFY_KILL_PROCESS = "SYS_TTS_NOTIFY_EXIT_0"
        const val NOTIFICATION_CHAN_ID = "system_tts_service"

        const val DEFAULT_VOICE_NAME = "DEFAULT_默认"

        /**
         * 更新配置
         */
        fun notifyUpdateConfig(isOnlyReplacer: Boolean = false) {
            if (isOnlyReplacer)
                AppConst.localBroadcast.sendBroadcast(Intent(ACTION_UPDATE_REPLACER))
            else
                AppConst.localBroadcast.sendBroadcast(Intent(ACTION_UPDATE_CONFIG))
        }
    }

    private val mCurrentLanguage: MutableList<String> = mutableListOf("zho", "CHN", "")


    private val mTextProcessor = TextProcessor()
    private val mTtsManager: ITtsManager by lazy {
        TtsManagerImpl.global.apply {
            context.androidContext = this@SystemTtsService
            context.eventListener = this@SystemTtsService
            context.cfg = TtsManagerConfig(
                requestTimeout = SysTtsConfig::requestTimeout,
                maxRetryTimes = SysTtsConfig::maxRetryCount,
                streamPlayEnabled = SysTtsConfig::isStreamPlayModeEnabled,
                silenceSkipEnabled = SysTtsConfig::isSkipSilentAudio,
                bgmShuffleEnabled = SysTtsConfig::isBgmShuffleEnabled,
                bgmVolume = SysTtsConfig::bgmVolume
            )
            textProcessor = mTextProcessor
        }
    }

    private val mNotificationReceiver: NotificationReceiver by lazy { NotificationReceiver() }
    private val mLocalReceiver: LocalReceiver by lazy { LocalReceiver() }

    private val mScope = CoroutineScope(Job())

    // WIFI 锁
    private val mWifiLock by lazy {
        val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "tts-server:wifi_lock")
    }

    // 唤醒锁
    private var mWakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()

        registerGlobalReceiver(
            listOf(ACTION_NOTIFY_KILL_PROCESS, ACTION_NOTIFY_CANCEL), mNotificationReceiver
        )

        AppConst.localBroadcast.registerReceiver(
            mLocalReceiver,
            IntentFilter(ACTION_UPDATE_CONFIG)
        )

        if (SysTtsConfig.isWakeLockEnabled)
            mWakeLock = (getSystemService(POWER_SERVICE) as PowerManager).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "tts-server:wake_lock"
            )

        mWakeLock?.acquire(60 * 20 * 100)
        mWifiLock.acquire()

        initManager()
    }

    fun initManager() {
        mScope.launch {
            mTtsManager.init().onFailure {
                when (it) {
                    ConfigEmptyError -> {
                        getString(R.string.config_empty_error).let {
                            longToast(it)
                            logE(it)
                        }
                    }

                    else -> {
                        logE("init failed: $it")
                        longToast("init failed: $it")
                    }
                }
            }
        }
    }

    fun loadReplacer() {
        mTextProcessor.loadReplacer()
    }


    override fun onDestroy() {
        super.onDestroy()

        logger.debug { "service destroy" }

        mScope.launch {
            mTtsManager.destroy()
        }
        unregisterReceiver(mNotificationReceiver)
        AppConst.localBroadcast.unregisterReceiver(mLocalReceiver)

        mWakeLock?.release()
        mWifiLock.release()

        stopForeground(/* removeNotification = */ true)
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        return if (Locale.SIMPLIFIED_CHINESE.isO3Language == lang || Locale.US.isO3Language == lang) {
            if (Locale.SIMPLIFIED_CHINESE.isO3Country == country || Locale.US.isO3Country == country) TextToSpeech.LANG_COUNTRY_AVAILABLE else TextToSpeech.LANG_AVAILABLE
        } else TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onGetLanguage(): Array<String> {
        return mCurrentLanguage.toTypedArray()
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        val result = onIsLanguageAvailable(lang, country, variant)
        mCurrentLanguage.clear()
        mCurrentLanguage.addAll(
            mutableListOf(
                lang.toString(),
                country.toString(),
                variant.toString()
            )
        )

        return result
    }

    override fun onGetDefaultVoiceNameFor(
        lang: String?,
        country: String?,
        variant: String?
    ): String {
        return DEFAULT_VOICE_NAME
    }


    override fun onGetVoices(): MutableList<Voice> {
        val list =
            mutableListOf(Voice(DEFAULT_VOICE_NAME, Locale.getDefault(), 0, 0, true, emptySet()))

        dbm.systemTtsV2.getAllGroupWithTts().forEach { groups ->
            groups.list.forEach { it ->
                if (it.config is TtsConfigurationDTO) {
                    val tts = (it.config as TtsConfigurationDTO).source

                    list.add(
                        Voice(
                            /* name = */ "${it.displayName}_${it.id}",
                            /* locale = */ Locale.forLanguageTag(tts.locale),
                            /* quality = */ 0,
                            /* latency = */ 0,
                            /* requiresNetworkConnection = */true,
                            /* features = */mutableSetOf<String>().apply {
                                add(it.order.toString())
                                add(it.id.toString())
                            }
                        )
                    )
                }

            }
        }

        return list
    }

    override fun onIsValidVoiceName(voiceName: String?): Int {
        val isDefault = voiceName == DEFAULT_VOICE_NAME
        if (isDefault) return TextToSpeech.SUCCESS

        val index =
            dbm.systemTtsV2.all.indexOfFirst { "${it.displayName}_${it.id}" == voiceName }

        return if (index == -1) TextToSpeech.ERROR else TextToSpeech.SUCCESS
    }

    override fun onStop() {
        logger.info { getString(R.string.cancel) }
        synthesizerJob?.cancel()
        synthesizerJob = null
        updateNotification(getString(R.string.systts_state_idle), "")
    }

    private lateinit var mCurrentText: String
    private var synthesizerJob: Job? = null
    private var mNotificationJob: Job? = null


    private fun getConfigIdFromVoiceName(voiceName: String): Result<Long?, Unit> {
        if (voiceName.isNotBlank()) {
            val voiceSplitList = voiceName.split("_")
            if (voiceSplitList.isEmpty()) {
                return Err(Unit)
            } else {
                voiceSplitList.getOrNull(voiceSplitList.size - 1)?.let { idStr ->
                    return Ok(idStr.toLongOrNull())
                }
            }
        }
        return Ok(null)
    }

    override fun onSynthesizeText(
        request: SynthesisRequest,
        callback: android.speech.tts.SynthesisCallback
    ) = runBlocking {
        mNotificationJob?.cancel()
        reNewWakeLock()
        startForegroundService()
        val text = request.charSequenceText.toString().trim()
        mCurrentText = text
        updateNotification(getString(R.string.systts_state_synthesizing), text)

        if (text.isBlank()) {
            callback.start(16000, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
        } else
            synthesizerJob = mScope.launch {
                // If the voiceName is not empty, get the configuration ID from the voiceName.
                var cfgId: Long? = getConfigIdFromVoiceName(request.voiceName ?: "").onFailure {
                    longToast(R.string.voice_name_bad_format)
                    callback.error(TextToSpeech.ERROR_INVALID_REQUEST)
                    return@launch
                }.value

                mTtsManager.synthesize(
                    params = SystemParams(text = request.charSequenceText.toString()),
                    forceConfigId = cfgId,
                    callback = object :
                        com.github.jing332.tts.manager.SynthesisCallback {
                        override fun onSynthesizeStart(sampleRate: Int) {
                            callback.start(
                                /* sampleRateInHz = */ sampleRate,
                                /* audioFormat = */ AudioFormat.ENCODING_PCM_16BIT,
                                /* channelCount = */ 1
                            )
                        }

                        override fun onSynthesizeAvailable(audio: ByteArray) {
                            writeToCallBack(callback, audio)
                        }

                    }
                ).onFailure {
                    logE("error: $it")
                    if (it is ForceConfigIdNotFound) {
                        longToast(getString(R.string.tts_config_not_exist))
                        callback.error(TextToSpeech.ERROR_INVALID_REQUEST)
                    }
                }
            }
        synthesizerJob?.join()
        callback.done()
        logger.debug { "done" }

        mNotificationJob = mScope.launch {
            delay(5000)
            stopForeground(true)
            mNotificationDisplayed = false
        }
    }

    private fun writeToCallBack(
        callback: android.speech.tts.SynthesisCallback,
        pcmData: ByteArray
    ) {
        try {
            val maxBufferSize: Int = callback.maxBufferSize
            var offset = 0
            while (offset < pcmData.size && mTtsManager.isSynthesizing) {
                val bytesToWrite = maxBufferSize.coerceAtMost(pcmData.size - offset)
                callback.audioAvailable(pcmData, offset, bytesToWrite)
                offset += bytesToWrite
            }
        } catch (e: Exception) {
            logE("writeToCallBack: ${e.toString()}")
        }
    }

    private fun reNewWakeLock() {
        if (mWakeLock != null && mWakeLock?.isHeld == false) {
            mWakeLock?.acquire(60 * 20 * 1000)
        }
    }

    private var mNotificationBuilder: Notification.Builder? = null
    private lateinit var mNotificationManager: NotificationManager

    // 通知是否显示中
    private var mNotificationDisplayed = false

    /* 启动前台服务通知 */
    private fun startForegroundService() {
        if (SysTtsConfig.isForegroundServiceEnabled && !mNotificationDisplayed) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val chan = NotificationChannel(
                    NOTIFICATION_CHAN_ID,
                    getString(R.string.systts_service),
                    NotificationManager.IMPORTANCE_NONE
                )
                chan.lightColor = Color.CYAN
                chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                mNotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                mNotificationManager.createNotificationChannel(chan)
            }
            startForegroundCompat(SystemNotificationConst.ID_SYSTEM_TTS, getNotification())
            mNotificationDisplayed = true
        }
    }

    /* 更新通知 */
    private fun updateNotification(title: String, content: String? = null) {
        if (SysTtsConfig.isForegroundServiceEnabled)
            runOnUI {
                mNotificationBuilder?.let { builder ->
                    content?.let {
                        val bigTextStyle =
                            Notification.BigTextStyle().bigText(it).setSummaryText("TTS")
                        builder.style = bigTextStyle
                        builder.setContentText(it)
                    }

                    builder.setContentTitle(title)
                    startForegroundCompat(
                        SystemNotificationConst.ID_SYSTEM_TTS,
                        builder.build()
                    )
                }
            }
    }

    /* 获取通知 */
    @Suppress("DEPRECATION")
    private fun getNotification(): Notification {
        val notification: Notification
        /*Android 12(S)+ 必须指定PendingIntent.FLAG_*/
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        /*点击通知跳转*/
        val pendingIntent =
            PendingIntent.getActivity(
                this, 1, Intent(
                    this,
                    MainActivity::class.java
                ).apply { /*putExtra(KEY_FRAGMENT_INDEX, INDEX_SYS_TTS)*/ }, pendingIntentFlags
            )

        val killProcessPendingIntent = PendingIntent.getBroadcast(
            this, 0, Intent(
                ACTION_NOTIFY_KILL_PROCESS
            ), pendingIntentFlags
        )
        val cancelPendingIntent =
            PendingIntent.getBroadcast(
                this,
                0,
                Intent(ACTION_NOTIFY_CANCEL),
                pendingIntentFlags
            )

        mNotificationBuilder = Notification.Builder(applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationBuilder?.setChannelId(NOTIFICATION_CHAN_ID)
        }
        notification = mNotificationBuilder!!
            .setSmallIcon(R.mipmap.ic_app_notification)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(this, R.color.md_theme_light_primary))
            .addAction(0, getString(R.string.kill_process), killProcessPendingIntent)
            .addAction(0, getString(R.string.cancel), cancelPendingIntent)
            .build()

        return notification
    }

    @Suppress("DEPRECATION")
    inner class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_NOTIFY_KILL_PROCESS -> { // 通知按钮{结束进程}
                    stopForeground(true)
                    exitProcess(0)
                }

                ACTION_NOTIFY_CANCEL -> { // 通知按钮{取消}
                    if (mTtsManager.isSynthesizing)
                        onStop() /* 取消当前播放 */
                    else /* 无播放，关闭通知 */ {
                        stopForeground(true)
                        mNotificationDisplayed = false
                    }
                }
            }
        }
    }

    inner class LocalReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_UPDATE_CONFIG -> mScope.launch { initManager() }
                ACTION_UPDATE_REPLACER -> mScope.launch { loadReplacer() }
            }
        }
    }

    private fun logD(msg: String) = logger.debug(msg)
    private fun logI(msg: String) = logger.info(msg)
    private fun logW(msg: String) = logger.warn(msg)
    private fun logE(msg: String) {
        updateNotification("⚠️ " + getString(R.string.error), msg)

        logger.error(msg)
    }

    override fun onEvent(event: EventType) {
        when (event) {
            is EventType.Request ->
                if (event.retries > 0)
                    logW(getString(R.string.systts_log_start_retry, event.retries))
                else
                    logI(
                        getString(
                            R.string.systts_log_request_audio,
                            event.params.text.toHtmlBold() + "<br>" + event.config.source.voice
                        )
                    )


            is EventType.DirectPlay -> logI(
                getString(
                    R.string.systts_log_direct_play,
                    event.fragment.text.toHtmlBold()
                )
            )

            is EventType.RequestSuccess -> logI(
                getString(
                    R.string.systts_log_success,
                    if (event.size > 0) event.size.sizeToReadable() else getString(R.string.unknown),
                    "${event.timeCost}ms"
                )
            )

            is EventType.RequestTimeout -> logW(
                getString(
                    R.string.failed_timed_out,
                    SysTtsConfig.requestTimeout
                )
            )

            is EventType.StandbyTts -> logI(
                getString(
                    R.string.systts_set_standby
                ) + event.tts
            )

            EventType.RequestCountEnded -> logW("到达重试上限，跳过！")

            is EventType.AudioSourceError -> logE("音频源错误: ${event.cause}")

            is EventType.AudioDecodingError -> logE("音频解码错误: ${event.cause}")

            is EventType.Error -> {
                logger.error(event.cause) { "EventType.Error" }
                logE(event.cause.toString())
            }
        }
    }

}