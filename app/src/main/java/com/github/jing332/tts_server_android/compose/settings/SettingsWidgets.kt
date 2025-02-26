package com.github.jing332.tts_server_android.compose.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jing332.compose.widgets.AppDialog
import com.github.jing332.compose.widgets.LabelSlider
import com.github.jing332.tts_server_android.R

internal val horizontalPadding: Dp = 16.dp
internal val verticalPadding: Dp = 12.dp

@Composable
internal fun DropdownPreference(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit,
    actions: @Composable ColumnScope. () -> Unit = {},
) {
    BasePreferenceWidget(modifier = modifier, icon = icon, onClick = {
        onExpandedChange(true)
    }, title = title, subTitle = subTitle) {
        DropdownMenu(
            modifier = Modifier.align(Alignment.Top),
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }) {
            actions()
        }
    }
}

@Composable
internal fun DividerPreference(title: @Composable () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = verticalPadding + 4.dp)
            .semantics(true) {
                contentDescription = context.getString(R.string.divider_preference_desc)
            }
            .minimumInteractiveComponentSize()
    ) {
        Row(
            Modifier
                .padding(vertical = 8.dp)
                .align(Alignment.Start)
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
            ) {
                title()
            }
        }
    }

}

@Composable
internal fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit,
    icon: @Composable () -> Unit = {},

    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    BasePreferenceWidget(
        modifier = modifier
            .focusable()
            .toggleable(
                role = Role.Switch,
                value = checked,
                enabled = true,
                onValueChange = { onCheckedChange(!checked) }),

        title = title,
        subTitle = subTitle,
        icon = icon,
        content = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.align(Alignment.CenterVertically)

            )
        }
    )
}

@Composable
internal fun BasePreferenceWidget(
    modifier: Modifier = Modifier,
    role: Role? = null,
    onClick: (() -> Unit)? = null,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit = {},
    icon: @Composable () -> Unit = {},
    content: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier = Modifier
        .minimumInteractiveComponentSize()
        .defaultMinSize(minHeight = 64.dp)
        .clip(MaterialTheme.shapes.extraSmall)
        .then(
            if (onClick == null) Modifier else Modifier.clickable(
                role = role,
                onClick = onClick
            )
        )
        .then(modifier)
        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        .semantics(true) {}
    ) {
        Column(
            Modifier.align(Alignment.CenterVertically)
        ) {
            icon()
        }

        Column(
            Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
                .padding(horizontal = 8.dp)
        ) {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleMedium) {
                title()
            }

            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                subTitle()
            }
        }

        Row(
            Modifier
                .align(Alignment.CenterVertically)
        ) {
            content()
        }
    }
}


@Composable
internal fun SliderPreference(
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit,
    icon: @Composable () -> Unit = {},
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    label: String,
) {
    val view = LocalView.current
    LaunchedEffect(value) {
        view.announceForAccessibility(value.toString())
    }

    PreferenceDialog(
        title = title,
        subTitle = subTitle,
        dialogContent = {
            LabelSlider(
                modifier = Modifier.padding(vertical = 16.dp),
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                buttonSteps = 1f,
                buttonLongSteps = 2f,
                text = label
            )
        },
        icon = icon,
        endContent = { Text(label) }
    )
}

@Composable
internal fun PreferenceDialog(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subTitle: @Composable () -> Unit,
    icon: @Composable () -> Unit,

    dialogContent: @Composable ColumnScope.() -> Unit,
    endContent: @Composable RowScope.() -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        AppDialog(title = title, content = {
            Column {
                dialogContent()
            }
        }, buttons = {
            TextButton(onClick = { showDialog = false }) {
                Text(stringResource(id = R.string.close))
            }
        }, onDismissRequest = { showDialog = false })
    }
    BasePreferenceWidget(modifier, onClick = {
        showDialog = true
    }, title = title, icon = icon, subTitle = subTitle) {
        endContent()
    }
}