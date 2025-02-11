package com.github.jing332.tts

import android.content.Context
import com.github.jing332.database.entities.systts.source.ITtsSource
import com.github.jing332.tts.speech.ITtsService
import io.github.oshai.kotlinlogging.KotlinLogging

object CachedEngineManager :
    AbstractCachedManager<String, ITtsService<ITtsSource>>(
        timeout = 1000L * 60L * 10L, // 10 min
        delay = 1000L * 60 // 1 min
    ) {
    private val logger = KotlinLogging.logger("CachedEngineManager")

    override fun onCacheRemove(key: String, value: ITtsService<ITtsSource>): Boolean {
        logger.atDebug { message = "Engine timeout destroy: $key" }
        value.onDestroy()

        return super.onCacheRemove(key, value)
    }

    fun getEngine(context: Context, source: ITtsSource): ITtsService<ITtsSource>? {
        val key = source.getKey() + ";" + source.javaClass.simpleName

        val cachedEngine = cache[key]
        return if (cachedEngine == null) {
            val engine = TtsFactory.createEngine(context, source) ?: return null
            cache.put(key, engine)
            engine
        } else {
            cachedEngine
        }

    }

    fun expireAll() {
        logger.atDebug { message = "Expire all cached engine" }
        cache.removeAll {
            it.onDestroy()
            true
        }
    }
}