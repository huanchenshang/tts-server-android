package com.github.jing332.script.rhino

import com.github.jing332.script.JavaScriptEngine
import com.github.jing332.script.runtime.RhinoGlobal
import com.github.jing332.script.runtime.RhinoScriptRuntime
import com.github.jing332.script.source.ReaderScriptSource
import com.github.jing332.script.source.ScriptSource
import com.github.jing332.script.source.StringScriptSource
import com.github.jing332.script.withRhinoContext
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject

open class RhinoScriptEngine(val runtime: RhinoScriptRuntime = RhinoScriptRuntime()) :
    JavaScriptEngine() {
    val globalScope: RhinoGlobal
        get() = runtime.global

    var scope: ScriptableObject? = null
        private set

    init {
        runtime.init()
    }


    @Deprecated("auto init")
    override fun init() {
    }

    override fun destroy() {
    }


    override fun execute(source: ScriptSource): Any? = withRhinoContext { cx ->
        val sourceName = source.sourceName.ifEmpty { "<Unknown>" }
        scope = cx.initStandardObjects().also {
            it.prototype = globalScope
        }

        when (source) {
            is ReaderScriptSource -> {
                cx.evaluateReader(scope, source.reader, sourceName, 1, null)
            }

            is StringScriptSource -> {
                cx.evaluateString(scope, source.script, sourceName, 1, null)
            }

            else -> IllegalArgumentException("Unsupported source type: ${source::class.java.name}")
        }
    }

    override fun put(key: String, value: Any?) {
        globalScope.defineProperty(key, value, ScriptableObject.READONLY)
    }

    override fun get(key: String): Any? = scope?.get(key) ?: globalScope.get(key)


//    fun wrapArguments(args: Array<out Any?>?): Array<Any?> {
//        return if (args == null) {
//            Context.emptyArgs
//        } else {
//            arrayOfNulls<Any>(args.size).also {
//                for (i in args.indices) {
//                    it[i] = Context.javaToJS(args[i], globalScope)
//                }
//            }
//        }
//    }


    fun invokeMethod(obj: ScriptableObject, name: String, vararg args: Any?): Any? {
        return invoke(obj, name, *args)
    }

    fun invokeFunction(name: String, vararg args: Any?): Any? {
        return invoke(
            scope
                ?: throw IllegalStateException("[scope] is not initialized. Has execute() been called?"),
            name, *args
        )
    }

    private fun invoke(thiz: ScriptableObject, name: String, vararg args: Any?): Any? {
        val parent = thiz.parentScope ?: globalScope
        return withRhinoContext { cx ->
            val method =
                ScriptableObject.getProperty(thiz, name) as? Function
                    ?: throw NoSuchMethodException(name)
            method.call(cx, parent, thiz, arrayOf(*args))
        }
    }

}