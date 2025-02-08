package com.github.jing332.script

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jing332.script.rhino.RhinoScriptEngine
import com.github.jing332.script.runtime.RhinoScriptRuntime
import com.github.jing332.script.simple.SimpleScriptEngine
import com.github.jing332.script.source.ReaderScriptSource
import com.github.jing332.script.source.StringScriptSource
import com.github.jing332.script.source.toScriptSource
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptRuntime
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import splitties.init.appCtx
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class ScriptTest {
    private fun eval(code: String): Any? {
        val engine = RhinoScriptEngine()
        return try {
            engine.execute(StringScriptSource(code))
        } catch (e: EvaluatorException) {
            throw Exception("${e.sourceName()}, ${e.lineNumber()}, ${e.columnNumber()}").apply {
                initCause(e)
            }
        }
    }

    @Test
    fun websocket() {
        val code = """
           var ws = new Websocket("wss://echo.websocket.org")
           println(ws.readyState)
           let latch = new java.util.concurrent.CountDownLatch(1);
           
           ws.on("open", function() {
               println("open")
               ws.send("hello, I am a client!")            
           })
           
           ws.on("text", function(data) {
               println("Text: " + data)
               ws.close(1000, "")
           })
           
           ws.on("closed", function() {
               println("closed")
               latch.countDown()
           })
           
           ws.on("failure", function(reason) {
               println("failure: " + reason)
               latch.countDown()
           })
           
           latch.await()
        """.trimIndent()
        eval(code)
    }

    @Test
    fun importer() {
        val code = """
            let String = Packages.java.lang.String
            
            println(String.valueOf(1))
            
            importPackage(java.util)
            let encoder = Base64.getEncoder()
        """.trimIndent()
        eval(code)
    }

    @Test
    fun interrupt() {
        val code = """
            function onLoad(){
                println("onLoad")
                try {
                    sleep(5000)     
                } catch (e) {
                    println("interrupted")
                }
                
                println("finished")               
            }
        """.trimIndent()

        val e = RhinoScriptEngine()
        try {
            e.execute(StringScriptSource(code))
        } catch (e: EvaluatorException) {
            throw RuntimeException("${e.message} ${e.lineNumber()}:${e.columnNumber()}").apply {
                initCause(e)
            }
        }

        e.globalScope.defineProperty("sleep", object : BaseFunction() {
            override fun call(
                cx: Context?,
                scope: Scriptable?,
                thisObj: Scriptable?,
                args: Array<out Any>?
            ): Any = ensureArgumentsLength(args, 1) {
                val time = ScriptRuntime.toInt32(it[0]).toLong()
                Thread.sleep(time)
                super.call(cx, scope, thisObj, args)
            }
        }, ScriptableObject.READONLY)

        val onLoadFunc = e.globalScope.get("onLoad") as Function
        val currentThread = Thread.currentThread()
        Thread {
            Thread.sleep(1000)
            currentThread.interrupt()
        }.start()
//        val ret = onLoadFunc.call(e.rhino, e.topLevelScope, e.topLevelScope, arrayOf())
    }

    @Test
    fun httpDelay() {
        val engine = RhinoScriptEngine()

        val code = """
            console.log("log...")
            console.debug("debug...")
            console.info("info...")   
            console.warn("warn...")
            console.error("error...")
            
            let resp = http.get("https://reqres.in/api/users?delay=8") //delay 8s
            let str = resp.body().string()
            console.log("ret: " + str)
        """.trimIndent()

        val currentThread = Thread.currentThread()
        Thread {
            Thread.sleep(1000)
            currentThread.interrupt()
        }.start()
        engine.execute(StringScriptSource(code))
    }

    @Test
    fun scope() {
        val e = RhinoScriptEngine()

        // 测试两次执行环境是否独立
        // 即每次调用初始化新的scope
        // topLevelScope(global) 作为 scope.prototype 以复用
        e.execute(StringScriptSource("""let a=111"""))
        e.execute(StringScriptSource("""console.log(a)"""))
    }

    @Test
    fun simpleEngine() {
        val androidContext = InstrumentationRegistry.getInstrumentation().targetContext
        val e = SimpleScriptEngine(androidContext, "simple-engine")
        e.execute(
            """
            ttsrv.writeTxtFile("hello world", "test.txt")
        """.trimIndent().toScriptSource()
        )
    }

    @Test
    fun putAndGet() {
        val e = RhinoScriptEngine()
        e.globalScope.put("a", e.globalScope, "111")
        e.execute(
            """
            let OBJ = {
                name: "1234",            
            }
            console.log(a)
        """.trimIndent().toScriptSource()
        )
        println((e.get("OBJ") as ScriptableObject).get("name"))
    }

    @Test
    fun testRequire() {
        val code = """
//            require("https://cdn.bootcdn.net/ajax/libs/crypto-js/4.2.0/crypto-js.min.js")
            require("crypto")
        """.trimIndent().toScriptSource()
        val e = RhinoScriptEngine()
//        val reader = appCtx.assets.open("js/crypto.js").reader()
//        e.execute(ReaderScriptSource(reader))
        measureTimeMillis {
            e.execute(code)
        }.run {
            println("time: $this")
        }
    }
}