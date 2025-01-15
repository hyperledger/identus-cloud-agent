package common

import org.hyperledger.identus.client.models.*
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
                schemaId = schemaUrl,
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
                issuingKid = assertionKey,
                connectionId = connectionId,
                credentialFormat = credentialType.format,
                automaticIssuance = false,
                claims = null,
                issuingDID = "",
                jwtVcPropertiesV1 = JwtVCPropertiesV1(
                    credentialSchema = CredentialSchemaRef(
                        id = schemaUrl!!,
                        type = "JsonSchemaValidator2018",
                    ),
                    claims = claims,
                    issuingDID = did,
                    validityPeriod = validityPeriod ?: 3600.0,
                ),
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
