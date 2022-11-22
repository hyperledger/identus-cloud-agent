# Connection Protocol

This Protocol is for DID base connection 


The protocol is used when you wish to create a connection with another agent.


## PIURI

Version 1.0: `https://atalaprism.io/mercury/connections/1.0/request`

Version 1.0: `https://atalaprism.io/mercury/connections/1.0/response`

### Roles

- Inviter
  - Will create the message `https://didcomm.org/out-of-band/2.0/invitation`
  - will accept the Connection request and create new did peer and reply Connection response
- Invitee
  - Will accept the invitation
  - Will create a did peer and reply to the Invitee with Connection Request

### Notes



### Inviter create invitation message for connection  (Flow Diagram)

```mermaid
stateDiagram-v2
  [*] --> Initial
  Initial --> await_response:Send out-of-band invitation message
  await_response --> connection_request:receive DIDCOMM Connection Request message
  connection_request --> connection_response:send DIDCOMM Connection Response message
  connection_response --> done
  await_response --> error:recieve problem report response
  done --> [*]
```

### Invitee accepting invitation message for connection (Flow Diagram)

```mermaid
stateDiagram-v2
  [*] --> Initial: out-of-band invitation connection message
  Initial --> connection_request:Create Connection Request
  connection_request --> await_response: Send Connection Request DIDCOMM message
  await_response --> connection_response:receive DIDCOMM Connection Response message
  await_response --> error:recieve problem report response
  connection_response --> done:send ACK
  done --> [*]
```
