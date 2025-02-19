package com.github.jing332.tts_server_android.compose.systts.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.github.jing332.compose.widgets.CenterTextImage
import com.github.jing332.database.dbm
import com.github.jing332.database.entities.plugin.Plugin
import com.github.jing332.tts_server_android.R

@Composable
fun PluginSelectionDialog(onDismissRequest: () -> Unit, onSelect: (Plugin) -> Unit) {
    AlertDialog(onDismissRequest = onDismissRequest,
        title = { Text(stringResource(id = R.string.select_plugin)) },
        text = {
            val plugins = dbm.pluginDao.allEnabled
            if (plugins.isEmpty())
                Text(
                    stringResource(id = R.string.no_plugins),
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )

            LazyColumn {
                items(plugins, { it.id }) { plugin ->
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(MaterialTheme.shapes.small)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple()
                            ) { onSelect(plugin) }
                            .padding(vertical = 4.dp)) {
                        SubcomposeAsyncImage(
                            plugin.iconUrl,
                            null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(32.dp),
                            error = {
                                CenterTextImage(plugin.name.first().toString())

                            }
                        )

                        Column {
                            Text(
                                text = plugin.name,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = plugin.pluginId,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )


}