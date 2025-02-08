package com.github.jing332.tts

import com.github.jing332.tts.manager.AbstractTtsManager
import com.github.jing332.tts.manager.IBgmPlayer
import com.github.jing332.tts.manager.IResultProcessor
import com.github.jing332.tts.manager.ITextProcessor
import com.github.jing332.tts.manager.ITtsRepository
import com.github.jing332.tts.manager.ITtsRequester
import io.github.oshai.kotlinlogging.KotlinLogging
import splitties.init.appCtx

open class TtsManagerImpl(
    final override val context: ManagerContext
) : AbstractTtsManager() {
    override var textProcessor: ITextProcessor = TextProcessor(context)
    override var ttsRequester: ITtsRequester = TtsRequester(context)
    override var resultProcessor: IResultProcessor = ResultProcessor(context)
    override var repo: ITtsRepository = TtsRepository(context)
    override var bgmPlayer: IBgmPlayer = BgmPlayer(context)

    companion object {
        val global by lazy {
            val logger = KotlinLogging.logger("TtsManager")
            TtsManagerImpl(
                ManagerContext(
                    androidContext = appCtx,
                    logger = logger,
                    cfg = TtsManagerConfig()
                )
            )
        }
    }
}