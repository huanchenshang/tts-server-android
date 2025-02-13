package com.github.jing332.tts

import com.github.jing332.tts.manager.ITtsRequester
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.manager.TtsConfiguration
import com.github.jing332.tts.speech.EngineState
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

class TtsRequester(
    var context: ManagerContext,
) : ITtsRequester {

    override suspend fun request(
        params: SystemParams, tts: TtsConfiguration
    ): Result<ITtsRequester.Result, RequesterError> {
        val engine =
            CachedEngineManager.getEngine(context.androidContext, tts.source) ?: return Err(
                InitializationError("engine ${tts.source} not found")
            )
        if (engine.state != EngineState.Initialized) {
            engine.onInit()
        }

        return if (engine.isSyncPlay(tts.source)) {
            Ok(
                ITtsRequester.Result(
                    callback = ITtsRequester.ISyncPlayCallback {
                        engine.syncPlay(params, tts.source)
                    }
                )
            )
        } else {
            try {
                Ok(
                    ITtsRequester.Result(stream = engine.getStream(params, tts.source))
                )
            } catch (e: Exception) {
                Err(RequestError(e))
            }

        }
    }

    override fun destroy() {
    }

}