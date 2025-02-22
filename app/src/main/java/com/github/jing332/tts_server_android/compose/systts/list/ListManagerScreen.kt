package com.github.jing332.tts_server_android.compose.systts.list

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drake.net.utils.withIO
import com.github.jing332.common.utils.longToast
import com.github.jing332.compose.widgets.LazyListIndexStateSaver
import com.github.jing332.compose.widgets.TextFieldDialog
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.AbstractListGroup
import com.github.jing332.database.entities.systts.BgmConfiguration
import com.github.jing332.database.entities.systts.GroupWithSystemTts
import com.github.jing332.database.entities.systts.SystemTtsGroup
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.database.entities.systts.TtsConfigurationDTO
import com.github.jing332.database.entities.systts.source.LocalTtsSource
import com.github.jing332.database.entities.systts.source.PluginTtsSource
import com.github.jing332.tts_server_android.AppLocale
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.compose.LocalDrawerState
import com.github.jing332.tts_server_android.compose.LocalNavController
import com.github.jing332.tts_server_android.compose.ShadowReorderableItem
import com.github.jing332.tts_server_android.compose.SharedViewModel
import com.github.jing332.tts_server_android.compose.nav.NavRoutes
import com.github.jing332.tts_server_android.compose.nav.NavTopAppBar
import com.github.jing332.tts_server_android.compose.systts.AuditionDialog
import com.github.jing332.tts_server_android.compose.systts.ConfigDeleteDialog
import com.github.jing332.tts_server_android.compose.systts.ConfigExportBottomSheet
import com.github.jing332.tts_server_android.compose.systts.list.ui.ItemDescriptorFactory
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.QuickEditBottomSheet
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.TagDataClearConfirmDialog
import com.github.jing332.tts_server_android.compose.systts.plugin.PluginSelectionDialog
import com.github.jing332.tts_server_android.compose.systts.sizeToToggleableState
import com.github.jing332.tts_server_android.constant.AppConst
import com.github.jing332.tts_server_android.constant.SpeechTarget
import com.github.jing332.tts_server_android.model.rhino.speech_rule.SpeechRuleEngine
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.toCode
import com.github.jing332.tts_server_android.ui.view.AppDialogs.displayErrorDialog
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ListManagerScreen(
    sharedVM: SharedViewModel,
    vm: ListManagerViewModel = viewModel()
) {
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val drawerState = LocalDrawerState.current

    var showSortDialog by remember { mutableStateOf<List<SystemTtsV2>?>(null) }
    if (showSortDialog != null) SortDialog(
        onDismissRequest = { showSortDialog = null },
        list = showSortDialog!!
    )

    var showQuickEdit by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (showQuickEdit != null) {
        QuickEditBottomSheet(onDismissRequest = {
            dbm.systemTtsV2.insert(showQuickEdit!!)
            if (showQuickEdit?.isEnabled == true) SystemTtsService.notifyUpdateConfig()
            showQuickEdit = null
        }, systts = showQuickEdit!!, onSysttsChange = {
            showQuickEdit = it
        })
    }

    fun navigateToEdit(systts: SystemTtsV2) {
        sharedVM.put(NavRoutes.TtsEdit.DATA, systts)
        navController.navigate(NavRoutes.TtsEdit.id)
    }

    // 长按Item拖拽提示
    var hasShownTip by rememberSaveable { mutableStateOf(false) }

    var showTagClearDialog by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (showTagClearDialog != null) {
        val systts = showTagClearDialog!!
        val config = systts.config as TtsConfigurationDTO
        TagDataClearConfirmDialog(
            tagData = config.speechRule.tagData.toString(),
            onDismissRequest = { showTagClearDialog = null },
            onConfirm = {
                config.speechRule.target = SpeechTarget.ALL
                config.speechRule.resetTag()
                dbm.systemTtsV2.update(systts)
                if (systts.isEnabled) SystemTtsService.notifyUpdateConfig()
                showTagClearDialog = null
            }
        )
    }

    fun switchSpeechTarget(systts: SystemTtsV2) {
        if (!hasShownTip) {
            hasShownTip = true
            context.longToast(R.string.systts_drag_tip_msg)
        }

        val model = systts.copy()
        val config = model.config as TtsConfigurationDTO
        if (config.speechRule.target == SpeechTarget.BGM) return

        if (config.speechRule.target == SpeechTarget.CUSTOM_TAG) dbm.speechRuleDao.getByRuleId(
            config.speechRule.tagRuleId
        )?.let { speechRule ->
            val keys = speechRule.tags.keys.toList()
            val idx = keys.indexOf(config.speechRule.tag)

            val nextIndex = (idx + 1)
            val newTag = keys.getOrNull(nextIndex)
            if (newTag == null) {
                if (config.speechRule.isTagDataEmpty()) {
                    config.speechRule.target = SpeechTarget.ALL
                    config.speechRule.resetTag()
                } else {
                    showTagClearDialog = model
                    return
                }
            } else {
                config.speechRule.tag = newTag
                runCatching {
                    config.speechRule.tagName =
                        SpeechRuleEngine.getTagName(context, speechRule, info = config.speechRule)
                }.onFailure {
                    config.speechRule.tagName = ""
                    context.displayErrorDialog(it)
                }

            }
        }
        else {
            dbm.speechRuleDao.getByRuleId(config.speechRule.tagRuleId)?.let {
                config.speechRule.target = SpeechTarget.CUSTOM_TAG
                config.speechRule.tag = it.tags.keys.first()
            }
        }

        dbm.systemTtsV2.update(model)
        if (model.isEnabled) SystemTtsService.notifyUpdateConfig()
    }

    var deleteTts by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (deleteTts != null) {
        ConfigDeleteDialog(
            onDismissRequest = { deleteTts = null }, content = deleteTts?.displayName ?: ""
        ) {
            dbm.systemTtsV2.delete(deleteTts!!)
            deleteTts = null
        }
    }

    var groupAudioParamsDialog by remember { mutableStateOf<SystemTtsGroup?>(null) }
    if (groupAudioParamsDialog != null) {
        GroupAudioParamsDialog(onDismissRequest = { groupAudioParamsDialog = null },
            params = groupAudioParamsDialog!!.audioParams,
            onConfirm = {
                dbm.systemTtsV2.updateGroup(
                    groupAudioParamsDialog!!.copy(audioParams = it)
                )

                groupAudioParamsDialog = null
            })
    }

    val models by vm.list.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LazyListIndexStateSaver(models = models, listState = listState)

    val reorderState = rememberReorderableLazyListState(
        listState = listState, onMove = vm::reorder
    )

    var addGroupDialog by remember { mutableStateOf(false) }
    if (addGroupDialog) {
        var name by remember { mutableStateOf("") }
        TextFieldDialog(title = stringResource(id = R.string.add_group),
            text = name,
            onTextChange = { name = it },
            onDismissRequest = { addGroupDialog = false }) {
            addGroupDialog = false
            dbm.systemTtsV2.insertGroup(SystemTtsGroup(name = name))
        }
    }

    var showGroupExportSheet by remember { mutableStateOf<List<GroupWithSystemTts>?>(null) }
    if (showGroupExportSheet != null) {
        val list = showGroupExportSheet!!
        ListExportBottomSheet(onDismissRequest = { showGroupExportSheet = null }, list = list)
    }

    var showExportSheet by remember { mutableStateOf<List<SystemTtsV2>?>(null) }
    if (showExportSheet != null) {
        val jStr = remember { AppConst.jsonBuilder.encodeToString(showExportSheet!!) }
        ConfigExportBottomSheet(json = jStr) { showExportSheet = null }
    }

    var addPluginDialog by remember { mutableStateOf(false) }
    if (addPluginDialog) {
        PluginSelectionDialog(onDismissRequest = { addPluginDialog = false }) {
            navigateToEdit(
                SystemTtsV2(
                    config = TtsConfigurationDTO(
                        source = PluginTtsSource(
                            pluginId = it.pluginId,
                            locale = AppLocale.current(context).toCode()
                        )
                    )
                )
            )
        }
    }

    var showAuditionDialog by remember { mutableStateOf<SystemTtsV2?>(null) }
    if (showAuditionDialog != null) AuditionDialog(systts = showAuditionDialog!!) {
        showAuditionDialog = null
    }

    var showOptions by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NavTopAppBar(drawerState = drawerState, title = {
                Text(stringResource(id = R.string.system_tts))
            }, actions = {
                var showAddMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showAddMenu = true }) {
                    Icon(Icons.Default.Add, stringResource(id = R.string.add_config))

                    DropdownMenu(expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false }) {

                        @Composable
                        fun MenuItem(
                            icon: @Composable () -> Unit,
                            @StringRes title: Int,
                            onClick: () -> Unit
                        ) {
                            DropdownMenuItem(text = {
                                Text(stringResource(id = title))
                            }, onClick = {
                                showAddMenu = false
                                onClick()
                            }, leadingIcon = icon)
                        }


                        MenuItem(
                            icon = { Icon(Icons.Default.PhoneAndroid, null) },
                            title = R.string.add_local_tts
                        ) {
                            navigateToEdit(
                                SystemTtsV2(
                                    config = TtsConfigurationDTO(
                                        source = LocalTtsSource(locale = AppConst.localeCode)
                                    )
                                )
                            )
                        }

                        MenuItem(
                            icon = { Icon(Icons.Default.Javascript, null) },
                            title = R.string.systts_add_plugin_tts
                        ) {
                            addPluginDialog = true
                        }

                        MenuItem(
                            icon = { Icon(Icons.Default.Audiotrack, null) },
                            title = R.string.add_bgm_tts
                        ) {
                            navigateToEdit(SystemTtsV2(config = BgmConfiguration()))
                        }

                        MenuItem(
                            icon = { Icon(Icons.Default.AddCard, null) },
                            title = R.string.add_group
                        ) {
                            addGroupDialog = true
                        }
                    }
                }

                IconButton(onClick = { showOptions = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(id = R.string.more_options))
                    MenuMoreOptions(
                        expanded = showOptions,
                        onDismissRequest = { showOptions = false },
                        onExportAll = { showGroupExportSheet = models },
                    )
                }
            })
        },
    ) { paddingValues ->
        Box(Modifier.padding(top = paddingValues.calculateTopPadding())) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .reorderable(state = reorderState),
                state = listState
            ) {
                models.forEachIndexed { _, groupWithSystemTts ->
                    val g = groupWithSystemTts.group
                    val checkState =
                        groupWithSystemTts.list.filter { it.isEnabled }.size.sizeToToggleableState(
                            groupWithSystemTts.list.size
                        )
                    val key = "g_${g.id}"
                    stickyHeader(key = key) {
                        ShadowReorderableItem(reorderableState = reorderState, key = key) {
                            Group(modifier = Modifier.detectReorderAfterLongPress(reorderState),
                                name = g.name,
                                isExpanded = g.isExpanded,
                                toggleableState = checkState,
                                onToggleableStateChange = {
                                    vm.updateGroupEnable(groupWithSystemTts, it)
                                },
                                onClick = {
                                    dbm.systemTtsV2.updateGroup(g.copy(isExpanded = !g.isExpanded))
                                },
                                onDelete = {
                                    dbm.systemTtsV2.delete(*groupWithSystemTts.list.toTypedArray())
                                    dbm.systemTtsV2.deleteGroup(g)
                                },
                                onRename = {
                                    dbm.systemTtsV2.updateGroup(g.copy(name = it))
                                },
                                onCopy = {
                                    scope.launch {
                                        val group = g.copy(id = System.currentTimeMillis(),
                                            name = it.ifBlank { context.getString(R.string.unnamed) })
                                        dbm.systemTtsV2.insertGroup(group)
                                        dbm.systemTtsV2.getByGroup(g.id)
                                            .forEachIndexed { index, tts ->
                                                dbm.systemTtsV2.insert(
                                                    tts.copy(
                                                        id = System.currentTimeMillis() + index,
                                                        groupId = group.id
                                                    )
                                                )
                                            }
                                    }
                                },
                                onEditAudioParams = {
                                    groupAudioParamsDialog = g
                                },
                                onExport = {
                                    showGroupExportSheet = listOf(groupWithSystemTts)
                                },
                                onSort = {
                                    showSortDialog = groupWithSystemTts.list
                                }
                            )
                        }
                    }

                    if (g.isExpanded) {
                        itemsIndexed(groupWithSystemTts.list.sortedBy { it.order },
                            key = { _, v -> "${g.id}_${v.id}" }) { _, item ->
                            if (g.id == 1L) println(item.displayName + ", " + item.order)

                            ShadowReorderableItem(
                                reorderableState = reorderState,
                                key = "${g.id}_${item.id}"
                            ) {
                                val descriptor = remember(item) {
                                    ItemDescriptorFactory.from(context, item)
                                }
                                Item(reorderState = reorderState,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    name = item.displayName ?: "",
                                    tagName = descriptor.tagName,
                                    type = descriptor.type,
                                    standby = descriptor.standby,
                                    enabled = item.isEnabled,
                                    onEnabledChange = {
                                        vm.updateTtsEnabled(item, it)
                                        if (it) SystemTtsService.notifyUpdateConfig()
                                    },
                                    desc = descriptor.desc,
                                    params = descriptor.bottom,
                                    onClick = { showQuickEdit = item },
                                    onLongClick = { switchSpeechTarget(item) },
                                    onCopy = {
                                        navigateToEdit(item.copy(id = System.currentTimeMillis()))
                                    },
                                    onDelete = { deleteTts = item },
                                    onEdit = { navigateToEdit(item) },
                                    onAudition = { showAuditionDialog = item },
                                    onExport = {
                                        showExportSheet =
                                            listOf(item.copy(groupId = AbstractListGroup.DEFAULT_GROUP_ID))
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(60.dp))
                }
            }


            LaunchedEffect(key1 = Unit) {
                withIO {
                    vm.checkListData(context)
                }
            }

        }
    }
}