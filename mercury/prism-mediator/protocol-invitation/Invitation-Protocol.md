# Invitation Protocol

This Protocol is part of the DIDComm Messaging Specification.

Its a out-of-band style protocol.

See [https://identity.foundation/didcomm-messaging/spec/#invitation]
See [https://github.com/hyperledger/aries-rfcs/blob/main/features/0434-outofband/README.md]

## PIURI

`https://didcomm.org/out-of-band/2.0/invitation`

### Roles

- Sender
  - Will create the message `https://didcomm.org/out-of-band/2.0/invitation`
- Receiver
  - Will accept the invitation

### Notes

- Invitation has expiry date

### Sender create invitation message  (Flow Diagram)

```mermaid
stateDiagram-v2
  [*] --> Initial
  Initial --> await_response:Send out-of-band invitation message
  await_response --> done:receive DIDCOMM response message(single use)
  await_response --> await_response:recieve DIDCOMM response message(multi use message)
  await_response --> error:recieve problem report response
  done --> [*]
```

### Receiver accepting invitation (Flow Diagram)

```mermaid
stateDiagram-v2
  [*] --> Initial
  Initial --> prepare_response:recieve out-of-band invitation message
  prepare_response --> done:send DIDCOMM response message
  done --> [*]
```
