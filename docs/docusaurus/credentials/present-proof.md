# Present proof

The [Present Proof Protocol](/docs/concepts/glossary#present-proof-protocol) allows:
- a [Verifier](/docs/concepts/glossary#verifier) to request a verifiable credential presentation from a Holder/Prover
- a [Holder/Prover](/docs/concepts/glossary#holder) responds by presenting a cryptographic proof to the Verifier

The protocol provides endpoints for a Verifier to request new proof presentations from Holder/Provers and for a Holder/Prover to respond to the presentation request using a specific verifiable credential they own.

## Roles

The present proof protocol has two roles:

1.  Verifier: A subject requesting a proof presentation by sending a request presentation message, then verifying the presentation.
2.  Holder/Prover: A [subject](/docs/concepts/glossary#subject) that receives a [proof presentation](/docs/concepts/glossary#proof-presentation) request, prepares a proof, and sends it to the verifier by sending a proof presentation message.

## Prerequisites

Before using the Proof Presentation protocol, the following conditions must be present:

1. Holder/Prover and Verifier PRISM Agents must be up and running
2. A connection must be established between the Holder/Prover and Verifier PRISM Agents (see [Connections](../connections/connection.md))
3. The Holder/Prover should hold a [verifiable credential (VC)](/docs/concepts/glossary#verifiable-credential) received from an [Issuer](/docs/concepts/glossary#issuer) see [Issue](./issue.md).

## Overview

This protocol supports the presentation of verifiable claims between two Atala PRISM Agents, the Holder/Prover and the Verifier.

The protocol consists of the following main parts:

1. The Verifier creates a new proof presentation request using the [`/present-proof/presentations`](/agent-api/#tag/Present-Proof/operation/requestPresentation) endpoint. It includes the identifier of an existing `connection` between both parties, `domain`, and a `challenge` to protect from potential replay attacks.
2. The Holder/Prover receives the presentation request from the Verifier and can retrieve the list of existing requests using the [`/present-proof/presentations`](/agent-api/#tag/Present-Proof/operation/getAllPresentation) endpoint.
3. The Holder/Prover can then review and accept a specific request using the [`/present-proof/presentations/{presentationId}`](/agent-api/#tag/Present-Proof/operation/updatePresentation) endpoint, providing the identifier of the `credential` record to use in the proof presentation.
4. The Verifier receives the proof presentation from the Holder/Prover and can accept it using the [`/present-proof/presentations/{presentationId}`](/agent-api/#tag/Present-Proof/operation/updatePresentation) endpoint, specifying `presentation-accept` as the action type.

## Endpoints

| Endpoint | Method | Description | Role |
| --- | --- | --- | --- |
| [`/present-proof/presentations`](/agent-api/#tag/Present-Proof/operation/requestPresentation) | POST | Creates and sends a new proof presentation request.  | Verifier |
| [`/present-proof/presentations`](/agent-api/#tag/Present-Proof/operation/getAllPresentation) | GET | Retrieves the collection of all the existing presentation proof records - sent or received. | Verifier, Holder/Prover |
| [`/present-proof/presentations/{id}`](/agent-api/#tag/Present-Proof/operation/getPresentation) | GET | Retrieves a specific presentation proof record by `id`. | Verifier, Holder/Prover |
| [`/present-proof/presentations/{id}`](/agent-api/#tag/Present-Proof/operation/updatePresentation) | PATCH | Updates an existing presentation proof record to, e.g., accept the request on the Holder/Prover side or accept the presentation on the Verifier side. | Verifier, Holder/Prover |

:::info
For more detailed information, please, check the full [PRISM Agent API](/agent-api).
:::

## Verifier interactions

This section describes the interactions available to the Verifier with the PRISM Agent.

### Creating and sending a Presentation Request

The Verifier needs to create a proof presentation request to start the process.
To do this, he makes a `POST` request to the [`/present-proof/presentations`](/agent-api/#tag/Present-Proof/operation/requestPresentation) endpoint with a JSON payload that includes the following information:

1. `connectionId`: This field represents the unique identifier of an existing connection between the verifier and the Holder/prover. It is for exchanging messages related to the protocol flow execution.
2. `challenge` and `domain`: The Verifier provides the random seed challenge and operational domain, and the Holder/Prover must sign the generated proof to protect from replay attacks.

```bash
curl -X 'POST' 'http://localhost:8070/prism-agent/present-proof/presentations' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
	-H "apikey: $API_KEY" \
  -d '{
        "connectionId": "872ddfa9-4115-46c2-8a1b-22c24c7431d7",
        "proofs":[],
        "options": {
          "challenge": "11c91493-01b3-4c4d-ac36-b336bab5bddf",
          "domain": "https://prism-verifier.com"
        }
      }'
```

Upon execution, a new presentation request record gets created with an initial state of `RequestPending`. The Verifier PRISM Agent will send the presentation request message to the PRISM Agent of the Holder/Prover through the specified DIDComm connection. The record state then is updated to `RequestSent`.

The Verifier can retrieve the list of presentation records by making a `GET` request to the [`/present-proof/presentations`](/agent-api/#tag/Present-Proof/operation/getAllPresentation) endpoint:
```bash
curl -X 'GET' 'http://localhost:8070/prism-agent/present-proof/presentations' \
  -H 'accept: application/json' \
	-H "apikey: $API_KEY"
```

### Accept presentation proof received from the Holder/prover
Once the Holder/Prover has received a proof presentation request, he can accept it using an appropriate verifiable credential. The PRISM Agent of the Verifier will receive that proof and verify it. Upon successful verification, the presentation record state gets updated to `PresentationVerified`.

The Verifier can then explicitly accept the specific verified proof presentation to change the record state to `PresentationAccepted` by making a `PATCH` request to the [`/present-proof/presentations/{id}`](/agent-api/#tag/Present-Proof/operation/updatePresentation) endpoint:

```bash
curl -X 'PATCH' 'http://localhost:8070/prism-agent/present-proof/presentations/{PRESENTATION_ID}' \
  -H 'Content-Type: application/json' \
  -H "apikey: $API_KEY" \
  -d '{
        "action": "presentation-accept"
      }'
```

```mermaid
---
title: Verifier Flow
---
stateDiagram-v2
  [*] --> RequestPending: new presentation request created by the Verifier
  RequestPending --> RequestSent: presentation request sent to the Holder/Prover PRISM Agent
  RequestSent --> PresentationReceived: presentation proof received from the Holder/Prover
  PresentationReceived --> PresentationVerified: presentation proof verified by the Verifier PRISM Agent
  PresentationVerified --> PresentationAccepted: verified presentation proof explicitly accepted by the Verifier
```

## Holder/Prover
This section describes the interactions available to the Holder/Prover with his PRISM Agent.

### Reviewing and accepting a received presentation request
The Holder/Prover can retrieve the list of presentation requests received by its PRISM Agent from different Verifiers making a `GET` request to the [`/present-proof/presentations`](/agent-api/#tag/Present-Proof/operation/getAllPresentation) endpoint:

```bash
curl -X 'GET' 'http://localhost:8090/prism-agent/present-proof/presentations' \
  -H 'accept: application/json' \
  -H "apikey: $API_KEY"
```

The Holder/Prover can then accept a specific request, generate the proof, and send it to the Verifier PRISM Agent by making a `PATCH` request to the [`/present-proof/presentations/{id}`](/agent-api/#tag/Present-Proof/operation/updatePresentation) endpoint:

```bash
curl -X 'PATCH' 'http://localhost:8090/prism-agent/present-proof/presentations/{PRESENTATION_ID}' \
  -H 'Content-Type: application/json' \
  -H "apikey: $API_KEY" \
  -d '{
        "action": "request-accept",
        "proofId": ["{CRED_RECORD_ID}"]
      }'
```

The Holder/Prover will have to provide the following information:
1. `presentationId`: The unique identifier of the presentation record to accept.
2. `proofId`: The unique identifier of the verifiable credential record to use as proof. 

The record state is updated to `PresentationPending` and processed by the Holder/Prover PRISM Agent. The agent will automatically generate the proof presentation, change the state to `PresentationGenerated`, and will eventually send it to the Verifier Agent, and change the state to `PresentationSent`.

```mermaid
---
title: Holder/Prover Flow
---
stateDiagram-v2
  [*] --> RequestReceived: presentation request received by the PRISM Agent
  RequestReceived --> PresentationPending: request accepted by the Holder/Prover
  PresentationPending --> PresentationGenerated: presentation proof generated by the PRISM Agent
  PresentationGenerated --> PresentationSent: generated proof sent to the Verifier PRISM Agent
```

## Sequence diagram

The following diagram shows the end-to-end flow for a verifier to request and verify a proof presentation from a Holder/prover. 

![](present-proof-flow.png)