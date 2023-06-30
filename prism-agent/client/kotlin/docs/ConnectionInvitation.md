
# ConnectionInvitation

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | [**java.util.UUID**](java.util.UUID.md) | The unique identifier of the invitation. It should be used as parent thread ID (pthid) for the Connection Request message that follows. | 
**type** | **kotlin.String** | The DIDComm Message Type URI (MTURI) the invitation message complies with. | 
**from** | **kotlin.String** | The DID representing the sender to be used by recipients for future interactions. | 
**invitationUrl** | **kotlin.String** | The invitation message encoded as a URL. This URL follows the Out of [Band 2.0 protocol](https://identity.foundation/didcomm-messaging/spec/v2.0/#out-of-band-messages) and can be used to generate a QR code for example. | 



