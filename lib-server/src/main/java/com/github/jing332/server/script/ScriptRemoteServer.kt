package com.github.jing332.server.script

import com.github.jing332.common.LogEntry
import com.github.jing332.server.Server
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.util.getOrFail
import kotlinx.serialization.json.Json
import java.net.BindException

class ScriptRemoteServer(
    val port: Int,
    val callback: Callback,
) : Server {
    companion object {
        private val jsContentType = ContentType.parse("text/javascript")
        private const val BASE_PATH = "api/sync"
    }

    private val ktor by lazy {
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation){
                json(Json{
                    prettyPrint = true
                    isLenient = true
                })
            }

            routing {
                get("$BASE_PATH/pull") {
                    call.respondText(contentType = jsContentType, text = callback.pull())
                }

                post("$BASE_PATH/push") {
                    val code = call.receiveText()
                    callback.push(code)
                    call.respondText("")
                }

                post("$BASE_PATH/action") {
                    val name = call.parameters["name"] ?: call.parameters.getOrFail("action")
                    callback.action(name)
                    call.respondText("")
                }

                get("$BASE_PATH/log") {
                    call.respond(callback.log())
                }
            }
        }
    }


    @Throws(BindException::class)
    override fun start(wait: Boolean) {
        ktor.environment.log.info("Start script sync server on port $port")
        ktor.start(wait)
    }

    override fun stop() {
        ktor.stop()
    }

    interface Callback {
        fun pull(): String
        fun push(code: String)
        fun action(name: String)
        fun log(): List<LogEntry>
    }

}