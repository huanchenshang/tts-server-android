package com.github.jing332.tts_server_android.compose.systts

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.github.jing332.common.LogEntry

class TtsLogViewModel : ViewModel() {
    val logs = mutableStateListOf<LogEntry>()
}