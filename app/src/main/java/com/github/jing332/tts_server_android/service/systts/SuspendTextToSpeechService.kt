package com.github.jing332.tts_server_android.service.systts

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeechService
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation

abstract class SuspendTextToSpeechService : TextToSpeechService() {

    private var job: Job = Job()
    override fun onStop() {
        if (job.isActive) job.cancel()
    }

    protected abstract suspend fun onSynthesizeTextSuspend(
        request: SynthesisRequest,
        callback: SynthesisCallback
    )

    @Synchronized
    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        job = Job()
        runBlocking(CoroutineName(Thread.currentThread().name) + job) {
            onSynthesizeTextSuspend(request, callback)
        }
    }

}