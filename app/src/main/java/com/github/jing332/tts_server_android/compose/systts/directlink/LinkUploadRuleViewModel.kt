package com.github.jing332.tts_server_android.compose.systts.directlink

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.github.jing332.script.runtime.console.ConsoleImpl
import com.github.jing332.tts_server_android.model.rhino.direct_link_upload.DirectUploadEngine
import com.github.jing332.tts_server_android.model.rhino.direct_link_upload.DirectUploadFunction

class LinkUploadRuleViewModel(val app: Application) : AndroidViewModel(app) {
    private lateinit var engine: DirectUploadEngine
    private fun initEngine(code: String) {
        engine = DirectUploadEngine(app, code)
    }

    fun updateCode(code: String) {
        if (::engine.isInitialized)
            engine.code = code
        else initEngine(code)
    }

    fun getConsole(): ConsoleImpl = engine.console

    fun invoke(function: DirectUploadFunction) {
        getConsole().info("${function.name} ...")
        val url = function.invoke("""{"test":"test"}""")
        getConsole().info("url: $url")
    }

    fun save() {
        engine.obtainFunctionList()
    }

    fun debug(): List<DirectUploadFunction> {
        return engine.obtainFunctionList()
    }

    override fun onCleared() {
        super.onCleared()
    }
}