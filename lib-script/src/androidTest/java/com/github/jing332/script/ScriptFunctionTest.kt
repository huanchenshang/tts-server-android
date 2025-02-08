package com.github.jing332.script

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jing332.script.rhino.RhinoScriptEngine
import com.github.jing332.script.runtime.RhinoScriptRuntime
import com.github.jing332.script.source.toScriptSource
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.javascript.ScriptableObject

@RunWith(AndroidJUnit4::class)
class ScriptFunctionTest {
    @Test
    fun invoke() {
        val code = """
            function test(){
                println("test()")                
            }
            
            let OBJ = {
                'test': function(){
                    println("OBJ.test()")
                }
            }
        """.trimIndent().toScriptSource()
        val engine = RhinoScriptEngine(RhinoScriptRuntime())
        engine.execute(code)
        engine.invokeMethod(engine.get("OBJ") as ScriptableObject, "test")
        engine.invokeFunction("test")
    }
}