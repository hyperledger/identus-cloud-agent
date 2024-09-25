package abilities

import com.google.gson.GsonBuilder
import common.TestConstants
import io.iohk.atala.automation.restassured.CustomGsonObjectMapperFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import models.*
import net.serenitybdd.screenplay.Ability
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.HasTeardown
import net.serenitybdd.screenplay.Question
import org.hyperledger.identus.client.models.*
import java.net.URL
import java.time.OffsetDateTime

open class ListenToEvents(
    private val url: URL,
    webhookPort: Int?,
) : Ability, HasTeardown {

    private val server: ApplicationEngine
    private val gson = GsonBuilder()
        .registerTypeAdapter(
            OffsetDateTime::class.java,
            CustomGsonObjectMapperFactory.OffsetDateTimeTypeAdapter(),
        )
        .create()

    var connectionEvents: MutableList<ConnectionEvent> = mutableListOf()
    var credentialEvents: MutableList<CredentialEvent> = mutableListOf()
    var presentationEvents: MutableList<PresentationEvent> = mutableListOf()
    var didEvents: MutableList<DidEvent> = mutableListOf()
    var authCodeCallbackEvents: MutableList<Pair<String, String>> = mutableListOf()

    private fun route(application: Application) {
        application.routing {
            post("/") {
                val eventString = call.receiveText()
                val event = gson.fromJson(eventString, Event::class.java)
                when (event.type) {
                    TestConstants.EVENT_TYPE_CONNECTION_UPDATED -> connectionEvents.add(
                        gson.fromJson(
                            eventString,
                            ConnectionEvent::class.java,
                        ),
                    )

                    TestConstants.EVENT_TYPE_ISSUE_CREDENTIAL_RECORD_UPDATED -> credentialEvents.add(
                        gson.fromJson(
                            eventString,
                            CredentialEvent::class.java,
                        ),
                    )

                    TestConstants.EVENT_TYPE_PRESENTATION_UPDATED -> presentationEvents.add(
                        gson.fromJson(
                            eventString,
                            PresentationEvent::class.java,
                        ),
                    )

                    TestConstants.EVENT_TYPE_DID_STATUS_UPDATED -> didEvents.add(
                        gson.fromJson(
                            eventString,
                            DidEvent::class.java,
                        ),
                    )

                    else -> {
                        throw IllegalArgumentException("ERROR: unknown event type ${event.type}")
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
            get("/auth-cb") {
                val authCode = call.parameters["code"]!!
                val state = call.parameters["state"]!!
                authCodeCallbackEvents.add(Pair(authCode, state))
                call.respond(HttpStatusCode.OK, "Login Successfully")
            }
        }
    }

    companion object {
        fun at(url: URL, webhookPort: Int?): ListenToEvents {
            return ListenToEvents(url, webhookPort)
        }

        fun with(actor: Actor): ListenToEvents {
            return actor.abilityTo(ListenToEvents::class.java)
        }

        fun presentationProofStatus(actor: Actor): Question<PresentationStatusAdapter.Status?> {
            return Question.about("presentation status").answeredBy {
                val proofEvent = with(actor).presentationEvents.lastOrNull {
                    it.data.thid == actor.recall<String>("thid")
                }
                proofEvent?.data?.status
            }
        }

        fun connectionState(actor: Actor): Question<Connection.State?> {
            return Question.about("connection state").answeredBy {
                val lastEvent = with(actor).connectionEvents.lastOrNull {
                    it.data.thid == actor.recall<Connection>("connection").thid
                }
                lastEvent?.data?.state
            }
        }

        fun credentialState(actor: Actor): Question<IssueCredentialRecord.ProtocolState?> {
            return Question.about("credential state").answeredBy {
                val credentialEvent = ListenToEvents.with(actor).credentialEvents.lastOrNull {
                    it.data.thid == actor.recall<String>("thid")
                }
                credentialEvent?.data?.protocolState
            }
        }

        fun didStatus(actor: Actor): Question<String> {
            return Question.about("did status").answeredBy {
                val didEvent = ListenToEvents.with(actor).didEvents.lastOrNull {
                    it.data.did == actor.recall<String>("shortFormDid")
                }
                didEvent?.data?.status
            }
        }
    }

    init {
        server = embeddedServer(
            Netty,
            port = webhookPort ?: url.port,
            host = "0.0.0.0",
            module = { route(this) },
        )
            .start(wait = false)
    }

    override fun toString(): String {
        return "Listen HTTP port at $url"
    }

    override fun tearDown() {
        server.stop()
    }
}
