package com.github.jing332.tts

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.github.jing332.common.utils.FileUtils
import com.github.jing332.common.utils.FileUtils.mimeType
import com.github.jing332.common.utils.runOnUI
import com.github.jing332.tts.manager.BgmSource
import com.github.jing332.tts.manager.IBgmPlayer
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import kotlin.math.pow


class BgmPlayer(val context: ManagerContext) : IBgmPlayer {
    companion object {
        const val TAG = "BgmPlayer"
        val logger = KotlinLogging.logger(TAG)
    }

    private val exoPlayer by lazy {
        ExoPlayer.Builder(context.androidContext).build().apply {
            addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)
                    val volume = mediaItem?.localConfiguration?.tag
                    if (volume != null && volume is Float && volume != this@apply.volume)
                        this@apply.volume = volume.pow(1.5f)
                }

                override fun onPlayerError(error: PlaybackException) {
                    super.onPlayerError(error)

                    logger.error(error){"bgm error, skip current media"}
                    removeMediaItem(currentMediaItemIndex)
                    seekToNextMediaItem()
                    prepare()
                }
            })
            repeatMode = Player.REPEAT_MODE_ALL
            shuffleModeEnabled = context.cfg.bgmShuffleEnabled()
        }
    }
    private val currentPlayList = mutableListOf<BgmSource>()

    override fun stop() {
        logger.debug { "bgm stop" }
        runOnUI { exoPlayer.pause() }
    }

    override fun destroy() {
        logger.debug { "bgm destroy" }
        runOnUI {
            exoPlayer.release()
        }
    }

    override fun play() {
        logger.debug { "bgm play" }
        runOnUI {
            if (!exoPlayer.isPlaying) exoPlayer.play()
        }
    }

    override fun setPlayList(
        list: List<BgmSource>,
    ) = runOnUI{
        logger.atDebug {
            message = "bgm setPlayList"
            payload = mapOf("list" to list)
        }

        if (list == currentPlayList) return@runOnUI
        currentPlayList.clear()
        currentPlayList.addAll(list)

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        for (source in list) {
            val file = File(source.path)
            if (file.isDirectory) {
                val allFiles = FileUtils.getAllFilesInFolder(file)
                    .run { if (context.cfg.bgmShuffleEnabled()) this.shuffled() else this }
                for (subFile in allFiles) {
                    if (!addMediaItem(source.volume, subFile)) continue
                }
            } else if (file.isFile) {
                addMediaItem(source.volume, file)
            }
        }
        exoPlayer.prepare()
    }

    private fun addMediaItem(volume: Float, file: File): Boolean {
        val mime = file.mimeType
        // 非audio或未知则跳过
        if (mime == null || !mime.startsWith("audio")) return false

        logger.atTrace {
            message = "bgm addMediaItem"
            payload = mapOf("file" to file)
        }
        val item =
            MediaItem.Builder().setTag(volume).setUri(file.absolutePath).build()
        exoPlayer.addMediaItem(item)

        return true
    }

}