package com.github.jing332.tts_server_android.service.forwarder.ms

import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.help.config.AppConfig
import com.github.jing332.tts_server_android.help.config.ServerConfig
import com.github.jing332.tts_server_android.service.forwarder.AbsForwarderService
import com.github.jing332.tts_server_android.ui.AppLog
import tts_server_lib.LogCallback
import tts_server_lib.Tts_server_lib

@Suppress("DEPRECATION")
class MsTtsForwarderService(override val isWakeLockEnabled: Boolean = ServerConfig.isWakeLockEnabled) :
    AbsForwarderService(
        name = "MsTtsForwarderService",
        id = 1233,
        actionLog = ACTION_ON_LOG,
        actionStarting = ACTION_ON_STARTED,
        actionClosed = ACTION_ON_CLOSED,
        notificationChanId = "server_status",
        notificationChanTitle = R.string.forwarder_ms,
        notificationTitle = R.string.forwarder_ms,
        notificationIcon = R.drawable.ic_microsoft
    ) {
    companion object {
        const val ACTION_ON_STARTED = "ACTION_ON_STARTED"
        const val ACTION_ON_CLOSED = "ACTION_ON_CLOSED"
        const val ACTION_ON_LOG = "ACTION_ON_LOG"

        val isRunning: Boolean
            get() = instance?.isRunning ?: false

        var instance: MsTtsForwarderService? = null
    }

    override val port: Int = ServerConfig.port

    @Deprecated("Deprecated in Java")
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    @Deprecated("Deprecated in Java")
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun initServer() {
        val cb = LogCallback { level, msg ->
            sendLog(AppLog(level, msg))
        }
        Tts_server_lib.init(cb)
    }

    override fun startServer() {
        Tts_server_lib.runServer(
            ServerConfig.port.toLong(), ServerConfig.token, AppConfig.isEdgeDnsEnabled
        )
    }

    override fun closeServer() {
        Tts_server_lib.closeServer()
    }
}