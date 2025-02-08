package com.github.jing332.tts

import android.os.SystemClock
import com.github.jing332.tts.exception.LoadException
import com.github.jing332.tts.manager.ITtsRequester
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.manager.TtsConfiguration
import com.github.jing332.tts.manager.event.EventType
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout

class TtsRequester(
    var context: ManagerContext,
) : ITtsRequester {
    private suspend fun requestInternal(
        times: Int, maxTimes: Int, params: SystemParams, tts: TtsConfiguration
    ): Result<ITtsRequester.Result, RequesterError> = coroutineScope {
        assert(times > 0)
        if (times > maxTimes) return@coroutineScope Err(TimesLimitExceeded)

        context.eventListener?.onEvent(EventType.Request(params, tts, times))
        val engine = CachedEngineManager.getEngine(context.androidContext, tts.source)
            ?: throw LoadException("TTS engine not found: $tts")
        if (engine.state is com.github.jing332.tts.speech.EngineState.Uninitialized) {
            engine.onInit()
        }

        return@coroutineScope try {
            if (engine.isSyncPlay(tts.source)) {
                Ok(ITtsRequester.Result(callback = ITtsRequester.ISyncPlayCallback {
                    engine.syncPlay(params, tts.source)
                }))
            } else withTimeout(context.cfg.requestTimeout) {
                val time = System.currentTimeMillis()
                val stream = engine.getStream(params, tts.source)
                context.eventListener?.onEvent(
                    EventType.RequestSuccess(
                        timeCost = System.currentTimeMillis() - time,
                        params = params,
                        config = tts
                    )
                )
                Ok(ITtsRequester.Result(stream = stream))
            }
        } catch (e: Exception) {
            val event: EventType =
                if (e is TimeoutCancellationException) EventType.RequestTimeout(params, tts)
                else EventType.RequestError(params, tts, e)
            context.eventListener?.onEvent(event)

            // return
            if (tts.standbyInfo?.config != null && tts.standbyInfo.tryTimesWhenTrigger + 1 > times) {
                context.eventListener?.onEvent(EventType.StandbyTts(params, tts))
                requestInternal(times + 1, maxTimes, params, tts.standbyInfo.config)
            } else {
                requestInternal(times + 1, maxTimes, params, tts)
            }
        }
    }

    override suspend fun request(
        params: SystemParams, tts: TtsConfiguration
    ): Result<ITtsRequester.Result, RequesterError> =
        requestInternal(1, context.cfg.maxRetryTimes + 1, params, tts)

    override fun onDestroy() {
    }

}