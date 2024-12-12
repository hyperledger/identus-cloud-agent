/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package org.hyperledger.identus.client.models

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.hyperledger.identus.client.adapters.StringOrStringArrayAdapter

/**
 *
 *
 * @param claims  The set of claims that will be included in the issued credential. The JSON object should comply with the schema applicable for this offer (i.e. 'schemaId' or 'credentialDefinitionId').
 * @param issuingDID  The issuer Prism DID by which the verifiable credential will be issued. DID can be short for or long form.
 * @param validityPeriod The validity period in seconds of the verifiable credential that will be issued.
 * @param schemaId
 * @param credentialDefinitionId  The unique identifier (UUID) of the credential definition that will be used for this offer. It should be the identifier of a credential definition that exists in the issuer agent's database. Note that this parameter only applies when the offer is of type 'AnonCreds'.
 * @param credentialFormat The credential format for this offer (defaults to 'JWT')
 * @param automaticIssuance  Specifies whether or not the credential should be automatically generated and issued when receiving the `CredentialRequest` from the holder. If set to `false`, a manual approval by the issuer via another API call will be required for the VC to be issued.
 * @param issuingKid  Specified the key ID (kid) of the DID, it will be used to sign credential. User should specify just the partial identifier of the key. The full id of the kid MUST be \"<issuingDID>#<kid>\" Note the cryto algorithm used with depend type of the key.
 * @param connectionId  The unique identifier of a DIDComm connection that already exists between the this issuer agent and the holder cloud or edeg agent. It should be the identifier of a connection that exists in the issuer agent's database. This connection will be used to execute the issue credential protocol. Note: connectionId is only required when the offer is from existing connection. connectionId is not required when the offer is from invitation for connectionless issuance.
 * @param goalCode   A self-attested code the receiver may want to display to the user or use in automatically deciding what to do with the out-of-band message.  goalcode is optional and can be provided when the offer is from invitation for connectionless issuance.
 * @param goal   A self-attested string that the receiver may want to display to the user about the context-specific goal of the out-of-band message.  goal is optional and can be provided when the offer is from invitation for connectionless issuance.
 */


data class CreateIssueCredentialRecordRequest(

    /*  The set of claims that will be included in the issued credential. The JSON object should comply with the schema applicable for this offer (i.e. 'schemaId' or 'credentialDefinitionId').  */
    @SerializedName("claims")
    val claims: kotlin.Any?,

    /*  The issuer Prism DID by which the verifiable credential will be issued. DID can be short for or long form.  */
    @SerializedName("issuingDID")
    val issuingDID: kotlin.String,

    /* The validity period in seconds of the verifiable credential that will be issued. */
    @SerializedName("validityPeriod")
    val validityPeriod: kotlin.Double? = null,

    @SerializedName("schemaId")
    @JsonAdapter(StringOrStringArrayAdapter::class)
    val schemaId: kotlin.collections.List<kotlin.String>? = null,

    /*  The unique identifier (UUID) of the credential definition that will be used for this offer. It should be the identifier of a credential definition that exists in the issuer agent's database. Note that this parameter only applies when the offer is of type 'AnonCreds'.  */
    @SerializedName("credentialDefinitionId")
    val credentialDefinitionId: java.util.UUID? = null,

    /* The credential format for this offer (defaults to 'JWT') */
    @SerializedName("credentialFormat")
    val credentialFormat: kotlin.String? = null,

    /*  Specifies whether or not the credential should be automatically generated and issued when receiving the `CredentialRequest` from the holder. If set to `false`, a manual approval by the issuer via another API call will be required for the VC to be issued.  */
    @SerializedName("automaticIssuance")
    val automaticIssuance: kotlin.Boolean? = null,

    /*  Specified the key ID (kid) of the DID, it will be used to sign credential. User should specify just the partial identifier of the key. The full id of the kid MUST be \"<issuingDID>#<kid>\" Note the cryto algorithm used with depend type of the key.  */
    @SerializedName("issuingKid")
    val issuingKid: kotlin.String? = null,

    /*  The unique identifier of a DIDComm connection that already exists between the this issuer agent and the holder cloud or edeg agent. It should be the identifier of a connection that exists in the issuer agent's database. This connection will be used to execute the issue credential protocol. Note: connectionId is only required when the offer is from existing connection. connectionId is not required when the offer is from invitation for connectionless issuance.  */
    @SerializedName("connectionId")
    val connectionId: java.util.UUID? = null,

    /*   A self-attested code the receiver may want to display to the user or use in automatically deciding what to do with the out-of-band message.  goalcode is optional and can be provided when the offer is from invitation for connectionless issuance.  */
    @SerializedName("goalCode")
    val goalCode: kotlin.String? = null,

    /*   A self-attested string that the receiver may want to display to the user about the context-specific goal of the out-of-band message.  goal is optional and can be provided when the offer is from invitation for connectionless issuance.  */
    @SerializedName("goal")
    val goal: kotlin.String? = null

//    @SerializedName("jwtVcPropertiesV1")
//    val jwtVcPropertiesV1: kotlin.collections.List<kotlin.String>? = null
//    @SerializedName("jwtVcPropertiesV1")
//    val anoncredsVcPropertiesV1: Option[AnonCredsVCPropertiesV1] = None
//    @SerializedName("sdJwtVcPropertiesV1")
//    val sdJwtVcPropertiesV1: Option[SDJWTVCPropertiesV1] = None
)

