package com.github.jing332.tts_server_android.compose.systts

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withMain
import com.github.jing332.common.audio.AudioPlayer
import com.github.jing332.common.utils.ClipboardUtils
import com.github.jing332.common.utils.StringUtils.sizeToReadable
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.ComposeExtensions.clickableRipple
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts.CachedEngineManager
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.manager.TtsConfiguration.Companion.toVO
import com.github.jing332.tts.speech.EngineState
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.AppConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx


private val logger = KotlinLogging.logger("AuditionDialog")

@Composable
fun AuditionDialog(
    systts: SystemTtsV2,
    text: String = AppConfig.testSampleText.value,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    val audioPlayer = remember { AudioPlayer(context) }

    DisposableEffect(systts) {
        onDispose {
            audioPlayer.stop()
        }
    }

    LaunchedEffect(systts) {
        if (systts.config !is TtsConfigurationDTO) {
            context.toast(R.string.not_support_audition)

            onDismissRequest()
            return@LaunchedEffect
        }

        launch(Dispatchers.IO) {
            val config = (systts.config as TtsConfigurationDTO).toVO()
            try {
                val e = CachedEngineManager.getEngine(appCtx, config.source)
                    ?: throw IllegalStateException("engine is null")

                if (e.state is EngineState.Uninitialized) e.onInit()
                if (e.isSyncPlay(config.source)) {
                    e.syncPlay(SystemParams(text = text), config.source)
                } else {
                    val stream = e.getStream(SystemParams(text = text), config.source)
                    val audio = stream.readBytes()
                    val rateAndMime =
                        com.github.jing332.common.audio.AudioDecoder.getSampleRateAndMime(audio)
                    withMain {
                        info = context.getString(
                            R.string.systts_test_success_info, audio.size.toLong().sizeToReadable(),
                            rateAndMime.first, rateAndMime.second
                        )
                    }

                    if (config.audioFormat.isNeedDecode)
                        audioPlayer.play(audio)
                    else
                        audioPlayer.play(audio, config.audioFormat.sampleRate)
                }
                withContext(Dispatchers.Main) {
                    onDismissRequest()
                }
            } catch (e: Exception) {
                error = e.message ?: e.toString()
                logger.warn { e.stackTraceToString() }
            }
        }
    }

    AppDialog(onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.audition)) },
        content = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SelectionContainer {
                    Text(
                        error.ifEmpty { text },
                        color = if (error.isEmpty()) Color.Unspecified else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (error.isEmpty())
                    LoadingContent(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        isLoading = info.isEmpty()
                    ) {
                        SelectionContainer {
                            Text(info, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.cancel)) }
        }
    )

}