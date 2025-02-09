package com.github.jing332.tts_server_android.compose.systts.plugin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.jing332.compose.ComposeExtensions.clickableRipple
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.App
import com.github.jing332.tts_server_android.AppLocale
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsUI
import com.github.jing332.tts_server_android.compose.systts.list.ui.PluginTtsViewModel
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.ui.view.ErrorDialogActivity
import io.github.oshai.kotlinlogging.KotlinLogging

@Suppress("DEPRECATION")
class PluginPreviewActivity : AppCompatActivity() {
    companion object {
        const val KEY_SOURCE = "source"
        const val KEY_PLUGIN = "plugin"
        const val ACTION_FINISH = "finish"

        private val logger = KotlinLogging.logger { PluginPreviewActivity::class.java.name }
    }

    private val mReceiver by lazy { MyBroadcastReceiver() }

    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_FINISH) {
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppConst.localBroadcast.unregisterReceiver(mReceiver)
        AppConst.localBroadcast.sendBroadcastSync(Intent(ErrorDialogActivity.ACTION_FINISH))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppConst.localBroadcast.registerReceiver(mReceiver, IntentFilter(ACTION_FINISH))

        var source = intent.getParcelableExtra<PluginTtsSource>(KEY_SOURCE)
        val plugin = intent.getParcelableExtra<Plugin>(KEY_PLUGIN)
        logger.atDebug {
            message = "loading preview plugin ui"
            payload = mapOf("source" to source, "plugin" to plugin)
        }
        if (source == null || plugin == null) {
            finish()
            return
        }

        if (source.locale.isBlank()) {
            val l = AppLocale.current(this)
            source = source.copy(locale = "${l.language}-${l.country}")// eg: en-US, zh-CN)
        }

        setContent {
            AppTheme {
                var systts by rememberSaveable {
                    mutableStateOf(
                        SystemTtsV2(
                            config = TtsConfigurationDTO(source = source)
                        )
                    )
                }
                PluginPreviewScreen(
                    plugin = plugin,
                    systts = systts,
                    onSysttsChange = { systts = it },
                    onSave = {
                        intent.putExtra(KEY_SOURCE, systts.ttsConfig.source as PluginTtsSource)
                        setResult(RESULT_OK, intent)
                        finish()
                    })
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PluginPreviewScreen(
        plugin: Plugin,
        systts: SystemTtsV2,
        onSysttsChange: (SystemTtsV2) -> Unit,
        onSave: () -> Unit
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(title = { Text(stringResource(id = R.string.plugin_preview_ui)) },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(id = R.string.nav_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            onSave()
                        }) {
                            Icon(Icons.Default.Save, stringResource(id = R.string.save))
                        }

                        var showSaveLogTips by remember { mutableStateOf(false) }
                        if (showSaveLogTips)
                            AppDialog(
                                onDismissRequest = { showSaveLogTips = false },
                                title = { Text(stringResource(R.string.write_plugin_log_to_file)) },
                                content = {
                                    Text(
                                        modifier = Modifier.clickableRipple {
//                                            runCatching {
//                                                val uri =
//                                                    FileProvider.getUriForFile(
//                                                        /* context = */ context,
//                                                        /* authority = */ AppConst.fileProviderAuthor,
//                                                        /* file = */ File(onIniFilePath())
//                                                    )
//                                                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
//                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                                    setDataAndType(uri, "text/*")
//                                                }
//
//                                                context.startActivity(
//                                                    Intent.createChooser(
//                                                        intent,"")
//                                                    )
//                                            }.onFailure {
//                                                context.longToast(it.toString())
//                                            }
                                        },
                                        text =
                                        App.context.getExternalFilesDir("logs")?.absolutePath
                                            ?: "/data/data/$packageName/files/logs"
                                    )
                                }
                            )

//                        var showOptions by remember { mutableStateOf(false) }
//                        IconButton(onClick = { showOptions = false }) {
//                            Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_options))
//
//                            DropdownMenu(
//                                expanded = showOptions,
//                                onDismissRequest = { showOptions = false }) {
//                                var isSaveRhinoLog by remember { PluginConfig.isSaveRhinoLog }
//                                CheckedMenuItem(
//                                    text = { Text(stringResource(R.string.write_plugin_log_to_file)) },
//                                    checked = isSaveRhinoLog,
//                                    onClick = {
//                                        isSaveRhinoLog = !isSaveRhinoLog
//                                        if (isSaveRhinoLog)
//                                            showSaveLogTips = true
//                                    },
//                                    leadingIcon = {
//                                        Icon(Icons.Default.DeveloperMode, null)
//                                    }
//                                )
//
//
//                            }
//                        }
                    }
                )
            }) { paddingValues ->
            val ui = remember { PluginTtsUI() }
            val pluginVM: PluginTtsViewModel = viewModel()
            pluginVM.onGetPlugin = { plugin }

            ui.EditContentScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                systts = systts,
                onSysttsChange = onSysttsChange,
                showBasicInfo = false,
                vm = pluginVM
            )
        }
    }
}