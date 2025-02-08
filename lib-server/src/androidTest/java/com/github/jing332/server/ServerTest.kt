package com.github.jing332.server

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jing332.common.LogEntry
import com.github.jing332.server.script.ScriptRemoteServer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ServerTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.github.jing332.server.test", appContext.packageName)
    }

    @Test
    fun scriptRemote() {
        val server = ScriptRemoteServer(4566, object : ScriptRemoteServer.Callback {
            override fun pull(): String = """function test(){ return "hello, I am TTS Server." }"""

            override fun push(code: String) {
                println("push (update code): $code")
            }

            override fun action(name: String) {
                println("action: $name")
            }

            override fun log(): List<LogEntry> =
                listOf(
                    LogEntry("debug...", Log.DEBUG),
                    LogEntry("verbose...", Log.VERBOSE),
                    LogEntry("info...", Log.INFO),
                    LogEntry("error...", Log.ERROR),
                    LogEntry("warn...", Log.WARN),
                )

        })
        server.start(wait = true)
    }
}