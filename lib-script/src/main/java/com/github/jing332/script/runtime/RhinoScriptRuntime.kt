package com.github.jing332.script.runtime

import com.github.jing332.common.LogLevel
import com.github.jing332.script.jsToString
import com.github.jing332.script.runtime.console.Console
import com.github.jing332.script.withRhinoContext
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeConsole
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.ScriptStackElement
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined

open class RhinoScriptRuntime(
    // Top level scope
    val global: RhinoGlobal = withRhinoContext { cx -> RhinoGlobal(cx) },

    var console: Console = Console(),
    val http: Http = Http(),
) {

    private fun toLogLevel(l: NativeConsole.Level) = when (l) {
        NativeConsole.Level.TRACE -> LogLevel.TRACE
        NativeConsole.Level.DEBUG -> LogLevel.DEBUG
        NativeConsole.Level.INFO -> LogLevel.INFO
        NativeConsole.Level.WARN -> LogLevel.WARN
        NativeConsole.Level.ERROR -> LogLevel.ERROR
    }

    fun log(vararg args: Any?): String {
        if (args.isEmpty()) {
            return ""
        }

        val arg0 = args[0]
        val formatString =
            if (arg0 is String && arg0.contains('%')) arg0
            else null

        if (formatString != null) {
            if (args.size == 1) {
                return formatString
            } else {
                val formattedArgs = args.drop(1).toTypedArray()
                try {
                    return String.format(formatString, *formattedArgs)
                } catch (_: Exception) {
                }
            }
        }


        return args.joinToString(" ") { jsToString(it ?: "") }
    }

    private fun regLoggerToObj(obj: ScriptableObject, name: String, level: NativeConsole.Level) {
        obj.defineProperty(name, object : BaseFunction() {
            override fun call(
                cx: Context,
                scope: Scriptable,
                thisObj: Scriptable,
                args: Array<out Any?>,
            ): Any {
                write(args, level)
                return Undefined.instance
            }
        }, ScriptableObject.READONLY)
    }

    private fun write(
        args: Array<out Any?>,
        level: NativeConsole.Level,
        stack: Array<out ScriptStackElement?>? = null,
    ) {
        val str = log(*args)
        console.write(
            toLogLevel(level),
            if (level == NativeConsole.Level.TRACE) str + "\n" + stack.contentToString() else str
        )
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
                write(args, level, stack)
            }
        })
        val logger = NativeObject()
        regLoggerToObj(logger, "log", NativeConsole.Level.INFO)
        regLoggerToObj(logger, "t", NativeConsole.Level.TRACE)
        regLoggerToObj(logger, "d", NativeConsole.Level.DEBUG)
        regLoggerToObj(logger, "i", NativeConsole.Level.INFO)
        regLoggerToObj(logger, "w", NativeConsole.Level.WARN)
        regLoggerToObj(logger, "e", NativeConsole.Level.ERROR)
        global.defineProperty("logger", logger, ScriptableObject.READONLY)

        global.defineGetter("http", ::http)
    }

    protected fun ScriptableObject.defineGetter(key: String, getter: () -> Any) {
        defineProperty(key, getter, null, ScriptableObject.READONLY)
    }
}