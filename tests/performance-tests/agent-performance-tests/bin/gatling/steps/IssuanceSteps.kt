package steps

import common.Configuration
import common.Configuration.RANDOM_CREDENTIAL
import common.Configuration.WAITING_LOOP_COUNTER_NAME
import common.Configuration.WAITING_LOOP_MAX_ITERATIONS
import common.Configuration.WAITING_LOOP_PAUSE_INTERVAL
import common.Utils
import common.Utils.logger
import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_OK

object IssuanceSteps {

    private val logger by Utils.logger()

    fun issuerOffersACredential(
        url: String = Configuration.ISSUER_AGENT_URL,
        apikey: String = Configuration.ISSUER_AGENT_API_KEY
    ): ChainBuilder =
        exec(
            http("Issuer offers a credential")
                .post("$url/issue-credentials/credential-offers")
                .header("content-type", "application/json")
                .header("apikey", apikey)
                .body(StringBody(RANDOM_CREDENTIAL))
                .check(
                    status().shouldBe(HTTP_CREATED),
                    jsonPath("$.recordId").find().saveAs("issuerRecordId")
                )
        ).exec { session ->
            logger.info("Issuer record ID: ${session.getString("issuerRecordId")}")
            session
        }.exitHereIfFailed()

    fun holderAwaitsCredentialOffer(
        url: String = Configuration.HOLDER_AGENT_URL,
        apikey: String = Configuration.HOLDER_AGENT_API_KEY
    ): ChainBuilder =
        doWhile(
            { session -> session.getString("holderRecordId") == "" },
            WAITING_LOOP_COUNTER_NAME
        ).on(
            exec(
                http("Holder gets credential records")
                    .get("$url/issue-credentials/records")
                    .header("content-type", "application/json")
                    .header("apikey", apikey)
                    .check(
                        status().shouldBe(HTTP_OK),
                        jsonPath("$.items[?(@.subjectId==\"#{holderDid}\")].recordId")
                            .withDefault("")
                            .saveAs("holderRecordId")
                    )
            ).exec { session ->
                logger.info("Achieved credential ID: ${session.getString("holderRecordId")}")
                session
            }.pause(WAITING_LOOP_PAUSE_INTERVAL).exitHereIf { session ->
                session.getInt(WAITING_LOOP_COUNTER_NAME) == WAITING_LOOP_MAX_ITERATIONS
            }
        )

    fun holderRequestsCredential(
        url: String = Configuration.HOLDER_AGENT_URL,
        apikey: String = Configuration.HOLDER_AGENT_API_KEY
    ): ChainBuilder =
        exec(
            http("Holder requests credential")
                .post("$url/issue-credentials/records/#{holderRecordId}/accept-offer")
                .header("content-type", "application/json")
                .header("apikey", apikey)
                .check(
                    status().shouldBe(HTTP_OK)
                )
        ).exitHereIfFailed()

    fun issuerReceivesRequest(
        url: String = Configuration.ISSUER_AGENT_URL,
        apikey: String = Configuration.ISSUER_AGENT_API_KEY
    ): ChainBuilder =
        doWhile(
            { session -> session.getString("issuerRecordIdState") != "RequestReceived"},
            WAITING_LOOP_COUNTER_NAME
        ).on(
            exec(
                http("Issuer record state achieves RequestReceived")
                    .get("$url/issue-credentials/records/#{issuerRecordId}")
                    .header("content-type", "application/json")
                    .header("apikey", apikey)
                    .check(
                        status().shouldBe(HTTP_OK),
                        jsonPath("$.protocolState").find().saveAs("issuerRecordIdState"),
                    )
            ).exec { session ->
                logger.info("Issuer recordId state: ${session.getString("issuerRecordIdState")}")
                session
            }.pause(WAITING_LOOP_PAUSE_INTERVAL).exitHereIf { session ->
                session.getInt(WAITING_LOOP_COUNTER_NAME) == WAITING_LOOP_MAX_ITERATIONS
            }
        )

    fun issuerIssuesCredential(
        url: String = Configuration.ISSUER_AGENT_URL,
        apikey: String = Configuration.ISSUER_AGENT_API_KEY
    ): ChainBuilder =
        exec(
            http("Issuer issues credential")
                .post("$url/issue-credentials/records/#{issuerRecordId}/issue-credential")
                .header("content-type", "application/json")
                .header("apikey", apikey)
                .check(
                    status().shouldBe(HTTP_OK)
                )
        ).exitHereIfFailed()

    fun issuerWaitsCredentialIssued(
        url: String = Configuration.ISSUER_AGENT_URL,
        apikey: String = Configuration.ISSUER_AGENT_API_KEY
    ): ChainBuilder =
        doWhile(
            { session -> session.getString("issuerRecordIdState") != "CredentialSent" },
            WAITING_LOOP_COUNTER_NAME
        ).on(
            exec(
                http("Issuer record state achieves CredentialSent")
                    .get("$url/issue-credentials/records/#{issuerRecordId}")
                    .header("content-type", "application/json")
                    .header("apikey", apikey)
                    .check(
                        status().shouldBe(HTTP_OK),
                        jsonPath("$.protocolState").find().saveAs("issuerRecordIdState"),
                    )
            ).exec { session ->
                logger.info("Issuer recordId state: ${session.getString("issuerRecordIdState")}")
                session
            }.pause(WAITING_LOOP_PAUSE_INTERVAL).exitHereIf { session ->
                session.getInt(WAITING_LOOP_COUNTER_NAME) == WAITING_LOOP_MAX_ITERATIONS
            }
        )

    fun holderAwaitsCredentialReceived(
        url: String = Configuration.HOLDER_AGENT_URL,
        apikey: String = Configuration.HOLDER_AGENT_API_KEY
    ): ChainBuilder =
        doWhile(
            { session -> session.getString("holderRecordIdState") != "CredentialReceived" },
            WAITING_LOOP_COUNTER_NAME
        ).on(
            exec(
                http("Holder record state achieves CredentialReceived")
                    .get("$url/issue-credentials/records/#{holderRecordId}")
                    .header("content-type", "application/json")
                    .header("apikey", apikey)
                    .check(
                        status().shouldBe(HTTP_OK),
                        jsonPath("$.protocolState").find().saveAs("holderRecordIdState"),
                    )
            ).exec { session ->
                logger.info("Holder recordId state: ${session.getString("holderRecordIdState")}")
                session
            }.pause(WAITING_LOOP_PAUSE_INTERVAL).exitHereIf { session ->
                session.getInt(WAITING_LOOP_COUNTER_NAME) == WAITING_LOOP_MAX_ITERATIONS
            }
        )
}
