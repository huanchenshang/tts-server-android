package com.github.jing332.server.forwarder

import android.util.Log
import com.github.jing332.server.BaseCallback
import com.github.jing332.server.Server
import com.github.jing332.server.installPlugins
import io.ktor.http.ContentType
import io.ktor.http.decodeURLQueryComponent
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.origin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import java.io.OutputStream

class SystemTtsForwardServer(val port: Int, val callback: Callback) : Server {
    private val ktor by lazy {
        embeddedServer(Netty, port = port) {
            installPlugins()
            intercept(ApplicationCallPipeline.Call) {
                val method = call.request.httpMethod.value
                val uri = call.request.uri
                val remoteAddress = call.request.origin.remoteAddress

                callback.log(
                    level = Log.INFO,
                    "$method: ${uri.decodeURLQueryComponent()} \n remote: $remoteAddress \n"
                )
            }

            routing {
                staticResources("/", "forwarder")

                suspend fun RoutingContext.handleTts(
                    params: TtsParams,
                ) {
                    call.respondOutputStream(ContentType.parse("audio/wav")) {
                        callback.tts(output = this, params)
                    }
                }

                get("api/tts") {
                    val text = call.parameters.getOrFail("text")
                    val engine = call.parameters.getOrFail("engine")
                    val voice = call.parameters["voice"] ?: ""
                    val speed = (call.parameters["rate"] ?: call.parameters["speed"])
                        ?.toIntOrNull() ?: 50
                    val pitch = call.parameters["pitch"]?.toIntOrNull() ?: 100

                    handleTts(TtsParams(text, engine, voice, speed, pitch))
                }

                post("api/tts") {
                    val params = call.receive<TtsParams>()
                    handleTts(params)
                }

                get("api/engines") {
                    call.respond(callback.engines())
                }

                get("api/voices") {
                    val engine = call.parameters.getOrFail("engine")
                    call.respond(callback.voices(engine))
                }

                get("api/legado") {
                    val api = call.parameters.getOrFail("api")
                    val name = call.parameters.getOrFail("name")
                    val engine = call.parameters.getOrFail("engine")
                    val voice = call.parameters["voice"] ?: ""
                    val pitch = call.parameters["pitch"] ?: "50"

                    call.respond(
                        LegadoUtils.getLegadoJson(api, name, engine, voice, pitch)
                    )
                }

            }
        }
    }

    override fun start(wait: Boolean) {
        ktor.start(wait)
    }

    override fun stop() {
        ktor.stop(100)
    }

    interface Callback : BaseCallback {
        suspend fun tts(
            output: OutputStream,
            params: TtsParams,
        )

        suspend fun voices(engine: String): List<Voice>
        suspend fun engines(): List<Engine>
    }
}