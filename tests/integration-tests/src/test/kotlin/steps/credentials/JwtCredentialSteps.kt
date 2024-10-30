package steps.credentials

import common.CredentialSchema
import interactions.Post
import interactions.body
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK
import org.hyperledger.identus.client.models.*

class JwtCredentialSteps {

    private fun sendCredentialOffer(
        issuer: Actor,
        holder: Actor,
        didForm: String,
        schemaGuid: String?,
        claims: Map<String, Any>,
        issuingKid: String?,
    ) {
        val did: String = if (didForm == "short") {
            issuer.recall("shortFormDid")
        } else {
            issuer.recall("longFormDid")
        }

        val schemaId: String? = if (schemaGuid != null) {
            val baseUrl = issuer.recall<String>("baseUrl")
            "$baseUrl/schema-registry/schemas/$schemaGuid"
        } else {
            null
        }

        val credentialOfferRequest = CreateIssueCredentialRecordRequest(
            schemaId = schemaId?.let { listOf(it) },
            claims = claims,
            issuingDID = did,
            issuingKid = issuingKid,
            connectionId = issuer.recall<Connection>("connection-with-${holder.name}").connectionId,
            validityPeriod = 3600.0,
            credentialFormat = "JWT",
            automaticIssuance = false,
        )

        issuer.attemptsTo(
            Post.to("/issue-credentials/credential-offers").body(credentialOfferRequest),
        )
    }

    private fun saveCredentialOffer(issuer: Actor, holder: Actor) {
        val credentialRecord = SerenityRest.lastResponse().get<IssueCredentialRecord>()

        issuer.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )

        issuer.remember("thid", credentialRecord.thid)
        holder.remember("thid", credentialRecord.thid)
    }

    @When("{actor} offers a jwt credential to {actor} with {string} form DID")
    fun issuerOffersAJwtCredential(issuer: Actor, holder: Actor, format: String) {
        val claims = linkedMapOf(
            "firstName" to "FirstName",
            "lastName" to "LastName",
        )
        sendCredentialOffer(issuer, holder, format, null, claims, "assertion-1")
        saveCredentialOffer(issuer, holder)
    }

    @When("{actor} offers a jwt credential to {actor} with {string} form DID using issuingKid {string}")
    fun issuerOffersAJwtCredentialWithIssuingKeyId(issuer: Actor, holder: Actor, format: String, issuingKid: String?) {
        val claims = linkedMapOf(
            "firstName" to "FirstName",
            "lastName" to "LastName",
        )
        sendCredentialOffer(issuer, holder, format, null, claims, issuingKid)
        saveCredentialOffer(issuer, holder)
    }

    @When("{actor} offers a jwt credential to {actor} with {} form using {} schema")
    fun issuerOffersJwtCredentialToHolderUsingSchema(
        issuer: Actor,
        holder: Actor,
        format: String,
        schema: CredentialSchema,
    ) {
        val schemaGuid = issuer.recall<String>(schema.name)
        val claims = schema.claims
        sendCredentialOffer(issuer, holder, format, schemaGuid, claims, "assertion-1")
        saveCredentialOffer(issuer, holder)
    }

    @When("{actor} offers a jwt credential to {actor} with {} form DID with wrong claims structure using {} schema")
    fun issuerOffersJwtCredentialToHolderWithWrongClaimStructure(
        issuer: Actor,
        holder: Actor,
        format: String,
        schema: CredentialSchema,
    ) {
        val schemaGuid = issuer.recall<String>(schema.name)!!
        val claims = linkedMapOf(
            "name" to "Name",
            "surname" to "Surname",
        )
        sendCredentialOffer(issuer, holder, format, schemaGuid, claims, "assertion-1")
    }

    @When("{actor} accepts jwt credential offer")
    fun holderAcceptsJwtCredentialOfferForJwt(holder: Actor) {
        val recordId = holder.recall<String>("recordId")
        holder.attemptsTo(
            Post.to("/issue-credentials/records/$recordId/accept-offer")
                .body(AcceptCredentialOfferRequest(holder.recall("longFormDid"), holder.recall("kidSecp256K1"))),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
    }

    @When("{actor} accepts jwt credential offer with keyId {string}")
    fun holderAcceptsJwtCredentialOfferForJwtWithKeyId(holder: Actor, keyId: String?) {
        val recordId = holder.recall<String>("recordId")
        holder.attemptsTo(
            Post.to("/issue-credentials/records/$recordId/accept-offer")
                .body(AcceptCredentialOfferRequest(holder.recall("longFormDid"), keyId)),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
    }
}
