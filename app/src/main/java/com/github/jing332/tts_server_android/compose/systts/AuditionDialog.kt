package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drake.net.utils.withMain
import com.github.jing332.common.utils.ClipboardUtils
import com.github.jing332.common.utils.StringUtils.sizeToReadable
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.ComposeExtensions.clickableRipple
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LoadingContent
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts.TtsManagerImpl
import com.github.jing332.tts.manager.SystemParams
import com.github.jing332.tts.manager.TtsConfiguration.Companion.toVO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.michaelbull.result.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuditionDialog(
    systts: SystemTtsV2,
    text: String = AppConfig.testSampleText.value,
    onDismissRequest: () -> Unit
) {
    val vm: AuditionDialogViewModel = viewModel()
    val context = LocalContext.current

    LaunchedEffect(systts.id) {
        if (systts.config !is TtsConfigurationDTO) {
            context.toast(R.string.not_support_audition)

            onDismissRequest()
            return@LaunchedEffect
        }

        val cfg = (systts.config as TtsConfigurationDTO).toVO()
        vm.init(cfg, text, onDismissRequest)
    }
    AppDialog(onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.audition)) },
        content = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    vm.error.ifEmpty { text },
                    color = if (vm.error.isEmpty()) Color.Unspecified else MaterialTheme.colorScheme.error,
//                    maxLines = if (error.isEmpty()) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodySmall
                )

                val infoStr = stringResource(
                    id = R.string.systts_test_success_info,
                    vm.audioInfo?.first?.toLong()?.sizeToReadable() ?: 0,
                    vm.audioInfo?.second ?: 0,
                    vm.audioInfo?.third ?: ""
                )
                if (vm.error.isEmpty())
                    LoadingContent(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .clickableRipple {
                                ClipboardUtils.copyText("TTS Server", infoStr)
                                context.toast(R.string.copied)
                            }, isLoading = vm.audioInfo == null
                    ) {
                        Text(infoStr, style = MaterialTheme.typography.bodyMedium)
                    }

            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(id = R.string.cancel)) }
        }
    )

}