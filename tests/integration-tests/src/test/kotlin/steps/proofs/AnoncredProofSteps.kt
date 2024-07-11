package steps.proofs

import interactions.*
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.abilities.CallAnApi
import org.apache.http.HttpStatus.SC_CREATED
import org.hyperledger.identus.client.models.*

class AnoncredProofSteps {

    @When("{actor} sends a anoncreds request for proof presentation to {actor} using credential definition issued by {actor}")
    fun verifierSendsAnAnoncredRequestForProofPresentationToHolder(verifier: Actor, holder: Actor, issuer: Actor) {
        val credentialDefinitionRegistryUrl =
            issuer.usingAbilityTo(CallAnApi::class.java)
                .resolve("/credential-definition-registry/definitions")
                .replace("localhost", "host.docker.internal")
        val credentialDefinitionId =
            "$credentialDefinitionRegistryUrl/${issuer.recall<CredentialDefinitionResponse>("anoncredsCredentialDefinition").guid}/definition"

        val anoncredsPresentationRequestV1 = AnoncredPresentationRequestV1(
            requestedAttributes = mapOf(
                "sex" to
                    AnoncredRequestedAttributeV1(
                        name = "sex",
                        restrictions = listOf(
                            mapOf(
                                ("attr::sex::value" to "M"),
                                ("cred_def_id" to credentialDefinitionId),
                            ),
                        ),
                    ),
            ),
            requestedPredicates = mapOf(
                "age" to AnoncredRequestedPredicateV1(
                    name = "age",
                    pType = ">=",
                    pValue = 18,
                    restrictions = emptyList(),
                ),
            ),
            name = "proof_req_1",
            nonce = "1103253414365527824079144",
            version = "0.1",
        )
        val presentationRequest = RequestPresentationInput(
            connectionId = verifier.recall<Connection>("connection-with-${holder.name}").connectionId,
            credentialFormat = "AnonCreds",
            anoncredPresentationRequest = anoncredsPresentationRequestV1,
            proofs = emptyList(),
        )
        verifier.attemptsTo(
            Post.to("/present-proof/presentations").body(presentationRequest),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )
        val presentationStatus = SerenityRest.lastResponse().get<PresentationStatus>()
        verifier.remember("thid", presentationStatus.thid)
        holder.remember("thid", presentationStatus.thid)
    }

    @When("{actor} accepts the anoncreds presentation request")
    fun holderAcceptsTheAnoncredsPresentationWithProof(holder: Actor) {
        val requestPresentationAction = RequestPresentationAction(
            anoncredPresentationRequest =
            AnoncredCredentialProofsV1(
                listOf(
                    AnoncredCredentialProofV1(
                        holder.recall<IssueCredentialRecord>("issuedCredential").recordId,
                        listOf("sex"),
                        listOf("age"),
                    ),
                ),
            ),
            action = RequestPresentationAction.Action.REQUEST_MINUS_ACCEPT,
        )

        val presentationId = holder.recall<String>("presentationId")
        holder.attemptsTo(
            Patch.to("/present-proof/presentations/$presentationId").body(requestPresentationAction),
        )
    }
}
