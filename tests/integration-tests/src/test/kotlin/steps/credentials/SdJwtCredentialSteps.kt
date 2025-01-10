package steps.credentials

import com.google.gson.Gson
import com.nimbusds.jose.util.Base64URL
import common.CredentialSchema
import interactions.Post
import interactions.body
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import models.JwtCredential
import models.SdJwtClaim
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.*
import org.hyperledger.identus.client.models.*

class SdJwtCredentialSteps {

    @When("{actor} offers a sd-jwt credential to {actor}")
    fun issuerOffersSdJwtCredentialToHolder(issuer: Actor, holder: Actor) {
        val connectionId = issuer.recall<Connection>("connection-with-${holder.name}").connectionId
        val did = issuer.recall<String>("shortFormDid")
        val schemaGuid = issuer.recall<String>(CredentialSchema.ID_SCHEMA.name)

        val schemaId: String? = if (schemaGuid != null) {
            val baseUrl = issuer.recall<String>("baseUrl")
            "$baseUrl/schema-registry/schemas/$schemaGuid"
        } else {
            null
        }

        val credentialOfferRequest = CreateIssueCredentialRecordRequest(
            schemaId = schemaId,
            claims = CredentialSchema.ID_SCHEMA.claims,
            issuingDID = did,
            connectionId = connectionId,
            validityPeriod = 3600.0,
            credentialFormat = "SDJWT",
            automaticIssuance = false,
        )

        issuer.attemptsTo(
            Post.to("/issue-credentials/credential-offers").body(credentialOfferRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )

        val credentialRecord = SerenityRest.lastResponse().get<IssueCredentialRecord>()
        issuer.remember("thid", credentialRecord.thid)
        holder.remember("thid", credentialRecord.thid)
    }

    @When("{actor} accepts credential offer for sd-jwt")
    fun holderAcceptsSdJwtCredentialOffer(holder: Actor) {
        holderAcceptsSdJwtCredentialOfferWithKeyBinding(holder, null)
    }

    @When("{actor} accepts credential offer for sd-jwt with '{}' key binding")
    fun holderAcceptsSdJwtCredentialOfferWithKeyBinding(holder: Actor, key: String?) {
        val recordId = holder.recall<String>("recordId")
        val did = holder.recall<String>("longFormDid")
        val request = AcceptCredentialOfferRequest(subjectId = did, keyId = key)
        holder.attemptsTo(
            Post.to("/issue-credentials/records/$recordId/accept-offer").body(request),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
    }

//    @When("{actor} tries to offer a sd-jwt credential to {actor}")
//    fun issuerTriesToOfferSdJwtCredentialToHolder(issuer: Actor, holder: Actor) {
//        val connectionId = issuer.recall<Connection>("connection-with-${holder.name}").connectionId
//        val did = issuer.recall<String>("shortFormDid")
//
//        val credentialOfferRequest = CreateIssueCredentialRecordRequest(
//            claims = claims,
//            issuingDID = did,
//            connectionId = connectionId,
//            validityPeriod = 3600.0,
//            credentialFormat = "SDJWT",
//            automaticIssuance = false,
//        )
//
//        issuer.attemptsTo(
//            Post.to("/issue-credentials/credential-offers").body(credentialOfferRequest),
//        )
//    }

    @Then("{actor} checks the sd-jwt credential contents")
    fun holderChecksTheSdJwtCredentialContents(holder: Actor) {
        commonValidation(holder) { payload, _ ->
            holder.attemptsTo(
                Ensure.that(payload.containsKey("cnf")).isFalse(),
            )
        }
    }

    @Then("{actor} checks the sd-jwt credential contents with holder binding")
    fun holderChecksTheSdJwtCredentialContentsWithHolderBinding(holder: Actor) {
        commonValidation(holder) { payload, _ ->
            holder.attemptsTo(
                Ensure.that(payload.containsKey("cnf")).isTrue(),
            )
        }
    }

    @Then("{actor} should see the issuance has failed")
    fun issuerShouldSeeTheIssuanceHasFailed(issuer: Actor) {
        issuer.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_EXPECTATION_FAILED),
        )
    }

    private fun commonValidation(
        holder: Actor,
        additionalChecks: (payload: Map<String, Any>, disclosedClaims: Map<String, SdJwtClaim>) -> Unit,
    ) {
        val issuedCredential = holder.recall<IssueCredentialRecord>("issuedCredential")
        val jwtCredential = JwtCredential.parseBase64(issuedCredential.credential!!)
        val actualClaims = CredentialSchema.ID_SCHEMA.claims.mapValues { it.value.toString() }

        val payload = jwtCredential.payload!!.toJSONObject()

        val disclosedClaims = jwtCredential.signature!!.toString().split("~")
            .drop(1)
            .dropLastWhile { it.isBlank() }
            .map { Base64URL.from(it).decodeToString() }
            .map { Gson().fromJson(it, Array<String>::class.java) }
            .associate { it[1] to SdJwtClaim(salt = it[0], key = it[1], value = it[2]) }

        holder.attemptsTo(
            Ensure.that(payload.containsKey("_sd")).isTrue(),
            Ensure.that(payload.containsKey("_sd_alg")).isTrue(),
            Ensure.that(payload.containsKey("iss")).isTrue(),
            Ensure.that(payload.containsKey("iat")).isTrue(),
            Ensure.that(payload.containsKey("exp")).isTrue(),
            Ensure.that(disclosedClaims["firstName"]!!.value).isEqualTo(actualClaims["firstName"]!!),
            Ensure.that(disclosedClaims["lastName"]!!.value).isEqualTo(actualClaims["lastName"]!!),
        )

        additionalChecks(payload, disclosedClaims)
    }
}
