package com.github.jing332.tts_server_android.compose.systts.list.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.TextToSpeechSource

abstract class IConfigUI {
    open val showSpeechEdit: Boolean = true

    @Composable
    abstract fun FullEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
        onSave: () -> Unit,
        onCancel: () -> Unit
    )

    @Composable
    abstract fun ParamsEditScreen(
        modifier: Modifier,
        systemTts: SystemTtsV2,
        onSystemTtsChange: (SystemTtsV2) -> Unit,
    )

    fun SystemTtsV2.copySource(source: TextToSpeechSource): SystemTtsV2 {
        val config = config as TtsConfigurationDTO
        return this.copy(config = config.copy(source = source))
    }
}