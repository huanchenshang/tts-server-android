package com.github.jing332.common.audio.exo

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.source.MediaSource
import com.drake.net.utils.withMain
import com.github.jing332.common.audio.AudioDecoderException
import com.github.jing332.common.audio.ExoPlayerHelper
import com.github.jing332.common.utils.rootCause
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@SuppressLint("UnsafeOptInUsageError")
class ExoAudioDecoder(val context: Context) : Closeable {
    companion object {
        private const val CANCEL_MESSAGE_ENDED = "CANCEL_MESSAGE_ENDED"
        private const val CANCEL_MESSAGE_ERROR = "CANCEL_MESSAGE_ERROR"
    }

    private var mContinuation: Continuation<Unit>? = null
    var callback: Callback? = null

    private val exoPlayer by lazy {
        val rendererFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                return DecoderAudioSink(
                    onPcmBuffer = {
                        callback?.onReadPcmAudio(it)
                    },
                    onEndOfStream = {

                    }
                )
            }
        }

        ExoPlayer.Builder(context, rendererFactory).build().apply {
            addListener(object : Player.Listener {
                @SuppressLint("SwitchIntDef")
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        ExoPlayer.STATE_ENDED -> {
                            mContinuation?.resume(Unit)
                        }
                    }

                    super.onPlaybackStateChanged(playbackState)
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)
                    mContinuation?.resumeWithException(error)
                }
            })

            playWhenReady = true
        }
    }

    @Throws(AudioDecoderException::class)
    suspend fun doDecode(bytes: ByteArray) {
        decodeInternal(ExoPlayerHelper.createMediaSourceFromByteArray(bytes))
    }

    @Throws(AudioDecoderException::class)
    suspend fun doDecode(inputStream: InputStream) {
        decodeInternal(ExoPlayerHelper.createMediaSourceFromInputStream(inputStream))
    }

    private suspend fun decodeInternal(mediaSource: MediaSource) = withMain {
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()

        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                mContinuation = continuation //直接把continuation存起来，方便后续的resumeWithException 和 resume
                continuation.invokeOnCancellation {
                    exoPlayer.stop()
                }
            }
        } catch (e: Throwable) { //直接捕获Throwable
            throw AudioDecoderException(
                message = "${e.rootCause?.localizedMessage}",
                cause = e
            )
        }
    }


    fun interface Callback {
        fun onReadPcmAudio(byteBuffer: ByteBuffer)
    }

    override fun close() {
        exoPlayer.release()
        mContinuation?.context?.cancel()
        mContinuation = null
        callback = null
    }

}