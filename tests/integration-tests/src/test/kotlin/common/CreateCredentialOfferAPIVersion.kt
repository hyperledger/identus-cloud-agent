package common

import org.hyperledger.identus.client.models.CreateIssueCredentialRecordRequest
import java.util.UUID

enum class CreateCredentialOfferAPIVersion {
    V0 {
        override fun buildCredentialOfferRequest(
            credentialType: CredentialType,
            did: String,
            assertionKey: String,
            schemaUrl: String?,
            claims: Map<String, Any>,
            connectionId: UUID,
            validityPeriod: Double?,
        ): CreateIssueCredentialRecordRequest {
            return CreateIssueCredentialRecordRequest(
                schemaId = schemaUrl?.let { listOf(it) },
                claims = claims,
                issuingDID = did,
                issuingKid = assertionKey,
                connectionId = connectionId,
                validityPeriod = validityPeriod ?: 3600.0,
                credentialFormat = credentialType.format,
                automaticIssuance = false,
            )
        }
    },

    // TODO: it's a copy/paste from the V0, I have to regenerate the Kotlin HTTP client
    V1 {
        override fun buildCredentialOfferRequest(
            credentialType: CredentialType,
            did: String,
            assertionKey: String,
            schemaUrl: String?,
            claims: Map<String, Any>,
            connectionId: UUID,
            validityPeriod: Double?,
        ): CreateIssueCredentialRecordRequest {
            return CreateIssueCredentialRecordRequest(
                schemaId = schemaUrl?.let { listOf(it) },
                claims = claims,
                issuingDID = did,
                issuingKid = assertionKey,
                connectionId = connectionId,
                validityPeriod = validityPeriod ?: 3600.0,
                credentialFormat = credentialType.format,
                automaticIssuance = false,
            )
        }
    },
    ;

    abstract fun buildCredentialOfferRequest(
        credentialType: CredentialType,
        did: String,
        assertionKey: String,
        schemaUrl: String?,
        claims: Map<String, Any>,
        connectionId: UUID,
        validityPeriod: Double? = null,
    ): CreateIssueCredentialRecordRequest
}
