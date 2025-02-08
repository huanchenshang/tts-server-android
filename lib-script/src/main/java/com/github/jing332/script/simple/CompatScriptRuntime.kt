package com.github.jing332.script.simple

import com.github.jing332.script.runtime.RhinoScriptRuntime
import com.github.jing332.script.simple.ext.JsExtensions

class CompatScriptRuntime(val ttsrv: JsExtensions) : RhinoScriptRuntime() {
    override fun init() {
        super.init()
        global.defineGetter("ttsrv", ::ttsrv)
    }
}