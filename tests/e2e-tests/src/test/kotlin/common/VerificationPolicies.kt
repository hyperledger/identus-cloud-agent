package common

import api_models.Constraint
import api_models.VerificationPolicy

object VerificationPolicies {

    val schemaId = "http://atalaprism.io/schemas/1.0/StudentCredential"
    val trustedIssuer1 = "did:example:123456789abcdefghi"
    val trustedIssuer2 = "did:example:123456789abcdefghj"

    val VERIFICATION_POLICY = VerificationPolicy(
        name = "Trusted Issuer and SchemaID",
        description = "Verification Policy with trusted issuer and schemaId",
        constraints = listOf(
            Constraint(
                schemaId = schemaId,
                trustedIssuers = listOf(
                    trustedIssuer1,
                    trustedIssuer2
                )
            )
        )
    )
}
