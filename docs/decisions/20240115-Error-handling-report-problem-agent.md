# Error Handling Report Problem for Agent

- Status: accepted
- Deciders: Shailesh Patil, Fabio Pinheiro, Yurii Shynbuiev, Benjamin Voiturier, David Poltorak
- Date: 2024-01-16
- Tags: report, problem, error, handling, agent

Technical Story: [Error Handling Report Problem for Agent]

## Context and Problem Statement

In decentralized systems like those with remote collaborating agents, effectively reporting errors and warnings is a complex task. 
It's crucial to not only highlight problems but also provide the necessary context to those who need this information, 
which might include different groups: those who should be informed and those who can actually resolve the issues. 

It is more challenging when a problem is identified significantly later or in a different location from where it originated, 
involving collaboration among various parties for a solution.
For DIDComm, the aspect of interoperability is especially critical in the context of problem reporting. 
In DIDComm, an agent developed by one team is required to be adept at interpreting errors reported by an agent from a completely different team,
presenting a unique challenge in this area.

As of the time of writing, the cloud agent supports 3 DIDComm flows: `Connection`, `Issuance` and `Verification.`
Each cloud agent operates a background thread that facilitates interactions between two agents.
For each flow, the agent tracks a protocol state, and depending on this state, it triggers a DIDComm(V2) message to communicate with the other agent.
The communication between two agents, Agent A and Agent B, occurs asynchronously in the background job.
If an error occurs in this background job over DIDComm in Agent A, it is recorded in Agent A's logs. However, Agent B remains uninformed about any such errors.

## Decision Drivers <!-- optional -->

What are our needs? Let’s try to sum up the required capabilities based on 
[Report Problem 2.0](https://identity.foundation/didcomm-messaging/spec/#problem-reports), we need:


The cloud agent is designed to perform three distinct roles: `Issuer`, `Holder`, and `Verifier`. Within these roles, 
it operates across three protocol flows, namely `Connection`, `Issuance`, and `Verification`.

| Agent(Protocol) Flows       | Protocols                                                                                          |
|-----------------------------|----------------------------------------------------------------------------------------------------|
| Connection                  | https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol              |
| Issuance                    | https://github.com/decentralized-identity/waci-didcomm/tree/main/issue_credential                  |
| Verification(Present proof) | https://github.com/decentralized-identity/waci-didcomm/blob/main/present_proof/present-proof-v3.md |

  Custom Behavior table
  This table defines the expected behavior of the Agent in different scenarios not covered by the specifications.

 | Agent            | Behaviour                      | Action            |
 |------------------|--------------------------------|---------------------|
 | Scenario G1	     | Send a problem report          | e.p.msg.unsupported |
 | Scenario G2      | Send a problem report          | e.p.msg.unsupported | 
 | Scenario G3      | Send a problem report          | e.p.error           | 
 | Scenario G4      | (Sync ?) Send a problem report | e.p.trust.crypto    | 
 | Scenario G5      | (Sync ?) Send a problem report | e.p.did             | 
 | Scenario G6      | (Sync ?) Send a problem report | e.p.did.malformed   | 
 | Scenario G7      | Send a problem report          | e.p.msg.<PIURI>     | 

### Scenarios Description

#### General Scenarios:

- **G1** - Receive a message for an unsupported protocol
 - G1 - Send a problem report `e.p.msg.unsupported`

- **G2** - Receive a message for an unsupported version of the protocol.
 - G2 - Send a problem report `e.p.msg.unsupported` and say what version(s) its supported

- **G3** - When an internal error or any unexpected error happens.
 - G3 - Send a problem report "e.p.error"

- **G4** - If the message is tampered or got any crypto errors when decoding.
 - G4 -  (sync!) Send a problem report "e.p.trust.crypto"

- **G5** - If the DID method is not supported (`did.peer` in this case)
 - G5 - (sync?) e.p.did

- **G6** - If the DID method is malformed.
 - G6 - (sync?) e.p.did.malformed

- **G7** - When a parsing error from the decrypted message.
 - G7 - Send a problem report `e.p.msg.<PIURI>` if the plaintext message is malformed or if parsing into a specific protocol's data model fails.

##  Connection Flow Scenarios
https://github.com/hyperledger/aries-rfcs/tree/main/features/0160-connection-protocol              

| Agent             | Behaviour                                           | Action                       |
|-------------------|-----------------------------------------------------|------------------------------|
| Scenario C1	      | Send a problem report (Invitation expired)          | e.p.msg.invitation-expired   |
| Scenario C2       | Send a problem report (Invitation parsing decoding) | e.p.msg.malformed-invitation |
| Scenario C3       | Send a problem report (DB connection issues)        | e.p.me.res.storage           |
| Scenario C4       | Send a problem report (After max retries)           | e.p.req.max-retries-exceeded |
| Scenario C5 (G3)  | Send a problem report Any other error               | e.p.error                    |

- **C1** - OOB Invitation has expired
- **C2** - OOB is tampered / decoding error 
- **C3** - Database connection or related issue
- **C4** - Max retries (Cascading Problems): Connection state cannot be moved after max retries
- **C5** - See G3


##  Issuance Flow Scenarios
https://github.com/decentralized-identity/waci-didcomm/tree/main/issue_credential

| Agent            | Behaviour                                      | Action                             |
|------------------|------------------------------------------------|------------------------------------|
| Scenario I1	     | Send a problem report                          | e.p.msg.credential-format-mismatch |
| Scenario I2	     | Send a problem report                          | e.p.msg.invalid-signature          |
| Scenario I3	     | Send a problem report                          | e.p.msg.schema-mismatch            |
| Scenario I4      | Send a problem report (DB connection issues)   | e.p.me.res.storage                 |
| Scenario I5      | Send a problem report (After max retries)      | e.p.req.max-retries-exceeded       |
| Scenario I6(G3)  | Send a problem report Any other error          | e.p.error                          |

- **I1** - Credential format mismatch: The Holder expects a credential format schema, but the credential issued is different format
- **I2** - Credential signature that cannot be verified. This might be due to the credential being tampered with, or the public key used for signing being incorrect or expired.
- **I3** - Schema Mismatch: The Holder expects a credential that adheres to a certain schema, but the credential presented follows a different schema.
- **I4** - Database connection or related issue
- **I5** - Max retries (Cascading Problems): Issuance state cannot be moved after max retries
- **I6** - See G3


##  Verification(Present proof)  Flow Scenarios
https://github.com/decentralized-identity/waci-didcomm/blob/main/present_proof/present-proof-v3.md

| Agent           | Behaviour                                    | Action                                |
|-----------------|----------------------------------------------|---------------------------------------|
| Scenario V1	    | Send a problem report                        | e.p.msg.credential-format-mismatch    |
| Scenario V2	    | Send a problem report                        | e.p.msg.invalid-signature             |
| Scenario V3	    | Send a problem report                        | e.p.msg.revoked-credentials           |
| Scenario V4	    | Send a problem report                        | e.p.msg.expired-credentials           |
| Scenario V5	    | Send a problem report                        | e.p.msg.proof-mismatch                |
| Scenario V6	    | Send a problem report                        | e.p.msg.schema-mismatch               |
| Scenario V7	    | Send a problem report                        | e.p.msg.revoked-or-expired-issuer-key |
| Scenario V8     | Send a problem report (After max retries)    | e.p.req.max-retries-exceeded          |
| Scenario V9(G3) | Send a problem report Any other error        | e.p.error                             |
| Scenario V10    | Send a problem report (DB connection issues) | e.p.me.res.storage                    |

- **V1** - Credential Format Mismatch: The verifier expects a credential in a specific format, but the prover presents it in a different format.
- **V2** - Credentials presented have signatures that cannot be verified. This might be due to the credential being tampered with, or the public key used for verification being incorrect.
- **V3** - Revoked Credentials: The prover presents a credential that has been revoked by the issuer
- **V4** - Expired Credentials: The credentials presented are past their expiry date, making them invalid for the transaction.
- **V5** - Mismatch in the Proof Request and Presentation: The verifier's proof request asks for certain attributes or predicates, but the presentation from the prover doesn't match these requirements.
- **V6** - Schema Mismatch: The verifier expects a credential that adheres to a certain schema, but the credential presented follows a different schema.
- **V7** - Credential Format Mismatch: The verifier expects a credential in a specific format, but the prover presents it in a different format.
- **V8** - Max retries (Cascading Problems): Verification State cannot be moved after max retries
- **V9** - See G3
- **V10** - Database connection or related issue

## Decision Outcome

In the event of an issue in a cloud agent, the following actions are taken:

1. The error is logged, including the X-Request-ID and the thread ID (thid).

2. A problem report message is generated and sent out as outlined in the previously mentioned table
  This message is sent in accordance with the return route defined here: 
   [Return Route Extension for DIDComm Messaging](https://github.com/decentralized-identity/didcomm-messaging/blob/main/extensions/return_route/main.md).
3. [Implement the Problem Reporting](https://didcomm.org/report-problem/2.0/)

### Out of the Scope

1. The problem report generated is not stored in the database along with the associated X-Request-ID.

2. [Replying to Warnings](https://identity.foundation/didcomm-messaging/spec/#replying-to-warnings) 

3. [ACKs](https://identity.foundation/didcomm-messaging/spec/#acks) 


