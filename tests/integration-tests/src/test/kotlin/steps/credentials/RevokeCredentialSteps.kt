package steps.credentials

import interactions.*
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.automation.utils.Wait
import models.JwtCredential
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.hyperledger.identus.client.models.IssueCredentialRecord
import kotlin.time.Duration.Companion.seconds

class RevokeCredentialSteps {
    @When("{actor} revokes the credential issued to {actor}")
    fun issuerRevokesCredentialsIssuedToHolder(issuer: Actor, holder: Actor) {
        val issuedCredential = issuer.recall<IssueCredentialRecord>("issuedCredential")
//        val jwtCred = JwtCredential(issuedCredential.credential!!)
//        val statusListId = statusListId(jwtCred)
//        issuer.remember("statusListId", statusListId)
//
//        issuer.attemptsTo(
//            Get.resource("/credential-status/$statusListId"),
//        )
//        val encodedList = SerenityRest.lastResponse().get<String>("credentialSubject.encodedList")
//        issuer.remember("encodedStatusList", encodedList)
//
//        issuer.attemptsTo(
//            Patch.to("/credential-status/revoke-credential/${issuedCredential.recordId}"),
//            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK),
//        )
    }

    @When("{actor} tries to revoke credential from {actor}")
    fun holderTriesToRevokeCredentialFromIssuer(holder: Actor, issuer: Actor) {
        val issuedCredential = issuer.recall<IssueCredentialRecord>("issuedCredential")
        val receivedCredential = holder.recall<IssueCredentialRecord>("issuedCredential")
        holder.attemptsTo(
            Patch.to("/credential-status/revoke-credential/${issuedCredential.recordId}"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_NOT_FOUND),
        )
        holder.attemptsTo(
            Patch.to("/credential-status/revoke-credential/${receivedCredential.recordId}"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_UNPROCESSABLE_ENTITY),
        )
    }

    @Then("{actor} should see the credential was revoked")
    fun credentialShouldBeRevoked(issuer: Actor) {
        Wait.until(
            timeout = 60.seconds,
            errorMessage = "Encoded Status List didn't change after revoking.",
        ) {
            val statusListId: String = issuer.recall("statusListId")
            val encodedStatusList: String = issuer.recall("encodedStatusList")
            issuer.attemptsTo(
                Get.resource("/credential-status/$statusListId"),
            )
            val actualEncodedList: String = SerenityRest.lastResponse().jsonPath().get("credentialSubject.encodedList")
            actualEncodedList != encodedStatusList
        }
    }

    @Then("{actor} should see the credential is not revoked")
    fun issuerShouldSeeTheCredentialIsNotRevoked(issuer: Actor) {
        val issuedCredential = issuer.recall<IssueCredentialRecord>("issuedCredential")
//        val jwtCred = JwtCredential(issuedCredential.credential!!)
//        val statusListId = statusListId(jwtCred)
//        issuer.remember("statusListId", statusListId)
//
//        issuer.attemptsTo(
//            Get.resource("/credential-status/$statusListId"),
//        )
    }

    private fun statusListId(jwtCredential: JwtCredential): String {
//        val listUrl = jwtCredential.payload.read<String>("$.vc.credentialStatus.statusListCredential")
//        return listUrl.split("/credential-status/")[1]
        return ""
    }
}
