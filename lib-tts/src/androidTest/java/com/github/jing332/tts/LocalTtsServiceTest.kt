package com.github.jing332.tts

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.speech.local.LocalTtsService
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalTtsServiceTest {
    @Test
    fun testCancelable() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val tts = LocalTtsService(context, "")
            tts.onInit()
            val file = tts.getAudioFile(
                source = LocalTtsSource(),
                text = "这是中文语音合成示例啊啊啊",
                params = AudioParams()
            )
            println("size: ${file.readBytes().size}")

            repeat(1) {
                val job = launch {
                    val stream = tts.getStream(
                        SystemParams(text = "直接播放测试: 你好开发者，这是中文语音合成示例"),
                        LocalTtsSource()
                    )

                    println(stream.readBytes().size)
                }

//                delay(2500)
//                job.cancel()
            }
        }
    }
}