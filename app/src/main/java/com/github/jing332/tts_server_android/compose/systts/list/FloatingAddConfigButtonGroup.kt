package com.github.jing332.tts_server_android.compose.systts.list

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.github.jing332.tts_server_android.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingAddConfigButtonGroup(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    addBgm: () -> Unit,
    addLocal: () -> Unit,
    addPlugin: () -> Unit,
    addGroup: () -> Unit,
) {
    var expended by rememberSaveable { mutableStateOf(false) }
    BackHandler(expended) {
        expended = false
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (expended) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 600, easing = LinearEasing), // 1秒线性动画
        label = "background color animation"
    )

    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .then(
                if (expended)
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        onClickLabel = stringResource(R.string.close)
                    ) { expended = false }
                else Modifier
            )
            .zIndex(1f)
            .semantics {
                invisibleToUser()
            }
    ) {
        val context = LocalContext.current
        FloatingActionButtonMenu(
            expanded = expended,
            button = {
                ToggleFloatingActionButton(
                    modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .semantics {
                            traversalIndex = -1f
                            stateDescription =
                                if (expended) context.getString(R.string.expended) else context.getString(
                                    R.string.collapsed
                                )
                            contentDescription = context.getString(R.string.add_config)
                            role = Role.DropdownList
                        }
                        .animateFloatingActionButton(
                            visible = visible || expended,
                            alignment = Alignment.BottomEnd
                        ),
                    checked = expended,
                    onCheckedChange = { expended = !expended }
                ) {
                    val imageVector by remember {
                        derivedStateOf {
                            if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                        }
                    }
                    Icon(
                        painter = rememberVectorPainter(imageVector),
                        contentDescription = null,
                        modifier = Modifier.animateIcon({ checkedProgress })
                    )
                }
            }
        ) {
            FloatingActionButtonMenuItem(
                onClick = {
                    addLocal()
                    expended = false
                },
                text = { Text(stringResource(R.string.add_local_tts)) },
                icon = { Icon(Icons.Default.PhoneAndroid, null) }
            )
            Spacer(Modifier.height(2.dp))
            FloatingActionButtonMenuItem(
                onClick = {
                    addPlugin()
                    expended = false
                },
                text = { Text(stringResource(R.string.systts_add_plugin_tts)) },
                icon = { Icon(Icons.Default.Javascript, null) }
            )
            Spacer(Modifier.height(2.dp))
            FloatingActionButtonMenuItem(
                onClick = {
                    addBgm()
                    expended = false
                },
                text = { Text(stringResource(R.string.add_bgm_tts)) },
                icon = { Icon(Icons.Default.MusicNote, null) }
            )
            Spacer(Modifier.height(2.dp))
            FloatingActionButtonMenuItem(
                onClick = {
                    addGroup()
                    expended = false
                },
                text = { Text(stringResource(R.string.add_group)) },
                icon = { Icon(Icons.Default.AddCard, null) },
            )
        }
    }
}

