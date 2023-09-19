package common

import api_models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.serenitybdd.screenplay.Ability
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.HasTeardown
import java.lang.IllegalArgumentException

open class ListenToEvents(
    private val host: String,
    private val port: Int,
): Ability, HasTeardown {

    private val server: ApplicationEngine

    var connectionEvents: MutableList<ConnectionEvent> = mutableListOf()
    var credentialEvents: MutableList<CredentialEvent> = mutableListOf()
    var presentationEvents: MutableList<PresentationEvent> = mutableListOf()
    var didEvents: MutableList<DidEvent> = mutableListOf()

    fun route(application: Application) {
        application.routing {
            post("/") {
                val eventString = call.receiveText()
                val event = Json.decodeFromString<Event>(eventString)
                when (event.type) {
                    TestConstants.EVENT_TYPE_CONNECTION_UPDATED -> connectionEvents.add(Json.decodeFromString<ConnectionEvent>(eventString))
                    TestConstants.EVENT_TYPE_ISSUE_CREDENTIAL_RECORD_UPDATED -> credentialEvents.add(Json.decodeFromString<CredentialEvent>(eventString))
                    TestConstants.EVENT_TYPE_PRESENTATION_UPDATED -> presentationEvents.add(Json.decodeFromString<PresentationEvent>(eventString))
                    TestConstants.EVENT_TYPE_DID_STATUS_UPDATED -> {
                        didEvents.add(Json.decodeFromString<DidEvent>(eventString))
                    }
                    else -> {
                        throw IllegalArgumentException("ERROR: unknown event type ${event.type}")
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    companion object {
        fun at(host: String, port: Int): ListenToEvents {
            return ListenToEvents(host, port)
        }

        fun `as`(actor: Actor): ListenToEvents {
            return actor.abilityTo(ListenToEvents::class.java)
        }
    }

    init {
        server = embeddedServer(
            Netty,
            port = port,
            host = if (host == "host.docker.internal") "0.0.0.0" else host,
            module = {route(this)})
            .start(wait = false)
    }

    override fun toString(): String {
        return "Listen HTTP port at ${host}:${port}"
    }

    override fun tearDown() {
        server.stop()
    }
}
