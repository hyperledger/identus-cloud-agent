package common

import org.hyperledger.identus.client.models.*
import java.util.UUID

enum class CreateCredentialOfferAPIVersion {
    V0 {
        override fun buildJWTCredentialOfferRequest(
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

        override fun buildSDJWTCredentialOfferRequest(
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

        override fun buildAnonCredsCredentialOfferRequest(
            credentialType: CredentialType,
            did: String,
            credentialDefinitionId: UUID,
            claims: Map<String, Any>,
            connectionId: UUID,
            validityPeriod: Double?,
        ) = CreateIssueCredentialRecordRequest(
            credentialDefinitionId = credentialDefinitionId,
            claims = claims,
            issuingDID = did,
            connectionId = connectionId,
            validityPeriod = validityPeriod ?: 3600.0,
            credentialFormat = credentialType.format,
            automaticIssuance = false,
        )
    },

    V1 {
        override fun buildJWTCredentialOfferRequest(
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
                throw IllegalArgumentException("Unsupported credential type: $credentialType")
            },
        )

        override fun buildSDJWTCredentialOfferRequest(
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
                throw IllegalArgumentException("Unsupported credential type: $credentialType")
            },
        )

        override fun buildAnonCredsCredentialOfferRequest(
            credentialType: CredentialType,
            did: String,
            credentialDefinitionId: UUID,
            claims: Map<String, Any>,
            connectionId: UUID,
            validityPeriod: Double?,
        ) = CreateIssueCredentialRecordRequest(
            connectionId = connectionId,
            credentialFormat = credentialType.format,
            automaticIssuance = false,
            anoncredsVcPropertiesV1 = AnonCredsVCPropertiesV1(
                issuingDID = did,
                credentialDefinitionId = credentialDefinitionId,
                claims = claims,
                validityPeriod = validityPeriod ?: 3600.0,
            ),
        )
    },
    ;

    abstract fun buildJWTCredentialOfferRequest(
        credentialType: CredentialType,
        did: String,
        assertionKey: String,
        schemaUrl: String?,
        claims: Map<String, Any>,
        connectionId: UUID,
        validityPeriod: Double? = null,
    ): CreateIssueCredentialRecordRequest

    abstract fun buildSDJWTCredentialOfferRequest(
        credentialType: CredentialType,
        did: String,
        assertionKey: String,
        schemaUrl: String?,
        claims: Map<String, Any>,
        connectionId: UUID,
        validityPeriod: Double? = null,
    ): CreateIssueCredentialRecordRequest

    abstract fun buildAnonCredsCredentialOfferRequest(
        credentialType: CredentialType,
        did: String,
        credentialDefinitionId: UUID,
        claims: Map<String, Any>,
        connectionId: UUID,
        validityPeriod: Double? = null,
    ): CreateIssueCredentialRecordRequest
}
