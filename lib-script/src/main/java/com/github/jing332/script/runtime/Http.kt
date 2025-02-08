package com.github.jing332.script.runtime

import com.drake.net.Net
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.Response

class Http() {
    private val logger: KLogger = KotlinLogging.logger { this::class.java.simpleName }

    @JvmOverloads
    fun get(url: CharSequence, headers: Map<CharSequence, CharSequence>? = null): Response {
        val req = Net.get(path = url.toString()) {
            headers?.forEach {
                setHeader(it.key.toString(), it.value.toString())
            }
        }

        return req.execute()
    }

    /*  fun getBytes(url: CharSequence, headers: Map<CharSequence, CharSequence>? = null): ByteArray
    fun getString(url: CharSequence, headers: Map<CharSequence, CharSequence>? = null): String

    fun post(
        url: CharSequence,
        body: CharSequence? = null,
        headers: Map<CharSequence, CharSequence>? = null
    )

    fun postString(
        url: CharSequence,
        body: CharSequence? = null,
        headers: Map<CharSequence, CharSequence>? = null
    ): String

    fun postBytes(
        url: CharSequence,
        body: CharSequence? = null,
        headers: Map<CharSequence, CharSequence>? = null
    ): ByteArray

    fun postMultipart(
        url: CharSequence,
        form: Map<CharSequence, Any>,
        type: CharSequence = "multipart/form-data",
        headers: Map<CharSequence, CharSequence>? = null
    )*/
}