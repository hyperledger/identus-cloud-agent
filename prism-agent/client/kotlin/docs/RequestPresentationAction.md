
# RequestPresentationAction

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**action** | [**inline**](#Action) | The action to perform on the proof presentation record. | 
**proofId** | **kotlin.collections.List&lt;kotlin.String&gt;** | The unique identifier of the issue credential record - and hence VC - to use as the prover accepts the presentation request. Only applicable on the prover side when the action is &#x60;request-accept&#x60;. |  [optional]


<a id="Action"></a>
## Enum: action
Name | Value
---- | -----
action | request-accept, request-reject, presentation-accept, presentation-reject



