# Present Proof Protocol

This Protocol is part of the **DIDComm Messaging Specification** but also **00454-present-proof-v2**

Its a Present Proof protocol based on DIDCOMMv2 message format.

A protocol supporting a general purpose verifiable presentation exchange regardless of the specifics of the underlying verifiable presentation request and verifiable presentation format

See [https://identity.foundation/didcomm-messaging/spec]
See [https://github.com/hyperledger/aries-rfcs/tree/main/features/0454-present-proof-v2]

## PIURI

Version 2.0: `https://didcomm.org/present-proof/3.0/propose-presentation`

Version .0: `https://didcomm.org/present-proof/3.0/request-presentation`

Version 3.0: `https://didcomm.org/present-proof/3.0/presentation`

### Roles

- Prover
  - Begin with a Propose Presentation Proof
- Verifier
  - Begin with a Request Presentation

### Prover received request presentation (Flow Diagram)

```mermaid
stateDiagram-v2
  [*] --> Initial
  Initial --> request_presentation_received:Presentation request received as DIDCOMMV2 message
  request_presentation_received --> proposal_sent: Presentation Proposal as DIDCOMMV2 message
  request_presentation_received --> signed_presentation_proof_sent:Presentation proof sent as DIDCOMMV2 message
  signed_presentation_proof_sent --> [*]
```

### Verifier request presentation sent  (Flow Diagram)

```mermaid
stateDiagram-v2
  [*] --> Initial
  Initial --> await_response:request_presentation_sent DIDCOMMV2 message
  await_response --> signed_presentation_proof_received:received DIDCOMMV2  signed proof received
  await_response --> propose_presentation_received:received DIDCOMMV2 presentation proposal received
  propose_presentation_received --> await_response:request_presentation_sent  DIDCOMMV2 request presentation proof
  signed_presentation_proof_received --> done
  await_response --> error:recieve problem report response
  done --> [*]
```



TODO See <https://github.com/hyperledger/aries-rfcs/blob/main/features/0453-issue-credential-v2/README.md>

### Holder State Machine

```mermaid
stateDiagram-v2
  state if_state_Proposal <<choice>>
  state if_state_Presentation <<choice>>
  state if_reveal_data <<choice>>
  state if_continue <<choice>>
  state if_ack_expected <<choice>>

  state Verifier_Request_Proof {

    [*] --> Send_Presentation_Request: Send Presentation Proof Request 
    Send_Presentation_Request --> Recive_Presentation_Proof : Receive Presentation Proof
    Send_Presentation_Request --> Recive_Presentation_Proposal : Receive Presentation Proposal
    Send_Presentation_Request --> verfier_problem_report: Receive Problem Report
    Recive_Presentation_Proposal --> if_state_Proposal : Is proposal valid
    if_state_Proposal --> Send_Presentation_Request: Yes
    if_state_Proposal --> verfier_problem_report: No, Sent problem report
    verfier_problem_report --> [*]
    
    Recive_Presentation_Proof --> Presentation_Received
    Presentation_Received --> if_state_Presentation : Is presentation valid
    if_state_Presentation --> Send_Ack_Presentation_Proof: Yes valid
    if_state_Presentation --> verfier_problem_report: Yes Invalid, sent problem report
    if_state_Presentation --> Ack_Expected?: No
    Ack_Expected? --> [*]
    Send_Ack_Presentation_Proof --> [*]
    verfier_problem_report --> [*]
  }
    
  state Prover_Proposal_Proof {     
   [*] --> Receive_Presentation_Request: Receive Presentation Proof Request
    Receive_Presentation_Request --> if_reveal_data:Do you want to reveal data? 
    if_reveal_data --> if_continue : No Do you want to continue?
    if_continue --> send_presentation_proposal :  Yes
    if_continue --> Problem_report : No,Sent problem report
    send_presentation_proposal --> Receive_Presentation_Request
    Problem_report --> [*]
    if_reveal_data --> send_presentation_proof : Yes
    send_presentation_proof --> if_ack_expected: sent
    if_ack_expected --> receive_ack_presentation: Yes Received Ack
    if_ack_expected --> Problem_report: Yes,Received problem report
    if_ack_expected --> [*] : No
    receive_ack_presentation --> [*]
  }


```
