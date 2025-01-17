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
        ): CreateIssueCredentialRecordRequest = CreateIssueCredentialRecordRequest(
            schemaId = schemaUrl,
            claims = claims,
            issuingDID = did,
            issuingKid = assertionKey,
            connectionId = connectionId,
            validityPeriod = validityPeriod ?: 3600.0,
            credentialFormat = credentialType.format,
            automaticIssuance = false,
        )
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
        ): CreateIssueCredentialRecordRequest = CreateIssueCredentialRecordRequest(
            issuingKid = assertionKey,
            connectionId = connectionId,
            credentialFormat = credentialType.format,
            automaticIssuance = false,
            claims = null,
            issuingDID = null,
            jwtVcPropertiesV1 = if (credentialType == CredentialType.JWT_VCDM_1_1) {
                JwtVCPropertiesV1(
                    credentialSchema = CredentialSchemaRef(
                        id = schemaUrl!!,
                        type = "JsonSchemaValidator2018",
                    ),
                    claims = claims,
                    issuingDID = did,
                    issuingKid = assertionKey,
                    validityPeriod = validityPeriod ?: 3600.0,
                )
            } else {
                null
            },
            sdJwtVcPropertiesV1 = if (credentialType == CredentialType.SD_JWT_VCDM_1_1) {
                SDJWTVCPropertiesV1(
                    credentialSchema = CredentialSchemaRef(
                        id = schemaUrl!!,
                        type = "JsonSchemaValidator2018",
                    ),
                    claims = claims,
                    issuingDID = did,
                    issuingKid = assertionKey,
                    validityPeriod = validityPeriod ?: 3600.0,
                )
            } else {
                null
            },
        )
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
