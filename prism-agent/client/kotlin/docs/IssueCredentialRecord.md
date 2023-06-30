
# IssueCredentialRecord

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**claims** | **kotlin.collections.Map&lt;kotlin.String, kotlin.String&gt;** | The claims that will be associated with the issued verifiable credential. | 
**recordId** | **kotlin.String** | The unique identifier of the issue credential record. | 
**createdAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | The date and time when the issue credential record was created. | 
**role** | **kotlin.String** | The role played by the Prism agent in the credential issuance flow. | 
**protocolState** | **kotlin.String** | The current state of the issue credential protocol execution. | 
**subjectId** | **kotlin.String** | The identifier (e.g DID) of the subject to which the verifiable credential will be issued. |  [optional]
**validityPeriod** | **kotlin.Double** | The validity period in seconds of the verifiable credential that will be issued. |  [optional]
**automaticIssuance** | **kotlin.Boolean** | Specifies whether or not the credential should be automatically generated and issued when receiving the &#x60;CredentialRequest&#x60; from the holder. If set to &#x60;false&#x60;, a manual approval by the issuer via API call will be required for the VC to be issued. |  [optional]
**updatedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | The date and time when the issue credential record was last updated. |  [optional]
**jwtCredential** | **kotlin.String** | The base64-encoded JWT verifiable credential that has been sent by the issuer. |  [optional]
**issuingDID** | **kotlin.String** | Issuer DID of the verifiable credential object. |  [optional]



