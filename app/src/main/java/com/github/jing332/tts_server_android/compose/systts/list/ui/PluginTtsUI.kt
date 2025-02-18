package com.github.jing332.tts_server_android.compose.systts.list.ui

import android.util.Log
import android.widget.LinearLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drake.net.utils.withIO
import com.github.jing332.common.utils.toScale
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.compose.widgets.LoadingDialog
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.BaseAudioFormat
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.AuditionDialog
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AuditionTextField
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.BasicInfoEditScreen
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.SaveActionHandler
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.TtsTopAppBar
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PluginTtsUI : IConfigUI() {
    companion object {
        const val TAG = "PluginTtsUI"
    }

    @Composable
    override fun ParamsEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
    ) {

        val tts = (systemTts.config as TtsConfigurationDTO).source as PluginTtsSource
        Column(modifier) {
            val rateStr =
                stringResource(
                    id = R.string.label_speech_rate,
                    if (tts.speed == 0f) stringResource(id = R.string.follow) else tts.speed.toString()
                )
            LabelSlider(
                text = rateStr,
                value = tts.speed,
                onValueChange = {
                    onSystemTtsChange(systemTts.copySource(tts.copy(speed = it.toScale(2))))
                },
                valueRange = 0f..2f
            )

            val volumeStr =
                stringResource(
                    id = R.string.label_speech_volume,
                    if (tts.volume == 0f) stringResource(id = R.string.follow) else tts.volume.toString()
                )
            LabelSlider(
                text = volumeStr, value = tts.volume, onValueChange = {
                    onSystemTtsChange(
                        systemTts.copySource(
                            tts.copy(volume = it.toScale(2))
                        )
                    )
                }, valueRange = 0f..2f
            )

            val pitchStr = stringResource(
                id = R.string.label_speech_pitch,
                if (tts.pitch == 0f) stringResource(id = R.string.follow) else tts.pitch.toString()
            )
            LabelSlider(
                text = pitchStr, value = tts.pitch, onValueChange = {
                    onSystemTtsChange(
                        systemTts.copySource(
                            tts.copy(pitch = it.toScale(2))
                        )
                    )
                }, valueRange = 0f..2f
            )
        }
    }

    @Composable
    override fun FullEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
        onSave: () -> Unit,
        onCancel: () -> Unit,
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TtsTopAppBar(
                    title = { Text(text = stringResource(id = R.string.edit_plugin_tts)) },
                    onBackAction = onCancel,
                    onSaveAction = {
                        onSave()
                    }
                )
            }
        ) { paddingValues ->
            EditContentScreen(
                modifier = Modifier
                    .padding(paddingValues)
                    .verticalScroll(
                        rememberScrollState()
                    ),
                systts = systemTts, onSysttsChange = onSystemTtsChange,
            )
        }
    }

    @Composable
    fun EditContentScreen(
        modifier: Modifier,
        systts: SystemTtsV2,
        onSysttsChange: (SystemTtsV2) -> Unit,
        showBasicInfo: Boolean = true,
        plugin: Plugin? = null,
        vm: PluginTtsViewModel = viewModel(),
    ) {
        var displayName by remember { mutableStateOf("") }

        @Suppress("NAME_SHADOWING")
        val systts by rememberUpdatedState(newValue = systts)
        val tts by rememberUpdatedState(newValue = (systts.config as TtsConfigurationDTO).source as PluginTtsSource)
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        SaveActionHandler {
            val sampleRate = try {
                withIO { vm.engine.getSampleRate(tts.locale, tts.voice) ?: 16000 }
            } catch (e: Exception) {
                context.displayErrorDialog(
                    e,
                    context.getString(R.string.plugin_tts_get_sample_rate_failed)
                )
                null
            }

            val isNeedDecode = try {
                withIO { vm.engine.isNeedDecode(tts.locale, tts.voice) }
            } catch (e: Exception) {
                context.displayErrorDialog(
                    e,
                    context.getString(R.string.plugin_tts_get_need_decode_failed)
                )
                null
            }

            if (sampleRate != null && isNeedDecode != null) {
                onSysttsChange(
                    systts.copy(
                        displayName = if (systts.displayName.isNullOrBlank()) displayName else systts.displayName,
                        config = (systts.config as TtsConfigurationDTO).copy(
                            audioFormat = BaseAudioFormat(
                                sampleRate = sampleRate,
                                isNeedDecode = isNeedDecode
                            )
                        ),
                    )
                )

                true
            } else
                false
        }

        var showLoadingDialog by remember { mutableStateOf(false) }
        if (showLoadingDialog)
            LoadingDialog(onDismissRequest = { showLoadingDialog = false })

        var showAuditionDialog by remember { mutableStateOf(false) }
        @Suppress("UNCHECKED_CAST")
        if (showAuditionDialog)
            AuditionDialog(
                systts = systts,
                engine = if (plugin == null) null else vm.service()
            ) {
                showAuditionDialog = false
            }

        Column(modifier) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                if (showBasicInfo)
                    BasicInfoEditScreen(
                        Modifier.fillMaxWidth(),
                        systemTts = systts,
                        onSystemTtsChange = onSysttsChange
                    )

                AuditionTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    onAudition = {
                        showAuditionDialog = true
                    }
                )

                LoadingContent(isLoading = vm.isLoading) {
                    Column {
                        AppSpinner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            label = { Text(stringResource(id = R.string.language)) },
                            value = tts.locale,
                            values = vm.locales.map { it.first },
                            entries = vm.locales.map { it.second },
                            onSelectedChange = { locale, _ ->
                                Log.d("PluginTtsUI", "locale onSelectedChange: $locale")
                                if (locale.toString().isBlank()) return@AppSpinner
                                onSysttsChange(systts.copySource(tts.copy(locale = locale.toString())))
                                runCatching {
                                    scope.launch(Dispatchers.IO) {
                                        vm.updateVoices(locale.toString())
                                    }
                                }
                            },
                        )

                        AppSpinner(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            label = { Text(stringResource(id = R.string.label_voice)) },
                            value = tts.voice,
                            values = vm.voices.map { it.first },
                            entries = vm.voices.map { it.second },
                            onSelectedChange = { voice, name ->
                                Log.d(
                                    "PluginTtsUI",
                                    "voice onSelectedChange: voice=$voice, name=$name"
                                )

                                val lastName =
                                    vm.voices.find { it.first == tts.voice }?.second ?: ""
                                onSysttsChange(
                                    systts.copy(
                                        displayName =
                                        if (systts.displayName.isNullOrBlank() || lastName == systts.displayName) name
                                        else systts.displayName,
                                        config = (systts.config as TtsConfigurationDTO).copy(
                                            source = tts.copy(
                                                voice = voice as String
                                            )
                                        )
                                    )
                                )

                                runCatching {
                                    Log.d(
                                        "PluginTtsUI",
                                        "updateCustomUI: locale=${tts.locale}, voice=$voice"
                                    )
                                    vm.updateCustomUI(tts.locale, voice)
                                }.onFailure {
                                    context.displayErrorDialog(it)
                                }

                                displayName = name
                            }
                        )

                        val scope = rememberCoroutineScope()
                        suspend fun load(linearLayout: LinearLayout) {
                            runCatching {
                                vm.load(context, plugin, tts, linearLayout)
                            }.onFailure {
                                it.printStackTrace()
                                context.displayErrorDialog(it)
                            }
                        }

                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            factory = {
                                LinearLayout(it).apply {
                                    orientation = LinearLayout.VERTICAL
                                    scope.launch { load(this@apply) }
                                }
                            }
                        )
                    }
                }
            }

            ParamsEditScreen(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                systemTts = systts,
                onSystemTtsChange = onSysttsChange
            )
        }
    }
}
