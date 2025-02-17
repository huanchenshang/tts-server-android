package com.github.jing332.script.runtime

import com.github.jing332.script.runtime.console.Console
import com.github.jing332.script.runtime.console.ConsoleUtils.Companion.putLogger
import com.github.jing332.script.runtime.console.ConsoleUtils.Companion.write
import com.github.jing332.script.withRhinoContext
import org.mozilla.javascript.NativeConsole
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.ScriptStackElement
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

open class RhinoScriptRuntime(
    var environment: Environment = Environment("", ""),
    var console: Console = Console(),
) {
    companion object {
        val sharedScope: ScriptableObject by lazy {
            withRhinoContext { cx ->
                RhinoTopLevel(cx)
            }
        }
    }

    val global: ScriptableObject by lazy {
        withRhinoContext { cx ->
            (cx.newObject(sharedScope) as ScriptableObject).apply {
                prototype = sharedScope
                parentScope = null
            }
        }
    }

    /**
     * Call from [com.github.jing332.script.rhino.RhinoScriptEngine.setRuntime]
     */
    open fun init() {
        NativeConsole.init(global, false, object : NativeConsole.ConsolePrinter {
            override fun print(
                cx: org.mozilla.javascript.Context,
                scope: Scriptable,
                level: NativeConsole.Level,
                args: Array<out Any?>,
                stack: Array<out ScriptStackElement?>?,
            ) {
                console.write(cx, scope, args, level, stack)
            }
        })
        val logger = NativeObject()
        console.putLogger(logger, "log", NativeConsole.Level.INFO)
        console.putLogger(logger, "t", NativeConsole.Level.TRACE)
        console.putLogger(logger, "d", NativeConsole.Level.DEBUG)
        console.putLogger(logger, "i", NativeConsole.Level.INFO)
        console.putLogger(logger, "w", NativeConsole.Level.WARN)
        console.putLogger(logger, "e", NativeConsole.Level.ERROR)
        global.defineProperty("logger", logger, ScriptableObject.READONLY)
        console.putLogger(global, "println", NativeConsole.Level.INFO)

        global.defineGetter("environment", ::environment)
    }

    protected fun ScriptableObject.defineGetter(key: String, getter: () -> Any) {
        defineProperty(key, getter, null, ScriptableObject.READONLY)
    }
}