
# CreateIssueCredentialRecordRequest

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**claims** | **kotlin.collections.Map&lt;kotlin.String, kotlin.String&gt;** | The claims that will be associated with the issued verifiable credential. | 
**issuingDID** | **kotlin.String** | The issuer DID of the verifiable credential object. | 
**connectionId** | **kotlin.String** | The unique identifier of a DIDComm connection that already exists between the issuer and the holder, and that will be used to execute the issue credential protocol. | 
**validityPeriod** | **kotlin.Double** | The validity period in seconds of the verifiable credential that will be issued. |  [optional]
**automaticIssuance** | **kotlin.Boolean** | Specifies whether or not the credential should be automatically generated and issued when receiving the &#x60;CredentialRequest&#x60; from the holder. If set to &#x60;false&#x60;, a manual approval by the issuer via API call will be required for the VC to be issued. |  [optional]



