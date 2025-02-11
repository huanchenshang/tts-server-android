package com.github.jing332.script.runtime

import android.content.Context
import com.github.jing332.script.runtime.console.Console
import com.github.jing332.script.runtime.console.GlobalConsole
import com.github.jing332.script.withRhinoContext
import org.mozilla.javascript.ScriptableObject

open class RhinoScriptRuntime() {
    // Top level scope
    val global: RhinoGlobal = withRhinoContext { cx -> RhinoGlobal(cx) }

    var console: Console = GlobalConsole.get()
    val http: Http = Http()

    /**
     * Call from [com.github.jing332.script.rhino.RhinoScriptEngine.setRuntime]
     */
    open fun init() {
        global.defineGetter("logger", ::console)
        global.defineGetter("console", ::console)
        global.defineGetter("http", ::http)
    }

    protected fun ScriptableObject.defineGetter(key: String, getter: () -> Any) {
        defineProperty(key, getter, null, ScriptableObject.READONLY)
    }
}