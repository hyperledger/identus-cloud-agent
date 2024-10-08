/**
 * Identus Cloud Agent API Reference
 *  The Identus Cloud Agent API facilitates the integration and management of self-sovereign identity capabilities within applications. It supports DID (Decentralized Identifiers) management, verifiable credential exchange, and secure messaging based on DIDComm standards. The API is designed to be interoperable with various blockchain and DLT (Distributed Ledger Technology) platforms, ensuring wide compatibility and flexibility. Key features include connection management, credential issuance and verification, and secure, privacy-preserving communication between entities. Additional information and the full list of capabilities can be found in the [Open Enterprise Agent documentation](https://docs.atalaprism.io/docs/category/prism-cloud-agent) 
 *
 * OpenAPI spec version: 1.39.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { HttpFile } from '../http/http';

export class CreateIssueCredentialRecordRequest {
    /**
    * The validity period in seconds of the verifiable credential that will be issued.
    */
    'validityPeriod'?: number;
    'schemaId'?: string | Array<string>;
    /**
    *  The unique identifier (UUID) of the credential definition that will be used for this offer. It should be the identifier of a credential definition that exists in the issuer agent\'s database. Note that this parameter only applies when the offer is of type \'AnonCreds\'. 
    */
    'credentialDefinitionId'?: string;
    /**
    * The credential format for this offer (defaults to \'JWT\')
    */
    'credentialFormat'?: string;
    /**
    *  The set of claims that will be included in the issued credential. The JSON object should comply with the schema applicable for this offer (i.e. \'schemaId\' or \'credentialDefinitionId\'). 
    */
    'claims': any | null;
    /**
    *  Specifies whether or not the credential should be automatically generated and issued when receiving the `CredentialRequest` from the holder. If set to `false`, a manual approval by the issuer via another API call will be required for the VC to be issued. 
    */
    'automaticIssuance'?: boolean;
    /**
    *  The issuer Prism DID by which the verifiable credential will be issued. DID can be short for or long form. 
    */
    'issuingDID': string;
    /**
    *  Specified the key ID (kid) of the DID, it will be used to sign credential. User should specify just the partial identifier of the key. The full id of the kid MUST be \"<issuingDID>#<kid>\" Note the cryto algorithm used with depend type of the key. 
    */
    'issuingKid'?: string;
    /**
    *  The unique identifier of a DIDComm connection that already exists between the this issuer agent and the holder cloud or edeg agent. It should be the identifier of a connection that exists in the issuer agent\'s database. This connection will be used to execute the issue credential protocol. Note: connectionId is only required when the offer is from existing connection. connectionId is not required when the offer is from invitation for connectionless issuance. 
    */
    'connectionId'?: string;
    /**
    *   A self-attested code the receiver may want to display to the user or use in automatically deciding what to do with the out-of-band message.  goalcode is optional and can be provided when the offer is from invitation for connectionless issuance. 
    */
    'goalCode'?: string;
    /**
    *   A self-attested string that the receiver may want to display to the user about the context-specific goal of the out-of-band message.  goal is optional and can be provided when the offer is from invitation for connectionless issuance. 
    */
    'goal'?: string;

    static readonly discriminator: string | undefined = undefined;

    static readonly attributeTypeMap: Array<{name: string, baseName: string, type: string, format: string}> = [
        {
            "name": "validityPeriod",
            "baseName": "validityPeriod",
            "type": "number",
            "format": "double"
        },
        {
            "name": "schemaId",
            "baseName": "schemaId",
            "type": "CreateIssueCredentialRecordRequestSchemaId",
            "format": ""
        },
        {
            "name": "credentialDefinitionId",
            "baseName": "credentialDefinitionId",
            "type": "string",
            "format": "uuid"
        },
        {
            "name": "credentialFormat",
            "baseName": "credentialFormat",
            "type": "string",
            "format": ""
        },
        {
            "name": "claims",
            "baseName": "claims",
            "type": "any",
            "format": ""
        },
        {
            "name": "automaticIssuance",
            "baseName": "automaticIssuance",
            "type": "boolean",
            "format": ""
        },
        {
            "name": "issuingDID",
            "baseName": "issuingDID",
            "type": "string",
            "format": ""
        },
        {
            "name": "issuingKid",
            "baseName": "issuingKid",
            "type": "string",
            "format": ""
        },
        {
            "name": "connectionId",
            "baseName": "connectionId",
            "type": "string",
            "format": "uuid"
        },
        {
            "name": "goalCode",
            "baseName": "goalCode",
            "type": "string",
            "format": ""
        },
        {
            "name": "goal",
            "baseName": "goal",
            "type": "string",
            "format": ""
        }    ];

    static getAttributeTypeMap() {
        return CreateIssueCredentialRecordRequest.attributeTypeMap;
    }

    public constructor() {
    }
}

