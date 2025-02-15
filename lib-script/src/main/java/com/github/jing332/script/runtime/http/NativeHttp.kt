package com.github.jing332.script.runtime.http

import com.drake.net.Net
import com.github.jing332.script.ensureArgumentsLength
import com.github.jing332.script.exception.NetworkException
import okhttp3.Response
import org.mozilla.javascript.Context
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

class NativeHttp : ScriptableObject() {
    companion object {
        @JvmStatic
        fun init(cx: Context, scope: Scriptable, sealed: Boolean) {
            val obj = NativeHttp()
            obj.prototype = getObjectPrototype(scope)
            obj.parentScope = scope

            obj.defineProperty(scope, "get", 2, ::get, DONTENUM, DONTENUM or READONLY)

            ScriptableObject.defineProperty(scope, "http", obj, DONTENUM or READONLY)
            if (sealed) obj.sealObject()
        }

        @JvmStatic
        private fun get(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any = ensureArgumentsLength(args, 1..2) {
            val url = args[0] as CharSequence
            val headers = args.getOrNull(1) as? Map<CharSequence, CharSequence>

            try {
                val resp = Net.get(url.toString()) {
                    headers?.forEach {
                        setHeader(it.key.toString(), it.value.toString())
                    }
                }.execute<Response>()
                cx.newObject(scope, "Response", arrayOf(resp))
            } catch (e: Exception) {
                val err = Context.reportRuntimeError(e.message)
                throw NetworkException(message = err.message, cause = e).apply {
                    sourceName = err.sourceName()
                    lineNumber = err.lineNumber()
                    columnNumber = err.columnNumber()
                }
            }
        }
    }

    override fun getClassName(): String = "Http"
}