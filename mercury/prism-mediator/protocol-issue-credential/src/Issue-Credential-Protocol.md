# Issue Credential Protocol

This Protocol is part of the **DIDComm Messaging Specification** but also **0453-issue-credential-v2**

Its a Issue Credential protocol based on DIDCOMMv2 message format.

A standard protocol for issuing credentials. This is the basis of interoperability between Issuers and Holders.

See [https://identity.foundation/didcomm-messaging/spec]
See [https://github.com/hyperledger/aries-rfcs/tree/main/features/0453-issue-credential-v2]

## PIURI

Version 1.0: `https://didcomm.org/issue-credential/1.0/propose-credential`


### Roles

- Issuer
  - Begin with a offer credential
- Holder
  - Begin with a proposal credential
  - Begin with a request credential

  


### Issuer received credential proposal (Flow Diagram)
```mermaid
stateDiagram-v2
  [*] --> Initial
  Initial --> proposal_received:credential proposal received as DIDCOMMV2 message
  proposal_received --> offer_sent:credential-offer-sent as DIDCOMMV2 message
  proposal_received --> error:send problem report response
  offer_sent --> [*]:follow credential sent offer flow
```
### Issuer Send offer credential  (Flow Diagram)

```mermaid
stateDiagram-v2
  [*] --> Initial
  Initial --> await_response:credential-offer-sent DIDCOMMV2 message
  await_response --> request_received:received DIDCOMMV2 credential request
  request_received --> credential_issued:send DIDCOMMV2 issue credential message
  credential_issued --> done
  await_response --> error:recieve problem report response
  done --> [*]
```


### Holder proposal credential (Flow Diagram)

```mermaid
stateDiagram-v2
  [*] --> Initial
  Initial --> await_response:credential-proposal-sent DIDCOMMV2 message
  await_response --> offer_received:recieved credential offer DIDCOMMV2 message
  offer_received --> [*]:follow credential request flow for Holder
  await_response --> error:recieve problem report response
```

### Holder request credential (Flow Diagram)

```mermaid
stateDiagram-v2
  [*] --> Initial
  Initial --> offer_received:recieved credential offer DIDCOMMV2 message
  offer_received --> request_sent: send DIDCOMMV2 credential request
  request_sent --> credential_received:received DIDCOMMV2 issue credentia message
  credential_received --> done
  offer_received --> error:send problem report response
  done --> [*]
```

