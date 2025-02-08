package com.github.jing332.tts

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jing332.database.entities.systts.IConfiguration
import com.github.jing332.tts.manager.TtsConfiguration
import com.github.jing332.database.entities.systts.source.MsTtsSource
import kotlinx.serialization.encodeToString

import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class DataStructJsonTest {
    @Test
    fun test() {
//        val json = SerializerManager.json
//
//        val cfg: IConfiguration = TtsConfiguration(source = MsTtsSource())
//        val cfgStr = json.encodeToString(cfg)
//        println(cfgStr)
    }
}