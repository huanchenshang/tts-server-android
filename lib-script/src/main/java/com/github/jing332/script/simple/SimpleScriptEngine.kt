package com.github.jing332.script.simple

import android.content.Context
import com.github.jing332.script.rhino.RhinoScriptEngine
import com.github.jing332.script.simple.ext.JsExtensions

class SimpleScriptEngine(context: Context, id: String) :
    RhinoScriptEngine(CompatScriptRuntime(JsExtensions(context, id))) {
}