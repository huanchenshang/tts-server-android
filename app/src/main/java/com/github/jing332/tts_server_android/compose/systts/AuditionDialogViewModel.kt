package com.github.jing332.tts_server_android.compose.systts

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.jing332.common.audio.AudioPlayer
import com.github.jing332.tts.TtsManagerImpl
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.manager.TtsConfiguration
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuditionDialogViewModel(app: Application) : AndroidViewModel(app) {
    private val audioPlayer = AudioPlayer(app)

    var error by mutableStateOf("")
    var audioInfo by mutableStateOf<Triple<Int, Int, String>?>(null)

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    fun init(config: TtsConfiguration, text: String, onFinished: () -> Unit) =
        viewModelScope.launch(Dispatchers.IO) {
            TtsManagerImpl.global.ttsRequester.request(
                params = SystemParams(text = text),
                tts = config,
            ).onSuccess { ret ->
                ret.onStream { ins ->
                    val audio = ins.readBytes()
                    val info =
                        com.github.jing332.common.audio.AudioDecoder.getSampleRateAndMime(audio)
                    withContext(Dispatchers.Main) {
                        audioInfo = Triple(audio.size, info.first, info.second)
                    }

                    if (config.audioFormat.isNeedDecode)
                        audioPlayer.play(audio)
                    else
                        audioPlayer.play(audio, config.audioFormat.sampleRate)
                }.onCallback {
                    it.play()
                }
            }

            withContext(Dispatchers.Main) { onFinished() }
        }
}