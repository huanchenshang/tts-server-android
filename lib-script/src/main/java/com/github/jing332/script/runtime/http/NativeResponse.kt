package com.github.jing332.script.runtime.http

import cn.hutool.core.text.CharSequenceUtil.bytes
import com.github.jing332.script.PropertyGetter
import com.github.jing332.script.definePrototypeMethod
import com.github.jing332.script.definePrototypeProperty
import com.github.jing332.script.definePrototypePropertys
import com.github.jing332.script.toNativeArrayBuffer
import okhttp3.Response
import org.mozilla.javascript.Context
import org.mozilla.javascript.LambdaConstructor
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.Undefined
import org.mozilla.javascript.json.JsonParser
import org.mozilla.javascript.typedarrays.NativeUint8Array

class NativeResponse @JvmOverloads constructor(val rawResponse: Response? = null) :
    ScriptableObject() {
    override fun getClassName(): String = CLASS_NAME



    companion object {
        const val CLASS_NAME = "Response"

        @JvmStatic
        val serialVersionUID: Long = 3110411773054879588L

        @JvmStatic
        fun init(cx: Context, scope: Scriptable, sealed: Boolean) {
            val constructor =
                LambdaConstructor(
                    scope,
                    CLASS_NAME,
                    1,
                    LambdaConstructor.CONSTRUCTOR_NEW
                ) { cx: Context, scope: Scriptable, args: Array<Any?> ->
                    NativeResponse(args.getOrNull(0) as? Response)
                }
            constructor.setPrototypePropertyAttributes(DONTENUM or READONLY or PERMANENT)

            constructor.definePrototypePropertys<NativeResponse>(
                cx, scope,
                listOf(
                    "status" to PropertyGetter { it.rawResponse?.code ?: 0 },
                    "statusText" to PropertyGetter { it.rawResponse?.message ?: "" },
                    "headers" to PropertyGetter {
                        it.rawResponse?.headers?.toMap() ?: Undefined.instance
                    },
                    "url" to PropertyGetter { it.rawResponse?.request?.url?.toString() ?: "" },
                    "redirected" to PropertyGetter { it.rawResponse?.isRedirect == true },
                    "ok" to PropertyGetter { it.rawResponse?.isSuccessful == true },
                )
            )

            constructor.definePrototypeMethod<NativeResponse>(
                scope, "json", 0, { cx, scope, thisObj, args -> thisObj.js_json() }
            )
            constructor.definePrototypeMethod<NativeResponse>(
                scope, "text", 0, { cx, scope, thisObj, args -> thisObj.js_text() }
            )
            constructor.definePrototypeMethod<NativeResponse>(
                scope, "bytes", 0, { cx, scope, thisObj, args -> thisObj.js_bytes(cx, scope) }
            )

            ScriptableObject.defineProperty(scope, CLASS_NAME, constructor, DONTENUM)
            if (sealed) constructor.sealObject()
        }


        private fun js_constructor(
            cx: Context,
            scope: Scriptable,
            args: Array<out Any?>,
        ): NativeResponse {
            val resp = args.getOrNull(0) as? Response
            val obj = NativeResponse(resp)
            return obj
        }

        private fun NativeResponse.checkResponse(): Response {
            rawResponse ?: throw IllegalStateException("rawResponse is null")
            if (rawResponse.isSuccessful == true)
                return rawResponse
            else
                throw Exception("Response failed: code=${rawResponse?.code}, message=${rawResponse?.message}")
        }

        private fun NativeResponse.js_json(): Any {
            val resp = checkResponse()
            val str = resp.body?.string() ?: return ""
            return JsonParser(Context.getCurrentContext(), this).parseValue(str)
        }

        private fun NativeResponse.js_text(): Any {
            val resp = checkResponse()
            return resp.body?.string() ?: ""
        }

        private fun NativeResponse.js_bytes(cx: Context, scope: Scriptable): Any {
            val bytes = checkResponse().body?.bytes() ?: ByteArray(0)
            val buffer = bytes.toNativeArrayBuffer()

            return cx.newObject(scope, "Uint8Array", arrayOf(buffer, 0, buffer.size()))
        }
    }
}