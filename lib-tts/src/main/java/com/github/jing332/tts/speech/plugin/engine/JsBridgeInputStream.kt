package com.github.jing332.tts.speech.plugin.engine

import android.util.Log
import androidx.annotation.Keep
import com.github.jing332.script.runtime.exception.ScriptException
import kotlinx.coroutines.sync.Mutex
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.locks.ReentrantLock

class JsBridgeInputStream : InputStream() {
    companion object {
        private const val TAG = "JsBridgeInputStream"
    }

    private val pis: PipedInputStream = PipedInputStream()
    private val pos: PipedOutputStream = PipedOutputStream(pis)
    private var isClosed = false
    private var errorCause: Exception? = null
    private val hasError: Boolean
        get() = errorCause != null

    private fun checkError() {
        errorCause?.let {
            throw it
        }
    }

    override fun read(): Int {
        checkError()
        if (isClosed && pis.available() == 0) {
            return -1 // Signal end of stream
        }

        try {
            val byte = pis.read() // Reads a single byte
            checkError()
            return byte
        } catch (e: IOException) {
            errorCause = e
            throw e
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        checkError()
        if (isClosed && pis.available() == 0) {  // Check for EOF *before* blocking read.  Crucial!
            return -1
        }
        if (off < 0 || len < 0 || len > b.size - off) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return 0
        }

        try {
            val byte = pis.read(b, off, len) // Reads up to 'len' bytes into the buffer
            checkError()
            return byte
        } catch (e: IOException) {
            errorCause = e
            throw e
        }
    }

    override fun available(): Int {
        checkError()
        return pis.available().apply {
            checkError()
        }
    }

    @Synchronized
    override fun close() {
        if (!isClosed) {
            isClosed = true
            try {
                pos.close() // Close output end first!  Very important.
            } finally {
                pis.close() // Then close the input end.
            }
        }
    }

    /**
     *  Interface for JavaScript to interact with the OutputStream.  The names
     *  and signatures MUST match your Kotlin definitions.
     */
    @Keep
    interface Callback {
        fun write(data: ByteArray)
        fun close()
        fun error(data: Any?)
    }

    suspend fun getCallback(mutex: Mutex): Callback {
        mutex.lock()
        return object : Callback {
            override fun write(data: ByteArray) {
                Log.v(TAG, "${this}.write(${data.size})")
                if (isClosed || hasError) return

                try {
                    pos.write(data)
                    pos.flush()
                } catch (e: IOException) {
                    errorCause = e
                    try {
                        close()
                    } catch (ignored: IOException) {
                    }
                }
            }

            override fun close() {
                Log.d(TAG, "${this}.close")
                try {
                    this@JsBridgeInputStream.close()
                } catch (e: IOException) {
                    errorCause = e
                } finally {
                    if (mutex.isLocked) mutex.unlock()
                }
            }

            override fun error(data: Any?) {
                Log.d(TAG, "${this}.error(${data})")
                errorCause = if (data is Exception)
                    data
                else
                    ScriptException("JavaScript: " + (data ?: "null"))

                try {
                    close()
                } catch (ignored: IOException) {
                }
            }
        }
    }
}
