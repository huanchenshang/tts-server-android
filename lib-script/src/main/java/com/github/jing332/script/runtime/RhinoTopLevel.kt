package com.github.jing332.script.runtime

import com.github.jing332.script.engine.ModuleSourceProvider
import org.mozilla.javascript.Context
import org.mozilla.javascript.ImporterTopLevel
import org.mozilla.javascript.LazilyLoadedCtor
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.commonjs.module.RequireBuilder
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider
import org.mozilla.javascript.typedarrays.NativeBuffer
import splitties.init.appCtx

/**
 * Custom global scope for Rhino JavaScript engine.
 */
class RhinoTopLevel(cx: Context) : ImporterTopLevel(cx) {
    init {
        initRequireBuilder(cx, this)

        NativeBuffer.init2(cx, this, false)
        // Property
        LazilyLoadedCtor(this, GlobalWebview.NAME, GlobalWebview::class.java.name, false, true)
        LazilyLoadedCtor(
            this,
            GlobalFileSystem.NAME,
            GlobalFileSystem::class.java.name,
            false,
            true
        )
        LazilyLoadedCtor(this, GlobalHttp.NAME, GlobalHttp::class.java.name, false, true)
        LazilyLoadedCtor(this, GlobalUUID.NAME, GlobalUUID::class.java.name, false, true)

        // Class
        NativeResponse.init(cx, this, false)
//        LazilyLoadedCtor(this, "Response", NativeResponse::class.java.name, false, true)

        LazilyLoadedCtor(this, "Websocket", NativeWebSocket::class.java.name, false, true)
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