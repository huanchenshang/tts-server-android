package com.github.jing332.tts_server_android

import android.content.Intent
import androidx.annotation.Keep
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import com.github.jing332.common.LogEntry
import com.github.jing332.common.toLogLevel
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.constant.KeyConst
import com.github.jing332.tts_server_android.service.systts.SystemTtsService

@Keep
class SysttsFilter : Filter<ILoggingEvent>() {
    companion object{
        const val TAG = "SysttsFilter"
        const val ACTION_ON_LOG = "SystemFilter.SYSTTS_ON_LOG"

    }
    override fun decide(event: ILoggingEvent): FilterReply {

        return if (event.loggerName == SystemTtsService.TAG) {
            val intent =
                Intent(ACTION_ON_LOG).putExtra(
                    KeyConst.KEY_DATA,
                    event.formattedMessage
                )
            AppConst.localBroadcast.sendBroadcast(intent)

            FilterReply.ACCEPT
        } else FilterReply.DENY
    }
}