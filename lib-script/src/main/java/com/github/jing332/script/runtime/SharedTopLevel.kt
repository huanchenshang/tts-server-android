package com.github.jing332.script.runtime

import com.github.jing332.script.rhino.ModuleSourceProvider
import com.github.jing332.script.runtime.http.NativeHttp
import com.github.jing332.script.runtime.http.NativeResponse
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
class SharedTopLevel(cx: Context) : ImporterTopLevel(cx) {
    init {
        initRequireBuilder(cx, this)

        NativeBuffer.init2(cx, this, false)
         // Property
        LazilyLoadedCtor(this, "fs", NativeFileSystem::class.java.name, false, true)
        LazilyLoadedCtor(this, "http", NativeHttp::class.java.name, false, true)
        LazilyLoadedCtor(this, "UUID", NativeUUID::class.java.name, false, true)

        // Class
//        LazilyLoadedCtor(this, "Buffer", NativeBuffer::class.java.name, false, true)
        LazilyLoadedCtor(this, "Response", NativeResponse::class.java.name, false, true)
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