package com.github.jing332.tts_server_android.compose.systts.list.ui.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.github.jing332.common.utils.ClipboardUtils
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.AppSpinner
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.SpeechRule
import com.github.jing332.database.entities.systts.AudioParams
import com.github.jing332.database.entities.systts.SpeechRuleInfo
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.systts.list.BasicAudioParamsDialog
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.constant.SpeechTarget
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import kotlinx.serialization.encodeToString

@Composable
fun SpeechRuleEditScreen(
    modifier: Modifier,
    systts: SystemTtsV2,
    onSysttsChange: (SystemTtsV2) -> Unit,

    showSpeechTarget: Boolean = true,
    speechRules: List<SpeechRule> = remember { dbm.speechRuleDao.allEnabled },
) {
    val context = LocalContext.current

    @Suppress("NAME_SHADOWING")
    val systts by rememberUpdatedState(newValue = systts)
    val config by rememberUpdatedState(newValue = systts.config as TtsConfigurationDTO)
    val speechRule by rememberUpdatedState(newValue = speechRules.find { it.ruleId == config.speechRule.tagRuleId })

    SaveActionHandler {
        var tagName = ""
        if (speechRule != null) {
            runCatching {
                tagName =
                    SpeechRuleEngine.getTagName(context, speechRule!!, info = config.speechRule)
            }.onFailure {
                context.displayErrorDialog(it, "获取标签名失败")
            }
        }

        tagName = tagName.ifBlank {
            speechRule?.tags?.getOrDefault(config.speechRule.tag, "") ?: ""
        }
        onSysttsChange(
            systts.copy(
                config = config.copy(config.speechRule.copy(tagName = tagName))
            )
        )
        true
    }

    var showStandbyHelpDialog by remember { mutableStateOf(false) }
    if (showStandbyHelpDialog)
        AppDialog(
            title = { Text(stringResource(id = R.string.systts_as_standby_help)) },
            content = {
                Text(
                    stringResource(id = R.string.systts_standby_help_msg)
                )
            },
            buttons = {
                TextButton(onClick = { showStandbyHelpDialog = false }) {
                    Text(stringResource(id = R.string.confirm))
                }
            },
            onDismissRequest = { showStandbyHelpDialog = false }
        )

    var showParamsDialog by remember { mutableStateOf(false) }
    if (showParamsDialog) {
        val params = config.audioParams
        fun changeParams(
            speed: Float = params.speed,
            volume: Float = params.volume,
            pitch: Float = params.pitch,
        ) {
            onSysttsChange(
                systts.copy(
                    config = config.copy(audioParams = AudioParams(speed, volume, pitch))
                )
            )
        }
        BasicAudioParamsDialog(
            onDismissRequest = { showParamsDialog = false },
            speed = params.speed,
            volume = params.volume,
            pitch = params.pitch,

            onSpeedChange = { changeParams(speed = it) },
            onVolumeChange = { changeParams(volume = it) },
            onPitchChange = { changeParams(pitch = it) },

            onReset = { changeParams(0f, 0f, 0f) }
        )
    }

    if (showSpeechTarget)
        Column(modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .horizontalScroll(rememberScrollState())
            ) {
                TextButton(onClick = { showParamsDialog = true }) {
                    Row {
                        Icon(Icons.Default.Speed, null)
                        Text(stringResource(id = R.string.audio_params))
                    }
                }

                Row(
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(role = Role.Checkbox) {
                            onSysttsChange(
                                systts.copy(
                                    config = config.copy(
                                        speechRule = config.speechRule.copy(isStandby = !config.speechRule.isStandby)
                                    )
                                )
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = config.speechRule.isStandby, onCheckedChange = null)
                    Text(stringResource(id = R.string.as_standby))
                    IconButton(onClick = { showStandbyHelpDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.HelpOutline,
                            stringResource(id = R.string.systts_as_standby_help)
                        )
                    }
                }
            }

            var showTagClearDialog by remember { mutableStateOf(false) }
            if (showTagClearDialog) {
                TagDataClearConfirmDialog(
                    config.speechRule.tagData.toString(),
                    onDismissRequest = { showTagClearDialog = false },
                    onConfirm = {
                        onSysttsChange(
                            systts.copy(
                                config = config.copy(
                                    speechRule = config.speechRule.copy(
                                        tagName = "",
                                        target = SpeechTarget.ALL
                                    ).apply { resetTag() }
                                )
                            )
                        )
                        showTagClearDialog = false
                    })
            }

            var showTagOptions by remember { mutableStateOf(false) }
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .padding(4.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                SegmentedButton(
                    config.speechRule.target != SpeechTarget.TAG,
                    onClick = {
                        if (config.speechRule.isTagDataEmpty())
                            onSysttsChange(
                                systts.copy(
                                    config = config.copy(
                                        speechRule = config.speechRule.copy(
                                            tagName = "",
                                            target = SpeechTarget.ALL
                                        ).apply { resetTag() }
                                    )
                                )
                            )
                        else
                            showTagClearDialog = true
                    },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = { Icon(Icons.Default.SelectAll, null) },
                ) {
                    Text(stringResource(id = R.string.ra_all), maxLines = 1)
                }

                SegmentedButton(
                    selected = config.speechRule.target == SpeechTarget.TAG,
                    onClick = {
                        if (config.speechRule.target == SpeechTarget.TAG)
                            showTagOptions = true
                        else
                            onSysttsChange(
                                systts.copy(
                                    config = config.copy(
                                        speechRule = config.speechRule.copy(target = SpeechTarget.TAG)
                                    )
                                )
                            )
                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = {
                        Icon(
                            Icons.Default.Tag,
                            null,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    },
                ) {
                    Text(
                        stringResource(id = R.string.tag),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp, end = 10.dp)
                    )

                    DropdownMenu(
                        expanded = showTagOptions,
                        onDismissRequest = { showTagOptions = false }) {
                        Text(
                            text = stringResource(R.string.tag_data),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.copy)) },
                            onClick = {
                                showTagOptions = false
                                val jStr =
                                    AppConst.jsonBuilder.encodeToString(config.speechRule)
                                ClipboardUtils.copyText(jStr)
                                context.toast(R.string.copied)
                            })
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.paste)) },
                            onClick = {
                                showTagOptions = false
                                val jStr = ClipboardUtils.text.toString()
                                if (jStr.isBlank()) {
                                    context.toast(R.string.format_error)
                                    return@DropdownMenuItem
                                }

                                runCatching {
                                    val info =
                                        AppConst.jsonBuilder.decodeFromString<SpeechRuleInfo>(
                                            jStr
                                        )
                                    onSysttsChange(systts.copy(config = config.copy(speechRule = info)))
                                }.onSuccess {
                                    context.longToast(R.string.save_success)
                                }.onFailure {
                                    context.displayErrorDialog(
                                        it,
                                        context.getString(R.string.format_error)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = config.speechRule.target == SpeechTarget.TAG) {
                Row(Modifier.padding(top = 4.dp)) {
                    AppSpinner(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        label = { Text(stringResource(id = R.string.speech_rule_script)) },
                        value = config.speechRule.tagRuleId,
                        values = speechRules.map { it.ruleId },
                        entries = speechRules.map { it.name },
                        onSelectedChange = { k, v ->
                            if (config.speechRule.target != SpeechTarget.TAG) return@AppSpinner
                            onSysttsChange(
                                systts.copy(
                                    config = config.copy(
                                        speechRule = config.speechRule.copy(
                                            tagRuleId = k as String
                                        )
                                    )
                                )
                            )
                        }
                    )

                    speechRule?.let { speechRule ->
                        AppSpinner(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp),
                            label = { Text(stringResource(id = R.string.tag)) },
                            value = config.speechRule.tag,
                            values = speechRule.tags.keys.toList(),
                            entries = speechRule.tags.values.toList(),
                            onSelectedChange = { k, _ ->
                                if (config.speechRule.target != SpeechTarget.TAG) return@AppSpinner
                                onSysttsChange(
                                    systts.copy(
                                        config = config.copy(
                                            speechRule = config.speechRule.copy(tag = k as String)
                                        )
                                    )
                                )
                            }
                        )
                    }
                }
            }

            speechRule?.let {
                CustomTagScreen(
                    info = config.speechRule,
                    onInfoChange = {
                        if (config.speechRule.target == SpeechTarget.TAG)
                            onSysttsChange(systts.copy(config = config.copy(speechRule = it)))
                    },
                    speechRule = it
                )
            }
        }
}

@Composable
private fun CustomTagScreen(
    info: SpeechRuleInfo,
    onInfoChange: (SpeechRuleInfo) -> Unit,
    speechRule: SpeechRule,
) {
    var showHelpDialog by remember { mutableStateOf("" to "") }
    if (showHelpDialog.first.isNotEmpty()) {
        AppDialog(title = { Text(showHelpDialog.first) }, content = {
            Text(showHelpDialog.second)
        }, buttons = {
            TextButton(onClick = { showHelpDialog = "" to "" }) {
                Text(stringResource(id = R.string.confirm))
            }
        }, onDismissRequest = { showHelpDialog = "" to "" })
    }

    Column(Modifier.padding(vertical = 4.dp)) {
        speechRule.tagsData[info.tag]?.forEach { defTag ->
            val key = defTag.key
            val label = defTag.value["label"] ?: ""
            val hint = defTag.value["hint"] ?: ""

            val items = defTag.value["items"]
            val value by rememberUpdatedState(newValue = info.tagData[key] ?: "")
            if (items.isNullOrEmpty()) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    leadingIcon = {
                        if (hint.isNotEmpty())
                            IconButton(onClick = { showHelpDialog = label to hint }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.HelpOutline,
                                    stringResource(id = R.string.help)
                                )
                            }
                    },
                    label = { Text(label) },
                    value = value,
                    onValueChange = {
                        onInfoChange(
                            info.copy(
                                tagData = info.tagData.toMutableMap().apply {
                                    this[key] = it
                                }
                            )
                        )
                    }
                )
            } else {
                val itemsMap by rememberUpdatedState(
                    newValue = AppConst.jsonBuilder.decodeFromString<Map<String, String>>(items)
                )

                val defaultValue = remember { defTag.value["default"] ?: "" }
                AppSpinner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    label = { Text(label) },
                    value = value.ifEmpty { defaultValue },
                    values = itemsMap.keys.toList(),
                    entries = itemsMap.values.toList(),
                    leadingIcon = {
                        if (hint.isNotEmpty())
                            IconButton(onClick = { showHelpDialog = label to hint }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.HelpOutline,
                                    stringResource(id = R.string.help)
                                )
                            }
                    },
                    onSelectedChange = { k, _ ->
                        onInfoChange(
                            info.copy(
                                tagData = info.tagData.toMutableMap().apply {
                                    this[key] = k as String
                                }
                            )
                        )
                    }
                )

            }

        }
    }
}