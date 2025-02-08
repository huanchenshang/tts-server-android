package com.github.jing332.script_engine

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sun.script.javascript.RhinoScriptEngine
import com.sun.script.javascript.RhinoScriptEngineFactory

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import javax.script.Bindings
import javax.script.ScriptContext

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.github.jing332.script_engine.test", appContext.packageName)
    }

    @Test
    fun jsr223(){
        val engine = RhinoScriptEngineFactory().scriptEngine
        val b = engine.createBindings()
        b.put("a", "qwq")
        engine.setBindings(b, ScriptContext.ENGINE_SCOPE)
        engine.eval("println(a)")

    }
}