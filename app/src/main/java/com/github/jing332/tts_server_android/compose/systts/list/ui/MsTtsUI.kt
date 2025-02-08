package com.github.jing332.tts_server_android.compose.systts.list.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.common.utils.toScale
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.ITtsSource
import com.github.jing332.database.entities.systts.source.MsTtsSource
import com.github.jing332.database.entities.systts.v1.tts.MsTtsApiType
import com.github.jing332.database.entities.systts.v1.tts.MsTtsFormatManger
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.AuditionDialog
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.SpeechRuleEditScreen
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.SaveActionHandler
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.AuditionTextField
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.TtsTopAppBar
import com.github.jing332.tts_server_android.model.GeneralVoiceData
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog

class MsTtsUI : IConfigUI() {
    @Composable
    override fun ParamsEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit
    ) {
        val config = (systemTts.config as TtsConfigurationDTO)
        val source = config.source as MsTtsSource
        fun onSourceChange(source: ITtsSource) {
            onSystemTtsChange(
                systemTts.copy(config = config.copy(source = source))
            )
        }

        Column(modifier) {
            val formats = remember { MsTtsFormatManger.getFormatsByApiType(MsTtsApiType.EDGE) }
            AppSpinner(
                label = { Text(stringResource(id = R.string.label_audio_format)) },
                value = source.format,
                values = formats,
                entries = formats,
                onSelectedChange = { k, v ->
                    onSourceChange(source.copy(format = k as String))
                },
                modifier = Modifier,
            )

            val speed = source.speed
            val rateStr = stringResource(
                id = R.string.label_speech_rate,
                if (speed == MsTtsSource.SPEED_FOLLOW) stringResource(id = R.string.follow) else speed.toString()
            )
            LabelSlider(
                value = speed,
                onValueChange = {
                    onSourceChange(source.copy(speed = it.toScale(2)))
                },
                valueRange = 0f..2f,
                text = rateStr,
            )

            val volume = source.volume
            val volStr = stringResource(
                id = R.string.label_speech_volume,
                if (volume == MsTtsSource.VOLUME_FOLLOW) stringResource(id = R.string.follow) else volume.toString()
            )
            LabelSlider(
                value = volume,
                onValueChange = {
                    onSourceChange(source.copy(volume = it.toScale(2)))
                },
                valueRange = 0f..2f,
                text = volStr,
            )

            val pitch = source.pitch
            val pitchStr = stringResource(
                id = R.string.label_speech_pitch,
                if (pitch == MsTtsSource.PITCH_FOLLOW) stringResource(id = R.string.follow) else pitch.toString()
            )
            LabelSlider(
                value = pitch,
                onValueChange = {
                    onSourceChange(source.copy(pitch = it.toScale(2)))
                },
                valueRange = 0f..2f,
                text = pitchStr
            )

        }
    }

    @Composable
    override fun FullEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
        onSave: () -> Unit,
        onCancel: () -> Unit
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TtsTopAppBar(
                    title = { Text(text = stringResource(id = R.string.edit_builtin_tts)) },
                    onBackAction = onCancel,
                    onSaveAction = {
                        onSave()
                    }
                )
            }) { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Content(
                    modifier = Modifier
                        .padding(8.dp),
                    systemTts = systemTts,
                    onSystemTtsChange = onSystemTtsChange,
                )
            }
        }
    }

    @Composable
    private fun Content(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,

        vm: MsTtsViewModel = viewModel()
    ) {
        val config = (systemTts.config as TtsConfigurationDTO)
        val source = config.source as MsTtsSource
        fun onSourceChange(source: ITtsSource) {
            onSystemTtsChange(
                systemTts.copy(config = config.copy(source = source))
            )
        }

        val context = LocalContext.current
        var displayName by remember { mutableStateOf("") }

        val systts by rememberUpdatedState(newValue = systemTts)
        SaveActionHandler {
            if (systts.displayName.isBlank())
                onSystemTtsChange(
                    systts.copy(
                        displayName = displayName
                    )
                )

            true
        }

        var showAudition by remember { mutableStateOf(false) }
        if (showAudition) {
            AuditionDialog(systts = systts) { showAudition = false }
        }

        Column(modifier) {
            SpeechRuleEditScreen(
                modifier = Modifier
                    .fillMaxWidth(),
                systts = systts,
                onSysttsChange = onSystemTtsChange
            )

            AuditionTextField(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp), onAudition = {
                showAudition = true
            })

            val apis =
                remember { listOf(R.string.systts_api_edge, R.string.systts_api_edge_okhttp) }

            LaunchedEffect(vm) {
                runCatching {
                    vm.load()
                    vm.updateLocales()
                }.onFailure {
                    context.displayErrorDialog(it)
                }
            }

            AppSpinner(
                label = { Text(stringResource(id = R.string.label_api)) },
                value = source.api,
                values = listOf(MsTtsApiType.EDGE, MsTtsApiType.EDGE_OKHTTP),
                entries = apis.map { stringResource(id = it) },
                onSelectedChange = { api, _ ->
                    onSourceChange(source.copy(api = api as Int))
                },
                modifier = Modifier.padding(top = 4.dp)
            )

            LoadingContent(isLoading = vm.isLoading) {
                Column {
                    AppSpinner(
                        label = { Text(stringResource(id = R.string.language)) },
                        value = source.locale,
                        values = vm.locales.map { it.first },
                        entries = vm.locales.map { it.second },
                        onSelectedChange = { lang, _ ->
                            onSourceChange(source.copy(locale = lang as String))
                            vm.onLocaleChanged(lang)
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    LaunchedEffect(source.locale) {
                        vm.onLocaleChanged(source.locale)
                    }

                    fun GeneralVoiceData.name() = localVoiceName + " (${voiceName})"

                    AppSpinner(
                        label = { Text(stringResource(id = R.string.label_voice)) },
                        value = source.voice,
                        values = vm.voices.map { it.voiceName },
                        entries = vm.voices.map { it.name() },
                        onSelectedChange = { voice, name ->
                            val lastName =
                                vm.voices.find { it.voiceName == source.voice }?.name() ?: ""
                            onSystemTtsChange(
                                systts.copy(
                                    displayName =
                                    if (systts.displayName.isNullOrBlank() || lastName == systts.displayName) name
                                    else systts.displayName,

                                    config = config.copy(source = source.copy(voice = voice as String))
                                )
                            )

                            displayName = name
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    )

                }
            }

            ParamsEditScreen(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                systemTts = systts,
                onSystemTtsChange = onSystemTtsChange
            )
        }

    }
}