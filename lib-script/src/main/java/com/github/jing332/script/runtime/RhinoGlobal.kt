package com.github.jing332.script.runtime

import com.github.jing332.script.jsToString
import com.github.jing332.script.rhino.ModuleSourceProvider
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.LazilyLoadedCtor
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.commonjs.module.RequireBuilder
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider
import org.mozilla.javascript.typedarrays.NativeUint8Array
import splitties.init.appCtx

class RhinoGlobal(cx: Context) : ImporterTopLevel(cx) {
    init {
        initRequireBuilder(cx, this)

        LazilyLoadedCtor(this, "UUID", NativeUUID::class.java.name, false, true)
        LazilyLoadedCtor(this, "Websocket", NativeWebSocket::class.java.name, false, true)
        defineProperty("println", object : BaseFunction() {
            override fun call(
                cx: Context?,
                scope: Scriptable?,
                thisObj: Scriptable?,
                args: Array<out Any>?
            ): Any {
                println(jsToString(args?: "") )


                return Undefined.instance
            }
        }, ScriptableObject.READONLY)
    }

    private fun initRequireBuilder(context: Context, scope: Scriptable) {
        val provider = ModuleSourceProvider(appCtx, "js")
        RequireBuilder()
            .setModuleScriptProvider(SoftCachingModuleScriptProvider(provider))
            .setSandboxed(false)
            .createRequire(context, scope)
            .install(scope)
    }
}