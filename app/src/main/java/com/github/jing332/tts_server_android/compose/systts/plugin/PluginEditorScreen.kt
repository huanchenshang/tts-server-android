package com.github.jing332.tts_server_android.compose.systts.plugin

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.common.utils.FileUtils.readAllText
import com.github.jing332.common.utils.longToast
import com.github.jing332.compose.widgets.TextFieldDialog
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.codeeditor.CodeEditorScreen
import com.github.jing332.tts_server_android.compose.codeeditor.LoggerBottomSheet
import com.github.jing332.tts_server_android.compose.codeeditor.string
import com.github.jing332.tts_server_android.conf.PluginConfig
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import io.github.rosemoe.sora.widget.CodeEditor

@Suppress("DEPRECATION")
@Composable
internal fun PluginEditorScreen(
    plugin: Plugin,
    onSave: (Plugin) -> Unit,
    vm: PluginEditorViewModel = viewModel()
) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    var codeEditor by remember { mutableStateOf<CodeEditor?>(null) }

//    @Suppress("NAME_SHADOWING")
//    var plugin by remember { mutableStateOf(plugin) }

    val code by vm.codeLiveData.asFlow().collectAsState(initial = "")
    LaunchedEffect(codeEditor, code) {
        if (codeEditor != null && code.isNotEmpty())
            codeEditor?.setText(code)
    }

    LaunchedEffect(vm) {
        vm.init(plugin, context.assets.open("defaultData/plugin-azure.js").readAllText())
    }

    var showTextParamDialog by remember { mutableStateOf(false) }
    if (showTextParamDialog) {
        var sampleText by remember { mutableStateOf(PluginConfig.textParam.value) }
        TextFieldDialog(
            title = stringResource(id = R.string.set_sample_text_param),
            text = sampleText,
            onTextChange = { sampleText = it },
            onDismissRequest = { showTextParamDialog = false }) {
            PluginConfig.textParam.value = sampleText
        }
    }

    var showDebugLogger by remember { mutableStateOf(false) }
    if (showDebugLogger) {
        LoggerBottomSheet(
            registry = (vm.console),
            onDismissRequest = {
                showDebugLogger = false
                vm.stopDebug()
            }) {
            runCatching {
                vm.debug(codeEditor!!.string())
            }.onFailure {
                context.displayErrorDialog(it)
            }
        }
    }

    var showVarsDialog by rememberSaveable { mutableStateOf(false) }
    if (showVarsDialog) {
        var p by rememberSaveable { mutableStateOf(vm.plugin) }
        PluginVarsBottomSheet(
            onDismissRequest = {
                showVarsDialog = false
                vm.updatePlugin(p)
            },
            plugin = p,
            onPluginChange = {
                p = it
            }
        )
    }

    val previewLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            val source =
                it.data?.getParcelableExtra<PluginTtsSource>(PluginPreviewActivity.KEY_SOURCE)
            Log.d("PluginEditor", "source update: $source")
            if (source == null) {
                context.longToast("空返回值")
            } else
                vm.updateSource(source)
        }

    fun previewUi() {
        AppConst.localBroadcast.sendBroadcastSync(Intent(PluginPreviewActivity.ACTION_FINISH))
        try {
            vm.updateCode(codeEditor!!.text.toString())
        } catch (e: Exception) {
            context.displayErrorDialog(e)
        }
        previewLauncher.launch(Intent(context, PluginPreviewActivity::class.java).apply {
            putExtra(PluginPreviewActivity.KEY_PLUGIN, vm.plugin)
            putExtra(PluginPreviewActivity.KEY_SOURCE, vm.pluginSource)
        })
    }

    CodeEditorScreen(
        title = {
            Column {
                Text(
                    stringResource(id = R.string.plugin),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(plugin.name, style = MaterialTheme.typography.bodyMedium)
            }
        },
        onBack = { navController.popBackStack() },
        onDebug = { showDebugLogger = true },

        onSave = {
            if (codeEditor != null) {
                runCatching {
                    vm.updateCode(codeEditor!!.string())
                    onSave(vm.plugin)
                    navController.popBackStack()
                }.onFailure {
                    context.displayErrorDialog(it)
                }
            }
        },
        onLongClickSave = { // 仅保存
            onSave(vm.plugin.copy(code = codeEditor!!.string()))
            navController.popBackStack()
        },

        onRemoteAction = { name, _ ->
            when (name) {
                "ui" -> {
                    previewUi()
                }
            }
        },
        onUpdate = { codeEditor = it },
        onSaveFile = {
            "ttsrv-plugin-${vm.plugin.name}.js" to codeEditor!!.text.toString().toByteArray()
        },
        onLongClickMoreLabel = stringResource(id = R.string.plugin_preview_ui),
        onLongClickMore = { previewUi() }
    ) { dismiss ->
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.plugin_preview_ui)) },
            onClick = {
                dismiss()
                previewUi()
            },
            leadingIcon = {
                Icon(Icons.Default.Settings, null)
            }
        )

        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.set_sample_text_param)) },
            onClick = {
                dismiss()
                showTextParamDialog = true
            },
            leadingIcon = {
                Icon(Icons.Default.TextFields, null)
            }
        )

        DropdownMenuItem(
            text = { Text(stringResource(R.string.plugin_set_vars)) },
            onClick = {
                dismiss()
                showVarsDialog = true
            },
            leadingIcon = {
                Icon(Icons.Default.EditNote, null)
            }
        )
    }
}