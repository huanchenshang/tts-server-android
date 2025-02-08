package com.github.jing332.script.rhino

import com.github.jing332.script.withRhinoContext
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined

class NativeEventTarget {
    val functions = hashMapOf<String, Function>()
    private var scope: ScriptableObject? = null

    private fun addEventListener(eventName: String, function: Function) {
        functions[eventName] = function
    }

    fun init(dest: ScriptableObject) {
        scope = dest
        dest.defineProperty("on", object : BaseFunction() {
            override fun call(
                cx: Context?,
                scope: Scriptable?,
                thisObj: Scriptable?,
                args: Array<out Any>?
            ): Any {
                val eventName = args!![0].toString()
                val function = args[1] as Function
                addEventListener(eventName, function)

                return Undefined.instance
            }
        }, ScriptableObject.READONLY)

        dest.defineProperty("addEventListener", object : BaseFunction() {
            override fun call(
                cx: Context?,
                scope: Scriptable?,
                thisObj: Scriptable?,
                args: Array<out Any>?
            ): Any {
                val eventName = args!![0].toString()
                val function = args[1] as Function
                addEventListener(eventName, function)
                return Undefined.instance
            }
        }, ScriptableObject.READONLY)
    }

    fun emit(eventName: String, vararg args: Any) {
        val function = functions[eventName]
        if (function != null)
            withRhinoContext {
                function.call(it, scope, scope, args)
            }
    }


}