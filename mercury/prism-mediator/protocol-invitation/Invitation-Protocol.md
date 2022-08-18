# Invitation Protocol

This Protocol is parte of the DIDComm Messaging Specification.

Its a out-of-band style protocol.

See [https://identity.foundation/didcomm-messaging/spec/#invitation]

## PIURI

`https://didcomm.org/out-of-band/2.0/invitation`

### Flow Diagram TODO

```mermaid
stateDiagram-v2
  [*] --> Invited:Send Invititation(has expiry)
  Invited --> Requested:Recieve connection request
  Requested --> Responded:Send Connection Response
  Responded --> Invited: Send Connection Error Retry if possible stay in same state
  Responded --> Completed:Recieve Acknowledgement
  Completed --> Responded:Recieve Acknowledgement Error Retry if possible stay in same state
  Completed --> [*] 
```

```mermaid
stateDiagram-v2
  [*] --> Invited:Recieve Invititation 
  Invited --> Requested: Send Connection Request
  Requested --> Responded: Recieve Connection Response
  Responded --> Invited: Send Connection Error Response Retry if possible stay in same state
  Responded --> Completed: Send Acknowledgement
  Completed --> Responded: Recieve Acknowledgement Error Retry if possible stay in same sate
  Completed --> [*] 
```
