package steps

import common.Configuration
import common.Utils
import common.Utils.logger
import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*

object IssuanceSteps {

    private val logger by Utils.logger()

    val issuerOffersACredential =
        exec(
            http("Issuer offers a credential")
                .post("${Configuration.ISSUER_AGENT_URL}/issue-credentials/credential-offers")
                .header("content-type", "application/json")
                .header("apikey", Configuration.ISSUER_AGENT_API_KEY)
                .body(
                    StringBody { session ->
                        """
              {
                "schemaId": "schema:1234",
                "subjectId": "${session.getString("holderDid")}",
                "validityPeriod": 3600,
                "claims": {
                  "prop1": "value1",
                  "prop2": "value2",
                  "prop3": "value3"
                },
                "automaticIssuance": false,
                "awaitConfirmation": false
              }
            """.trimIndent()
                    }
                )
                .check(
                    status().shouldBe(201),
                    jsonPath("$.recordId").find().saveAs("issuerRecordId")
                )
        ).exec { session ->
            logger.info("Issuer record ID: ${session.getString("issuerRecordId")}")
            session
        }.pause(10L)

    val holderRequestsCredential =
        exec(
            http("Holder gets credential records")
                .get("${Configuration.HOLDER_AGENT_URL}/issue-credentials/records")
                .header("content-type", "application/json")
                .header("apikey", Configuration.HOLDER_AGENT_API_KEY)
                .check(
                    status().shouldBe(200),
                    jsonPath("$.items[?(@.subjectId==\"#{holderDid}\")].recordId").find()
                        .saveAs("holderRecordId")
                )
        ).exec { session ->
            logger.info("Achieved credential ID: ${session.getString("holderRecordId")}")
            session
        }.exec(
            http("Holder requests credential")
                .post("${Configuration.HOLDER_AGENT_URL}/issue-credentials/records/#{holderRecordId}/accept-offer")
                .header("content-type", "application/json")
                .header("apikey", Configuration.HOLDER_AGENT_API_KEY)
                .check(
                    status().shouldBe(200)
                )
        )

    val issuerReceivesRequest =
        doWhile { session ->
            session.getString("issuerRecordIdState") != "RequestReceived"
        }.on(
            exec(
                http("Issuer record state achieves RequestReceived")
                    .get("${Configuration.ISSUER_AGENT_URL}/issue-credentials/records/#{issuerRecordId}")
                    .header("content-type", "application/json")
                    .header("apikey", Configuration.ISSUER_AGENT_API_KEY)
                    .check(
                        status().shouldBe(200),
                        jsonPath("$.protocolState").find().saveAs("issuerRecordIdState"),
                    )
            ).exec { session ->
                logger.info("Issuer recordId state: ${session.getString("issuerRecordIdState")}")
                session
            }.pause(2L)
        )

    val issuerIssuesCredential =
        exec(
            http("Issuer issues credential")
                .post("${Configuration.ISSUER_AGENT_URL}/issue-credentials/records/#{issuerRecordId}/issue-credential")
                .header("content-type", "application/json")
                .header("apikey", Configuration.ISSUER_AGENT_API_KEY)
                .check(
                    status().shouldBe(200)
                )
        ).doWhile { session ->
            session.getString("issuerRecordIdState") != "CredentialSent"
        }.on(
            exec(
                http("Issuer record state achieves CredentialSent")
                    .get("${Configuration.ISSUER_AGENT_URL}/issue-credentials/records/#{issuerRecordId}")
                    .header("content-type", "application/json")
                    .header("apikey", Configuration.ISSUER_AGENT_API_KEY)
                    .check(
                        status().shouldBe(200),
                        jsonPath("$.protocolState").find().saveAs("issuerRecordIdState"),
                    )
            ).exec { session ->
                logger.info("Issuer recordId state: ${session.getString("issuerRecordIdState")}")
                session
            }.pause(2L)
        )
}
