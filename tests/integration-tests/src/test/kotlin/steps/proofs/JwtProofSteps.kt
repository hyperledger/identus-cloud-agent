package steps.proofs

import interactions.*
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK
import org.hyperledger.identus.client.models.*

class JwtProofSteps {

    @When("{actor} sends a request for jwt proof presentation to {actor}")
    fun verifierSendsARequestForJwtProofPresentationToHolder(verifier: Actor, holder: Actor) {
        val verifierConnectionToHolder = verifier.recall<Connection>("connection-with-${holder.name}").connectionId
        val presentationRequest = RequestPresentationInput(
            connectionId = verifierConnectionToHolder,
            options = Options(
                challenge = "11c91493-01b3-4c4d-ac36-b336bab5bddf",
                domain = "https://example-verifier.com",
            ),
            proofs = listOf(
                ProofRequestAux(
                    schemaId = "https://schema.org/Person",
                    trustIssuers = listOf("did:web:atalaprism.io/users/testUser"),
                ),
            ),
        )
        verifier.attemptsTo(
            Post.to("/present-proof/presentations").body(presentationRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )
        val presentationStatus = SerenityRest.lastResponse().get<PresentationStatus>()
        verifier.remember("thid", presentationStatus.thid)
        holder.remember("thid", presentationStatus.thid)
    }

    @When("{actor} makes the jwt presentation of the proof")
    fun holderMakesThePresentationOfTheProofToVerifier(holder: Actor) {
        val requestPresentationAction = RequestPresentationAction(
            proofId = listOf(holder.recall<IssueCredentialRecord>("issuedCredential").recordId),
            action = RequestPresentationAction.Action.REQUEST_MINUS_ACCEPT,
        )
        val presentationId: String = holder.recall("presentationId")
        holder.attemptsTo(
            Patch.to("/present-proof/presentations/$presentationId").body(requestPresentationAction),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
    }
}
