package com.github.jing332.script.rhino

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.WrapFactory

object RhinoWrapFactory : WrapFactory() {
    override fun wrap(cx: Context?, scope: Scriptable?, obj: Any?, staticType: Class<*>?): Any? {
        return super.wrap(cx, scope, obj, staticType)
    }

    override fun wrapAsJavaObject(
        cx: Context?,
        scope: Scriptable?,
        javaObject: Any?,
        staticType: Class<*>?
    ): Scriptable {
        return super.wrapAsJavaObject(cx, scope, javaObject, staticType)
    }
}