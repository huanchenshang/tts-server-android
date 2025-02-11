package com.github.jing332.common

import android.graphics.Color
import androidx.annotation.IntDef

@IntDef(
    LogLevel.PANIC,
    LogLevel.FATIL,
    LogLevel.ERROR,
    LogLevel.WARN,
    LogLevel.INFO,
    LogLevel.DEBUG,
    LogLevel.TRACE
)
annotation class LogLevel {
    companion object {
        const val PANIC = 0
        const val FATIL = 1
        const val ERROR = 2
        const val WARN = 3
        const val INFO = 4
        const val DEBUG = 5
        const val TRACE = 6

        fun toColor(level: Int): Int {
            return when {
                level == WARN -> {
                    Color.rgb(255, 215, 0) /* 金色 */
                }

                level <= ERROR -> {
                    Color.RED
                }

                else -> {
                    Color.GRAY
                }
            }
        }

        fun toString(level: Int): String {
            when (level) {
                PANIC -> return "P"
                FATIL -> return "F"
                ERROR -> return "E"
                WARN -> return "W"
                INFO -> return "I"
                DEBUG -> return "D"
                TRACE -> return "T"
            }
            return level.toString()
        }
    }
}