package com.github.jing332.script.runtime.console

import com.github.jing332.common.LogLevel
import com.github.jing332.script.jsToString
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeConsole
import org.mozilla.javascript.ScriptStackElement
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined

internal class ConsoleUtils {
    companion object {
        fun toLogLevel(l: NativeConsole.Level) = when (l) {
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

        fun Console.putLogger(obj: ScriptableObject, name: String, level: NativeConsole.Level) {
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

        fun Console.write(
            args: Array<out Any?>,
            level: NativeConsole.Level,
            stack: Array<out ScriptStackElement?>? = null,
        ) {
            val str = log(*args)
            write(
                toLogLevel(level),
                if (level == NativeConsole.Level.TRACE) str + "\n" + stack.contentToString() else str
            )
        }
    }
}