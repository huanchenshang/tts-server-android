package com.github.jing332.tts

import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jing332.database.entities.systts.BaseAudioFormat
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.tts.manager.ITextProcessor
import com.github.jing332.tts.manager.ITtsRepository
import com.github.jing332.tts.manager.SynthesisCallback
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.manager.TextSegment
import com.github.jing332.tts.manager.TtsConfiguration
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.tts.manager.StandbyInfo
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class TtsManagerImplTest {
    // get context
    fun context(): Context {
        return InstrumentationRegistry.getInstrumentation().targetContext
    }

    private val text = """
        JAVA简易日志门面，是一套包装Logging 框架的界面程式，以外观模式实现。可以在软件部署的时候决定要使用的 Logging 框架，目前主要支援的有Java Logging API、log4j及logback等框架。
    """.trimIndent()

    @Test
    fun test() = runBlocking {
        val context = context()
        var audioTrack: AudioTrack? = null


        val manager = TtsManagerImpl.global
        manager.init()

        manager.synthesize(
            params = SystemParams(text = text, speed = 1f, pitch = 1f),
            callback = object : SynthesisCallback {
                override fun onSynthesizeStart(sampleRate: Int) {
                    audioTrack = AudioTrack.Builder()
                        .setAudioFormat(
                            AudioFormat.Builder().setSampleRate(sampleRate)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()
                        )
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .setBufferSizeInBytes(
                            AudioTrack.getMinBufferSize(
                                sampleRate,
                                AudioFormat.CHANNEL_OUT_MONO,
                                AudioFormat.ENCODING_PCM_16BIT
                            )
                        )
                        .build()

                    if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack?.play()
                    }
                }

                override fun onSynthesizeError(code: Int, reason: Exception?) {
                    reason?.printStackTrace()
                }

                override fun onSynthesizeFinish() {
                }

                override fun onSynthesizeAvailable(audio: ByteArray) {
//                    println("onSynthesizeAvailable: ${audio.size}")
                    audioTrack!!.write(audio, 0, audio.size)

//                        val maxBufferSize: Int = 1024
//                        var offset = 0
//                        while (offset < audio.size) {
//                            val bytesToWrite = maxBufferSize.coerceAtMost(audio.size - offset)
//                            audioTrack!!.write(audio, offset, bytesToWrite)
//                            offset += bytesToWrite
//                        }

                }

            })
    }
}