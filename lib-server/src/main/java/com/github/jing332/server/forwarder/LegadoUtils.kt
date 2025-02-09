package com.github.jing332.server.forwarder

import com.github.jing332.server.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Serializable
data class LegadoJson(
    @SerialName("contentType") val contentType: String = "audio/x-wav",
    @SerialName("header") val header: String? = null,
    @SerialName("id") val id: Long = System.currentTimeMillis(),
    @SerialName("lastUpdateTime") val lastUpdateTime: Long = System.currentTimeMillis(),
    @SerialName("name") val name: String,
    @SerialName("url") val url: String,
    @SerialName("concurrentRate") val concurrentRate: String = "100"
)

object LegadoUtils {
    fun getLegadoJson(
        api: String,
        displayName: String,
        engine: String,
        voice: String,
        pitch: String
    ): LegadoJson {
        val url = "$api?engine=$engine&text={{java.encodeURI(speakText)}}&rate={{speakSpeed * 2}}" +
                "&pitch=$pitch&voice=$voice"

        val data = LegadoJson(
            name = displayName,
            url = url
        )

        return data
    }
}