package com.github.jing332.script.runtime

import com.github.jing332.script.rhino.ModuleSourceProvider
import com.github.jing332.script.rhino.NativeWebSocket
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.LazilyLoadedCtor
import org.mozilla.javascript.NativeJavaPackage
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.commonjs.module.RequireBuilder
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider

class RhinoGlobal(cx: Context) : ImporterTopLevel(cx) {
    init {
        initRequireBuilder(cx, this)

        LazilyLoadedCtor(this, "Websocket", NativeWebSocket::class.java.name, false, true)
        defineProperty("println", object : BaseFunction() {
            override fun call(
                cx: Context?,
                scope: Scriptable?,
                thisObj: Scriptable?,
                args: Array<out Any>?
            ): Any {
                println(args?.joinToString(" "))

                return Undefined.instance
            }
        }, ScriptableObject.READONLY)
    }

    private fun initRequireBuilder(context: Context, scope: Scriptable) {
        val provider = ModuleSourceProvider()
        RequireBuilder()
            .setModuleScriptProvider(SoftCachingModuleScriptProvider(provider))
            .setSandboxed(true)
            .createRequire(context, scope)
            .install(scope)
    }
}