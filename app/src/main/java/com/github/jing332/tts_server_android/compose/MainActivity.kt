@file:Suppress("DEPRECATION")

package com.github.jing332.tts_server_android.compose

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.jing332.common.DateFormatConst
import com.github.jing332.common.utils.longToast
import com.github.jing332.common.utils.performLongPress
import com.github.jing332.common.utils.toast
import com.github.jing332.compose.widgets.AppLauncherIcon
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.systts.SystemTtsV2
import com.github.jing332.tts_server_android.BuildConfig
import com.github.jing332.tts_server_android.R
import com.github.jing332.tts_server_android.ShortCuts
import com.github.jing332.tts_server_android.compose.forwarder.systts.SystemTtsForwarderScreen
import com.github.jing332.tts_server_android.compose.nav.NavRoutes
import com.github.jing332.tts_server_android.compose.settings.SettingsScreen
import com.github.jing332.tts_server_android.compose.systts.SystemTtsScreen
import com.github.jing332.tts_server_android.compose.systts.list.ui.widgets.TtsEditContainerScreen
import com.github.jing332.tts_server_android.compose.theme.AppTheme
import com.github.jing332.tts_server_android.conf.AppConfig
import com.github.jing332.tts_server_android.service.systts.SystemTtsService
import com.github.jing332.tts_server_android.ui.AppHelpDocumentActivity
import com.github.jing332.tts_server_android.utils.MyTools.killBattery
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch


val LocalNavController = compositionLocalOf<NavHostController> { error("No nav controller") }
val LocalDrawerState = compositionLocalOf<DrawerState> { error("No drawer state") }

fun Context.asAppCompatActivity(): AppCompatActivity {
    return this as? AppCompatActivity ?: error("Context is not an AppCompatActivity")
}

fun Context.asActivity(): Activity {
    return this as? Activity ?: error("Context is not an Activity")
}

private var updateCheckTrigger by mutableStateOf(false)

class MainActivity : AppCompatActivity() {
    companion object {
        private val logger = KotlinLogging.logger { this::class.java.name }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val json = """
//           [ { "group": { "id": 1, "name": "默认分组" }, "list": [ { "id": 1681521093149, "displayName": "bgm", "speechRule": { "target": 3 }, "tts": { "#type": "bgm", "musicList": [ "/storage/emulated/0/Music/听琴如修禅-巫娜古琴100首" ], "volume": 87, "audioFormat": { } } }, { "id": 1734523614731, "displayName": "王隔壁_剧有范MK-III[男|青年]", "tts": { "#type": "plugin", "pluginId": "www.gstudios.com", "locale": "zh-CN", "voice": "758", "rate": 52, "audioFormat": { "sampleRate": 48000 } }, "order": 2 }, { "id": 1738145879439, "displayName": "MultiTTS", "tts": { "#type": "local", "engine": "org.nobody.multitts", "locale": "zh-CN", "voiceName": "bdetts_f32pangbai", "isDirectPlayMode": false, "audioFormat": { } }, "order": 1 } ] }, { "group": { "id": 1681999286014, "name": "呱呱", "order": 1, "audioParams": { "speed": 1.3 } }, "list": [ { "id": 1681529196730, "groupId": 1681999286014, "displayName": "百合 [现言|古言]", "speechRule": { "target": 4, "tag": "narration", "tagRuleId": "ttsrv.multi_voice", "tagName": "旁白" }, "tts": { "#type": "plugin", "pluginId": "www.gstudios.com", "locale": "zh-CN", "voice": "4", "rate": 1, "audioFormat": { "sampleRate": 48000 }, "audioPlayer": { "rate": 1.0, "pitch": 1.0, "volume": 1.0 } } }, { "id": 1681999236498, "groupId": 1681999286014, "displayName": "芷嫣 [现言|古言]", "speechRule": { "target": 4, "tag": "dialogue", "tagRuleId": "ttsrv.multi_voice", "tagName": "对话" }, "tts": { "#type": "plugin", "pluginId": "www.gstudios.com", "locale": "zh-CN", "voice": "53", "audioFormat": { "sampleRate": 48000 }, "audioPlayer": { "rate": 1.0, "pitch": 1.0, "volume": 1.0 }, "audioParams": { "volume": 2.11 } }, "order": 1 }, { "id": 1684907918205, "groupId": 1681999286014, "displayName": "芷嫣MK-II [现言|古言]", "tts": { "#type": "plugin", "pluginId": "www.gstudios.com", "locale": "zh-CN", "voice": "139", "rate": 49, "audioFormat": { "sampleRate": 48000 }, "audioPlayer": { "rate": 1.0, "pitch": 1.0, "volume": 1.0 } }, "order": 2 } ] }, { "group": { "id": 1704010979156, "name": "vits", "order": 2 }, "list": [ { "id": 1704010998111, "groupId": 1704010979156, "displayName": "八重神子_ZH", "tts": { "#type": "plugin", "pluginId": "v2.genshinvoice.top", "locale": "zh-CN", "voice": "八重神子_ZH", "audioFormat": { "sampleRate": 44100 } } } ] }, { "group": { "id": 1681820023716, "name": "本地", "order": 3 }, "list": [ { "id": 1681820029153, "groupId": 1681820023716, "displayName": "搜狗TTS (org.nobody.sgtts)", "speechRule": { "target": 4, "tag": "dialogue", "tagRuleId": "ttsrv.multi_voice", "tagName": "对话", "tagData": { "defaultLanguage": "true" } }, "tts": { "#type": "local", "engine": "org.nobody.sgtts", "locale": "zh-CN", "voiceName": "qingfeng", "rate": 85, "audioFormat": { } }, "order": 1 }, { "id": 1681820053515, "groupId": 1681820023716, "displayName": "搜狗TTS (org.nobody.sgtts)", "speechRule": { "target": 4, "tag": "dialogue", "tagRuleId": "ttsrv.multi_voice", "tagName": "对话" }, "tts": { "#type": "local", "engine": "org.nobody.sgtts", "locale": "zh-CN", "voiceName": "yaxinpro", "pitch": 72, "rate": 69, "audioFormat": { } } }, { "id": 1692514349137, "groupId": 1681820023716, "displayName": "度小宇", "tts": { "#type": "plugin", "pluginId": "tsn.baidu.com", "locale": "zh", "voice": "1", "audioFormat": { } }, "order": 2 } ] }, { "group": { "id": 12333, "name": "示例-旁白对话BGM", "order": 4 }, "list": [ { "id": 1690331046541, "groupId": 12333, "displayName": "⚠️请在右上角打开多语音！晓晓（zh-CN-XiaoxiaoNeural）", "speechRule": { "target": 4, "tag": "dialogue", "tagRuleId": "ttsrv.multi_voice" }, "tts": { "#type": "internal" } }, { "id": 1690331074092, "groupId": 12333, "displayName": "云健（zh-CN-YunjianNeural）", "speechRule": { "target": 4, "tag": "narration", "tagRuleId": "ttsrv.multi_voice" }, "tts": { "#type": "internal", "voiceName": "zh-CN-YunjianNeural" }, "order": 1 } ] }, { "group": { "id": 1689744455358, "name": "微软专业", "order": 5, "isExpanded": true }, "list": [ { "id": 1689744463741, "groupId": 1689744455358, "displayName": "云希 (zh-CN-YunxiNeural)", "isEnabled": true, "speechRule": { "tag": "narration", "tagRuleId": "ttsrv.multi_voice", "tagName": "旁白" }, "tts": { "#type": "plugin", "pluginId": "com.microsoft.translator", "locale": "zh-CN", "voice": "zh-CN-YunxiNeural", "data": { "style": "newscast", "languageSkill": "zh-CN", "role": "Boy", "styleDegree": "0.96" }, "volume": 100, "rate": 52, "audioFormat": { "sampleRate": 24000 } } } ] }, { "group": { "id": 1681554761681, "name": "微软", "order": 6 }, "list": [ { "id": 1681554671030, "groupId": 1681554761681, "displayName": "晓甄 (zh-CN-XiaozhenNeural)", "tts": { "#type": "plugin", "pluginId": "com.microsoft.translator", "locale": "zh-CN", "voice": "zh-CN-YunxiNeural", "data": { "style": "narration-relaxed", "styleDegree": "1.55", "role": "Narrator" }, "volume": 43, "rate": 62, "audioFormat": { "sampleRate": 24000 }, "audioParams": { "speed": 1.0, "volume": 1.0, "pitch": 1.0 } }, "order": 1 }, { "id": 1682419313176, "groupId": 1681554761681, "displayName": "Xiaochen Multilingual (zh-CN-XiaochenMultilingualNeural)", "tts": { "#type": "plugin", "pluginId": "com.microsoft.translator", "locale": "zh-CN", "voice": "zh-CN-XiaochenMultilingualNeural", "data": { "style": "", "styleDegree": "1.00", "role": "Boy", "languageSkill": "" }, "volume": 100, "rate": 61, "audioFormat": { "sampleRate": 24000 } } } ] } ]
//        """.trimIndent()
//
//        AppConst.jsonBuilder.decodeFromString<List<GroupWithV1TTS>>(json).forEach {
//            dbm.systemTtsDao.insertGroup(it.group)
//            it.list.forEach {
//                dbm.systemTtsDao.insertTts(it)
//            }
//        }


        ShortCuts.buildShortCuts(this)
        setContent {
            AppTheme {
                var showAutoCheckUpdaterDialog by remember { mutableStateOf(false) }
                if (showAutoCheckUpdaterDialog) {
                    val fromUser by remember { mutableStateOf(updateCheckTrigger) }
                    AutoUpdateCheckerDialog(fromUser, fromAction = true) {
                        showAutoCheckUpdaterDialog = false
                        updateCheckTrigger = false
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // A13
                    val notificationPermission =
                        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
                    if (!notificationPermission.status.isGranted) {
                        LaunchedEffect(notificationPermission) {
                            notificationPermission.launchPermissionRequest()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    showAutoCheckUpdaterDialog = AppConfig.isAutoCheckUpdateEnabled.value
                }

                val excludeFromRecent by AppConfig.isExcludeFromRecent
                LaunchedEffect(excludeFromRecent) {
                    (application.getSystemService(ACTIVITY_SERVICE) as ActivityManager).let { manager ->
                        manager.appTasks.forEach { task ->
                            task?.setExcludeFromRecents(excludeFromRecent)
                        }
                    }
                }

                LaunchedEffect(updateCheckTrigger) {
                    if (updateCheckTrigger) showAutoCheckUpdaterDialog = true
                }

                MainScreen { finish() }
            }
        }
    }
}

@Composable
private fun MainScreen(finish: () -> Unit) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val entryState by navController.currentBackStackEntryAsState()

    val gesturesEnabled = remember(entryState) {
        NavRoutes.routes.find { it.id == entryState?.destination?.route } != null
    }

    var lastBackDownTime by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = drawerState.isClosed) {
        val duration = 2000
        SystemClock.elapsedRealtime().let {
            if (it - lastBackDownTime <= duration) {
                finish()
            } else {
                lastBackDownTime = it
                context.toast(R.string.app_down_again_to_exit)
            }
        }
    }
    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalDrawerState provides drawerState,
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = gesturesEnabled,
            drawerContent = {
                NavDrawerContent(
                    Modifier
                        .fillMaxHeight()
                        .width(300.dp)
                        .clip(
                            MaterialTheme.shapes.large.copy(
                                topStart = CornerSize(0.dp),
                                bottomStart = CornerSize(0.dp)
                            )
                        )
                        .background(MaterialTheme.colorScheme.background)
                        .padding(12.dp),
                    navController,
                    drawerState,
                )
            }) {

            val sharedVM: SharedViewModel = viewModel()
            NavHost(
                navController = navController,
                startDestination = NavRoutes.SystemTTS.id
            ) {
                composable(NavRoutes.SystemTTS.id) { SystemTtsScreen(sharedVM) }
                composable(NavRoutes.SystemTtsForwarder.id) {
                    SystemTtsForwarderScreen()
                }
                composable(NavRoutes.Settings.id) { SettingsScreen(drawerState) }

                composable(NavRoutes.TtsEdit.id) {
                    var stateSystemTts by rememberSaveable {
                        mutableStateOf(
                            checkNotNull(sharedVM.getOnce<SystemTtsV2>(NavRoutes.TtsEdit.DATA)) {
                                "Not found systemTts from sharedVM"
                            }
                        )
                    }

                    TtsEditContainerScreen(
                        modifier = Modifier
                            .fillMaxSize(),
                        systts = stateSystemTts,
                        onSysttsChange = {
                            stateSystemTts = it
                            println("UpdateSystemTTS: $it")
                        },
                        onSave = {
                            navController.popBackStack()
                            dbm.systemTtsV2.insert(stateSystemTts)
                            if (stateSystemTts.isEnabled) SystemTtsService.notifyUpdateConfig()
                        },
                        onCancel = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }

    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NavDrawerContent(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    drawerState: DrawerState,
) {
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    @Composable
    fun DrawerItem(
        selected: Boolean = false,
        icon: @Composable () -> Unit,
        label: @Composable () -> Unit,
        onClick: () -> Unit,
    ) {
        NavigationDrawerItem(
            modifier = Modifier.padding(vertical = 2.dp),
            icon = icon,
            label = label,
            selected = selected,
            onClick = onClick,
            colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
        )
    }


    @Composable
    fun NavDrawerItem(
        icon: @Composable () -> Unit,
        targetScreen: NavRoutes,
        onClick: () -> Unit = {
            scope.launch { drawerState.close() }
            navController.navigateSingleTop(targetScreen.id, popUpToMain = true)
        },
    ) {
        val isSelected = navController.currentDestination?.route == targetScreen.id
        DrawerItem(
            icon = icon,
            label = { Text(text = stringResource(id = targetScreen.strId)) },
            selected = isSelected,
            onClick = onClick,
        )
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(24.dp))
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current
        val view = LocalView.current

        var isBuildTimeExpanded by remember { mutableStateOf(false) }
        val versionNameText = remember {
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        }
        Column(modifier = Modifier
            .padding(end = 4.dp)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = {
                    isBuildTimeExpanded = !isBuildTimeExpanded
                },
                onLongClick = {
                    view.performLongPress()
                    clipboardManager.setText(AnnotatedString(versionNameText))
                    context.longToast(R.string.copied)
                }
            )) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLauncherIcon(Modifier.size(64.dp), R.mipmap.ic_app_launcher_round)
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = versionNameText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            AnimatedVisibility(visible = isBuildTimeExpanded) {
                Text(
                    text = DateFormatConst.dateFormatSec.format(BuildConfig.BUILD_TIME * 1000),
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp)
        )

        for (route in NavRoutes.routes) {
            NavDrawerItem(icon = route.icon, targetScreen = route)
        }

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp)
        )

        DrawerItem(
            icon = { Icon(Icons.Default.BatteryFull, null) },
            label = { Text(stringResource(id = R.string.battery_optimization_whitelist)) },
            selected = false,
            onClick = { context.killBattery() }
        )

        DrawerItem(
            icon = { Icon(Icons.Default.ArrowCircleUp, null) },
            label = { Text(stringResource(id = R.string.check_update)) },
            selected = false,
            onClick = {
                scope.launch {
                    drawerState.close()
                    updateCheckTrigger = true
                }
            },
        )

        DrawerItem(
            icon = { Icon(Icons.Default.HelpOutline, null) },
            label = { Text(stringResource(id = R.string.app_help_document)) },
            selected = false,
            onClick = {
                context.startActivity(Intent(context, AppHelpDocumentActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                })
            },
        )

        var showAboutDialog by remember { mutableStateOf(false) }
        if (showAboutDialog)
            AboutDialog { showAboutDialog = false }

        DrawerItem(
            icon = { Icon(Icons.Default.Info, null) },
            label = { Text(stringResource(id = R.string.about)) },
            selected = false,
            onClick = { showAboutDialog = true }
        )
    }
}

/**
 * 单例并清空其他栈
 */
fun NavHostController.navigateSingleTop(
    route: String,
    popUpToMain: Boolean = false,
) {
    val navController = this
    val navOptions = NavOptions.Builder()
        .setLaunchSingleTop(true)
        .apply {
            if (popUpToMain) setPopUpTo(
                navController.graph.startDestinationId,
                inclusive = false,
                saveState = true
            )
        }
        .setRestoreState(true)
        .build()

    navController.navigate(route, navOptions)
}