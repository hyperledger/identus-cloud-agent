
# PresentationStatus

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**presentationId** | **kotlin.String** | The unique identifier of the presentation record. | 
**status** | [**inline**](#Status) | The current state of the proof presentation record. | 
**proofs** | [**kotlin.collections.List&lt;ProofRequestAux&gt;**](ProofRequestAux.md) | The type of proofs requested in the context of this proof presentation request (e.g., VC schema, trusted issuers, etc.) |  [optional]
**&#x60;data&#x60;** | **kotlin.collections.List&lt;kotlin.String&gt;** | The list of proofs presented by the prover to the verifier. |  [optional]
**connectionId** | **kotlin.String** | The unique identifier of an established connection between the verifier and the prover. |  [optional]


<a id="Status"></a>
## Enum: status
Name | Value
---- | -----
status | RequestPending, RequestSent, RequestReceived, RequestRejected, PresentationPending, PresentationGenerated, PresentationSent, PresentationReceived, PresentationVerified, PresentationAccepted, PresentationRejected, ProblemReportPending, ProblemReportSent, ProblemReportReceived



