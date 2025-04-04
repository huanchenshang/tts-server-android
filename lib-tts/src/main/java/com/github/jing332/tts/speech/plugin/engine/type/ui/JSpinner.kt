@file:Suppress("unused")

package com.github.jing332.tts.speech.plugin.engine.type.ui

import android.annotation.SuppressLint
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.github.jing332.compose.widgets.AppSpinner
import kotlin.math.max

@Suppress("MemberVisibilityCanBePrivate")
@SuppressLint("ViewConstructor")
class JSpinner(context: Context, val hint: CharSequence) : FrameLayout(context) {
    companion object {
        const val TAG = "JSpinner"
    }

    private val mItems = mutableStateListOf<Item>()
    var items: List<Item>
        get() = mItems
        set(value) {
            mItems.clear()
            mItems += value
        }

    private var mSelectedPosition by mutableIntStateOf(0)

    var selectedPosition: Int
        get() = mSelectedPosition
        set(value) {
            mSelectedPosition = value
        }

    var value: Any
        get() = items[selectedPosition]
        set(value) {
            selectedPosition = max(0, mItems.indexOfFirst { it.value == value })
        }

    private var mListener: OnItemSelectedListener? = null

    interface OnItemSelectedListener {
        fun onItemSelected(spinner: JSpinner, position: Int, item: Item)
    }

    fun setOnItemSelected(listener: OnItemSelectedListener?) {
        mListener = listener
    }

    init {
        val composeView = ComposeView(context)
        addView(composeView)
        composeView.setContent {
            Content()
        }
    }

    @Composable
    fun Content() {
        val item = mItems.getOrElse(mSelectedPosition) { mItems.getOrNull(0) } ?: Item("null", Unit)
        if (mItems.isEmpty()) {
            AppSpinner(
                labelText =  hint.toString(),
                value = item.value,
                values = listOf(Unit),
                entries = listOf(stringResource(id = com.github.jing332.compose.R.string.empty_list)),
                enabled = false,
                onSelectedChange = { _, _ -> }
            )
        } else
            AppSpinner(
                labelText =  hint.toString(),
                value = item.value,
                values = mItems.map { it.value },
                entries = mItems.map { it.name.toString() },
                onSelectedChange = { value, _ ->
                    val index = mItems.indexOfFirst { it.value == value }
                    mSelectedPosition = index
                    mListener?.onItemSelected(
                        spinner = this@JSpinner, position = index, item = mItems[index]
                    )
                }
            )
    }


}