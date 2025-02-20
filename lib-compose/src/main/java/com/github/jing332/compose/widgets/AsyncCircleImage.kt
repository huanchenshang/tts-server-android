package com.github.jing332.compose.widgets

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun AsyncCircleImage(
    model: Any? = null,
    contentDescription: String? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier.size(32.dp),
) {
    AsyncImage(
        model,
        contentDescription,
        modifier = modifier
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}