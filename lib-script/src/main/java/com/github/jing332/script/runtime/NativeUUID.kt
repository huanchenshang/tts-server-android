@file:OptIn(ExperimentalUuidApi::class)

package com.github.jing332.script.runtime

import com.github.jing332.script.ensureArgumentsLength
import com.github.jing332.script.toNativeArrayBuffer
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.typedarrays.NativeTypedArrayView
import org.mozilla.javascript.typedarrays.NativeUint8Array
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class NativeUUID : ScriptableObject() {
    companion object {
        @JvmStatic
        fun init(cx: Context, scope: Scriptable, sealed: Boolean) {
            val uuid = NativeUUID()
            uuid.prototype = getObjectPrototype(scope)
            uuid.parentScope = scope
            uuid.defineProperty(scope, "v4", 3, NativeUUID::v4, DONTENUM, DONTENUM or READONLY)
            uuid.defineProperty(
                scope,
                "stringify",
                3,
                NativeUUID::stringify,
                DONTENUM,
                DONTENUM or READONLY
            )
            uuid.defineProperty(
                scope,
                "parse",
                1,
                NativeUUID::parse,
                DONTENUM,
                DONTENUM or READONLY
            )
            uuid.defineProperty(
                scope,
                "validate",
                1,
                NativeUUID::validate,
                DONTENUM,
                DONTENUM or READONLY
            )

            if (sealed) uuid.sealObject()

            ScriptableObject.defineProperty(scope, "UUID", uuid, DONTENUM);
        }

        private fun v4(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any {
            return Uuid.random().toString()
        }

        @Suppress("UNCHECKED_CAST")
        private fun stringify(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any {
            val arg0 = args[0] as? NativeTypedArrayView<Number>
            for (byte in arg0!!.iterator()) {
                byte.toByte()
            }

            val bytes = arg0.map { it.toByte() }.toByteArray()

            return Uuid.fromByteArray(bytes).toString()
        }

        private fun parse(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any = ensureArgumentsLength(args, 1) {
            val str = it[0]!!.toString()

            val buffer = Uuid.parse(str).toByteArray().toNativeArrayBuffer()
            cx.newObject(scope, "Uint8Array", arrayOf(buffer, 0, buffer.length))
        }

        private fun validate(
            cx: Context,
            scope: Scriptable,
            thisObj: Scriptable,
            args: Array<Any>,
        ): Any = ensureArgumentsLength(args, 1) {
            try {
                val str = Context.toString(it[0])
                Uuid.parse(str)
                true
            } catch (_: IllegalArgumentException) {
                false
            }
        }

    }

    override fun getClassName(): String = "UUID"

}