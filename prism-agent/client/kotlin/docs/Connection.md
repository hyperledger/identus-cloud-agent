
# Connection

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**connectionId** | [**java.util.UUID**](java.util.UUID.md) | The unique identifier of the connection. | 
**role** | [**inline**](#Role) | The role played by the Prism agent in the connection flow. | 
**state** | [**inline**](#State) | The current state of the connection protocol execution. | 
**invitation** | [**ConnectionInvitation**](ConnectionInvitation.md) |  | 
**createdAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | The date and time the connection record was created. | 
**self** | **kotlin.String** | The reference to the connection resource. | 
**kind** | **kotlin.String** | The type of object returned. In this case a &#x60;Connection&#x60;. | 
**label** | **kotlin.String** | A human readable alias for the connection. |  [optional]
**myDid** | **kotlin.String** | The DID representing me as the inviter or invitee in this specific connection. |  [optional]
**theirDid** | **kotlin.String** | The DID representing the other peer as the an inviter or invitee in this specific connection. |  [optional]
**updatedAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | The date and time the connection record was last updated. |  [optional]


<a id="Role"></a>
## Enum: role
Name | Value
---- | -----
role | Inviter, Invitee


<a id="State"></a>
## Enum: state
Name | Value
---- | -----
state | InvitationGenerated, InvitationReceived, ConnectionRequestPending, ConnectionRequestSent, ConnectionRequestReceived, ConnectionResponsePending, ConnectionResponseSent, ConnectionResponseReceived, ProblemReportPending, ProblemReportSent, ProblemReportReceived



