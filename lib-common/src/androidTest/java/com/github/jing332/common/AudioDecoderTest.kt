package com.github.jing332.common

import androidx.media3.exoplayer.ExoPlaybackException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.drake.net.Net
import com.github.jing332.common.audio.exo.ExoAudioDecoder
import kotlinx.coroutines.runBlocking
import okhttp3.Response
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class AudioDecoderTest {

    class TestInputStream(ins: InputStream) : FilterInputStream(ins) {
        val random = Random(System.currentTimeMillis())
        fun randomThrowErr() {
            if (random.nextBoolean())
                throw RuntimeException("test throw error from stream")
        }

        override fun read(): Int {
            randomThrowErr()
            return super.read()
        }

        override fun read(b: ByteArray?, off: Int, len: Int): Int {
            randomThrowErr()
            return super.read(b, off, len)
        }

        override fun available(): Int {
            randomThrowErr()
            return super.available()
        }

    }

    @Test
    fun testExoDecoder() {
        val resp: Response = Net.get("https://download.samplelib.com/mp3/sample-3s.mp3").execute()
        assert(resp.isSuccessful)

        val bytes = resp.body!!.byteStream().readBytes()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            val decoder = ExoAudioDecoder(context)
            decoder.callback = object : ExoAudioDecoder.Callback {
                override fun onReadPcmAudio(byteBuffer: ByteBuffer) {
                    println(byteBuffer)
                }
            }
            repeat(2) {
                try {
                    decoder.doDecode(TestInputStream(ByteArrayInputStream(bytes)))
                } catch (e: ExoPlaybackException) {
                    e.printStackTrace()
                }
            }
        }
    }
}