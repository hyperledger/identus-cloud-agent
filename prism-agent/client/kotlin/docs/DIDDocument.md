
# DIDDocument

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **kotlin.String** | [DID subject](https://www.w3.org/TR/did-core/#did-subject). The value must match the DID that was given to the resolver. | 
**atContext** | **kotlin.collections.List&lt;kotlin.String&gt;** | The JSON-LD context for the DID resolution result. |  [optional]
**controller** | **kotlin.String** | [DID controller](https://www.w3.org/TR/did-core/#did-controller) |  [optional]
**verificationMethod** | [**kotlin.collections.List&lt;VerificationMethod&gt;**](VerificationMethod.md) |  |  [optional]
**authentication** | **kotlin.collections.List&lt;kotlin.String&gt;** |  |  [optional]
**assertionMethod** | **kotlin.collections.List&lt;kotlin.String&gt;** |  |  [optional]
**keyAgreement** | **kotlin.collections.List&lt;kotlin.String&gt;** |  |  [optional]
**capabilityInvocation** | **kotlin.collections.List&lt;kotlin.String&gt;** |  |  [optional]
**capabilityDelegation** | **kotlin.collections.List&lt;kotlin.String&gt;** |  |  [optional]
**service** | [**kotlin.collections.List&lt;Service&gt;**](Service.md) |  |  [optional]



