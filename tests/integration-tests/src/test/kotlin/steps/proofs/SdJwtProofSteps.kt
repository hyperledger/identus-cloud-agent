package steps.proofs

import abilities.ListenToEvents
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.nimbusds.jose.util.Base64URL
import interactions.*
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import models.JwtCredential
import models.SdJwtClaim
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK
import org.hyperledger.identus.client.models.*

class SdJwtProofSteps {

    @When("{actor} sends a request for sd-jwt proof presentation to {actor} requesting [{}] claims")
    fun verifierSendsARequestForSdJwtProofPresentationToHolder(verifier: Actor, holder: Actor, keys: String) {
        val claims = JsonObject()
        for (key in keys.split(",")) {
            claims.addProperty(key, "{}")
        }
        val verifierConnectionToHolder = verifier.recall<Connection>("connection-with-${holder.name}").connectionId
        val presentationRequest = RequestPresentationInput(
            connectionId = verifierConnectionToHolder,
            options = Options(
                challenge = "11c91493-01b3-4c4d-ac36-b336bab5bddf",
                domain = "https://example-verifier.com",
            ),
            proofs = listOf(),
            credentialFormat = "SDJWT",
            claims = claims,
        )
        verifier.attemptsTo(
            Post.to("/present-proof/presentations").body(presentationRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )
        val presentationStatus = SerenityRest.lastResponse().get<PresentationStatus>()
        verifier.remember("thid", presentationStatus.thid)
        holder.remember("thid", presentationStatus.thid)
    }

    @When("{actor} makes the sd-jwt presentation of the proof disclosing [{}] claims")
    fun holderMakesThePresentationOfTheProofToVerifier(holder: Actor, keys: String) {
        val claims = JsonObject()
        for (key in keys.split(",")) {
            claims.addProperty(key, "{}")
        }

        val requestPresentationAction = RequestPresentationAction(
            proofId = listOf(holder.recall<IssueCredentialRecord>("issuedCredential").recordId),
            action = RequestPresentationAction.Action.REQUEST_MINUS_ACCEPT,
            claims = claims,
            credentialFormat = "SDJWT",
        )

        val presentationId: String = holder.recall("presentationId")
        holder.attemptsTo(
            Patch.to("/present-proof/presentations/$presentationId").body(requestPresentationAction),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
    }

    @Then("{actor} has the sd-jwt proof verified")
    fun verifierHasTheSdJwtProofVerified(verifier: Actor) {
        VerifierProofSteps().verifierHasTheProofVerified(verifier)
        val proofEvent = ListenToEvents.with(verifier).presentationEvents.last().data
        val sdjwt = proofEvent.data!!.first()
        val isBound = !sdjwt.endsWith("~") // if it ends with a ~ there's no binding

        val jwt = JwtCredential.parseJwt(sdjwt.split("~").first())
        val claims = sdjwt.split("~")
            .drop(1)
            .dropLast(if (isBound) 1 else 0)
            .dropLastWhile { it.isBlank() }
            .map { Base64URL.from(it).decodeToString() }
            .map { Gson().fromJson(it, Array<String>::class.java) }
            .associate { it[1] to SdJwtClaim(salt = it[0], key = it[1], value = it[2]) }

        verifier.attemptsTo(
            Ensure.that(claims.containsKey("firstName")).isTrue(),
            Ensure.that(claims.containsKey("lastName")).isFalse(),
        )

        if (isBound) {
            val bindingJwt = JwtCredential.parseJwt(sdjwt.split("~").last())
            val payload = Gson().toJsonTree(bindingJwt.payload!!.toJSONObject()).asJsonObject

            verifier.attemptsTo(
                Ensure.that(payload.get("aud").asString).isEqualTo("https://example-verifier.com"),
            )
        }
    }
}
