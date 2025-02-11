package com.github.jing332.script.runtime.console

import com.github.jing332.common.toLogLevelChar
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging


class GlobalConsole : ConsoleImpl() {
    companion object {
        val logger: KLogger = KotlinLogging.logger("JS-Console")
        fun get(): GlobalConsole = GlobalConsole().apply {
            addLogListener {

            }
        }
    }

    override fun write(level: Int, data: CharSequence?) {
        logger.debug { level.toLogLevelChar() + " " + data }
        super.write(level, data)
    }

    override fun println(level: Int, charSequence: CharSequence): String? {
        logger.debug { level.toLogLevelChar() + " " + charSequence }
        return super.println(level, charSequence)
    }
}