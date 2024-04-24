package steps

import common.Configuration
import common.Configuration.WAITING_LOOP_COUNTER_NAME
import common.Configuration.WAITING_LOOP_MAX_ITERATIONS
import common.Configuration.WAITING_LOOP_PAUSE_INTERVAL
import common.Utils
import common.Utils.extractOutOfBandInvitationFromUrl
import common.Utils.logger
import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_OK

object ConnectionSteps {

    private val logger by Utils.logger("Connection Steps")

    /**
     * Generates a Connection invitation
     *
     * Function saves the following session variables to global virtual session user state:
     * "invitationUrl" - invitation URL to be used next by the holder
     * "inviterConnectionId" - connection ID for the inviter
     *
     * @param url URL of a PRISM Agent
     * @param apikey secret access api key
     * @param label Connection invitation label
     * @return Builder of a Gatling chain of Actions to be used in Gatling Simulation
     */
    fun generateInvitation(
        url: String = Configuration.ISSUER_AGENT_URL,
        apikey: String = Configuration.ISSUER_AGENT_API_KEY,
        label: String = "test"
    ): ChainBuilder =
        exec(
            http("Inviter generates connection invitation")
                .post("$url/connections")
                .header("content-type", "application/json")
                .header("apikey", apikey)
                .body(StringBody("""{"label": "$label"}"""))
                .check(
                    status().shouldBe(HTTP_CREATED),
                    jsonPath("$.invitation.invitationUrl").find().saveAs("invitationUrl"),
                    jsonPath("$.connectionId").find().saveAs("inviterConnectionId")
                )
        ).exec { session ->
            logger.info("Inviter connection ID: ${session.getString("inviterConnectionId")}")
            session
        }.exitHereIfFailed()


    /**
     * Invitee sends a connection request using invitor invitationUrl
     *
     * Uses `invitationUrl` achieved by executing `generateInvitation` function
     *
     * @param url URL of a PRISM Agent
     * @param apikey secret access api key
     * @return Builder of a Gatling chain of Actions to be used in Gatling Simulation
     */
    fun inviteeSendsConnectionRequest(
        url: String = Configuration.HOLDER_AGENT_URL,
        apikey: String = Configuration.HOLDER_AGENT_API_KEY
    ): ChainBuilder =
        exec(
            http("Invitee sends a connection request")
                .post("$url/connection-invitations")
                .header("content-type", "application/json")
                .header("apikey", apikey)
                .body(
                    StringBody { session ->
                        """{ "invitation": "${extractOutOfBandInvitationFromUrl(session.getString("invitationUrl")!!)}" }"""
                    }
                )
                .check(
                    status().shouldBe(HTTP_OK),
                    jsonPath("$.connectionId").find().saveAs("inviteeConnectionId"),
                    jsonPath("$.myDid").find().saveAs("holderDid")
                )
        ).exec { session ->
            logger.info("Invitee connection ID: ${session.getString("inviteeConnectionId")}")
            session
        }.exitHereIfFailed()

    /**
     * Inviter receives the connection request and sends the connection response to invitee
     *
     * @param url URL of a PRISM Agent
     * @param apikey secret access api key
     * @return Builder of a Gatling chain of Actions to be used in Gatling Simulation
     */
    fun inviterReceivesTheConnectionRequestAndSendsTheConnectionResponseToInvitee(
        url: String = Configuration.ISSUER_AGENT_URL,
        apikey: String = Configuration.ISSUER_AGENT_API_KEY
    ): ChainBuilder =
        doWhile(
            { session -> session.getString("inviterConnectionState") != "ConnectionResponseSent" },
            WAITING_LOOP_COUNTER_NAME)
            .on(
                exec(
                    http("Inviter receives connection request and sends response back")
                        .get("$url/connections/#{inviterConnectionId}")
                        .header("content-type", "application/json")
                        .header("apikey", apikey)
                        .check(
                            status().shouldBe(HTTP_OK),
                            jsonPath("$.state").find().saveAs("inviterConnectionState")
                        )
                ).exec { session ->
                    logger.info("Inviter connection state: ${session.getString("inviterConnectionState")}")
                    session
                }.pause(WAITING_LOOP_PAUSE_INTERVAL).exitHereIf { session ->
                    session.getInt(WAITING_LOOP_COUNTER_NAME) == WAITING_LOOP_MAX_ITERATIONS
                }
            )

    /**
     * Invitee achieves connection response
     *
     * @param url URL of a PRISM Agent
     * @param apikey secret access api key
     * @return Builder of a Gatling chain of Actions to be used in Gatling Simulation
     */
    fun inviteeAchievesConnectionResponse(
        url: String = Configuration.HOLDER_AGENT_URL,
        apikey: String = Configuration.HOLDER_AGENT_API_KEY
    ): ChainBuilder =
        doWhile(
            { session -> session.getString("inviteeConnectionState") != "ConnectionResponseReceived" },
            WAITING_LOOP_COUNTER_NAME)
            .on(
                exec(
                    http("Invitee achieves connection response")
                        .get("$url/connections/#{inviteeConnectionId}" )
                        .header("content-type", "application/json")
                        .header("apikey", apikey)
                        .check(
                            status().shouldBe(HTTP_OK),
                            jsonPath("$.state").find().saveAs("inviteeConnectionState"),
                        )
                ).exec { session ->
                    logger.info("Invitee connection state: ${session.getString("inviteeConnectionState")}")
                    session
                }.pause(WAITING_LOOP_PAUSE_INTERVAL).exitHereIf { session ->
                    session.getInt(WAITING_LOOP_COUNTER_NAME) == WAITING_LOOP_MAX_ITERATIONS
                }
            )

    /**
     * Delete all connections from a PRISM Agent
     *
     * @param url URL of a PRISM Agent
     * @param apikey secret access api key
     * @return Builder of a Gatling chain of Actions to be used in Gatling Simulation
     */
    fun deleteAllConnections(
        url: String = Configuration.ISSUER_AGENT_URL,
        apikey: String = Configuration.ISSUER_AGENT_API_KEY
    ): ChainBuilder =
        exec(
            http("Get connections")
                .get("$url/connections")
                .header("apikey", apikey)
                .header("content-type", "application/json")
                .check(
                    status().shouldBe(HTTP_OK),
                    jsonPath("$..connectionId").findAll().saveAs("connections")
                )
        ).foreach({ session -> session.getList<String>("connections") }, "connection").on(
            exec(
                http("Delete connection")
                    .delete("${url}/connections/#{connection}")
                    .header("apikey", apikey)
            ).exec { session ->
                logger.info("Current connection: ${session.getString("connection")}")
                session
            }
        )
}
