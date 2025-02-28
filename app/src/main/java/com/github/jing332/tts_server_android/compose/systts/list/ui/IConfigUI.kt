package com.github.jing332.tts_server_android.compose.systts.list.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.TextToSpeechSource
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.TtsTopAppBar

abstract class IConfigUI {
    open val showSpeechEdit: Boolean = true

    @Composable
    abstract fun FullEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
        onSave: () -> Unit,
        onCancel: () -> Unit,
        content: @Composable () -> Unit,
    )

    @Composable
    abstract fun ParamsEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
    )

    protected fun SystemTtsV2.copySource(source: TextToSpeechSource): SystemTtsV2 {
        val config = config as TtsConfigurationDTO
        return this.copy(config = config.copy(source = source))
    }

    @Composable
    protected fun DefaultFullEditScreen(
        modifier: Modifier,
        title: String,
        onCancel: () -> Unit,
        onSave: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TtsTopAppBar(
                    title = { Text(text = title) },
                    onBackAction = onCancel,
                    onSaveAction = {
                        onSave()
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(top = paddingValues.calculateTopPadding())
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}