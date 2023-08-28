package com.github.jing332.tts_server_android.compose

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drake.net.utils.withIO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.widgets.AppDialog
import com.github.jing332.tts_server_android.model.rhino.direct_link_upload.DirectUploadEngine
import com.github.jing332.tts_server_android.ui.AppActivityResultContracts
import com.github.jing332.tts_server_android.ui.FilePickerActivity
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import com.github.jing332.tts_server_android.utils.ClipboardUtils
import com.github.jing332.tts_server_android.utils.longToast
import com.github.jing332.tts_server_android.utils.toast
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigExportBottomSheet(
    json: String,
    fileName: String = "config.json",
    content: @Composable ColumnScope.() -> Unit = {},
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val fileSaver =
        rememberLauncherForActivityResult(AppActivityResultContracts.filePickerActivity()) {
        }

    var showSelectUploadTargetDialog by remember { mutableStateOf(false) }
    if (showSelectUploadTargetDialog) {
        val targetList = remember {
            DirectUploadEngine(context = context).obtainFunctionList()
        }
        AppDialog(
            title = { Text(stringResource(id = R.string.choose_an_upload_target)) },
            content = {
                Box {
                    var loading by remember { mutableStateOf(false) }
                    LazyColumn(
                        Modifier
                            .padding(vertical = 16.dp)
                    ) {
                        items(targetList) {
                            Row(
                                Modifier
                                    .clickable(!loading) {
                                        scope.launch {
                                            runCatching {
                                                loading = true
                                                val url =
                                                    withIO { it.invoke(json) } ?: throw Exception(
                                                        "url is null"
                                                    )
                                                ClipboardUtils.copyText("TTS Server", url)
                                                context.longToast(R.string.copied_url)
                                                loading = false
                                            }.onFailure {
                                                loading = false
                                                context.displayErrorDialog(it)
                                                return@launch
                                            }

                                            showSelectUploadTargetDialog = false
                                        }
                                    }
                                    .minimumInteractiveComponentSize()
                            ) {
                                Text(
                                    it.funcName,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = loading, Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                    ) {
                        CircularProgressIndicator(strokeWidth = 8.dp)
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showSelectUploadTargetDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        ) { showSelectUploadTargetDialog = false }
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest, /*modifier = Modifier.fillMaxSize()*/) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            content()
            Row(Modifier.align(Alignment.CenterHorizontally)) {
                TextButton(
                    onClick = {
                        ClipboardUtils.copyText(json)
                        context.toast(R.string.copied)
                    }
                ) {
                    Text(stringResource(id = R.string.copy))
                }

                TextButton(
                    onClick = {
                        showSelectUploadTargetDialog = true
                    }
                ) {
                    Text(stringResource(id = R.string.upload_to_url))
                }

                TextButton(
                    onClick = {
                        fileSaver.launch(
                            FilePickerActivity.RequestSaveFile(
                                fileName = fileName,
                                fileMime = "application/json",
                                fileBytes = json.toByteArray()
                            )
                        )
                    }) {
                    Text(stringResource(id = R.string.save_as_file))
                }
            }
            SelectionContainer(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = json,
                    Modifier.width(1000.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}