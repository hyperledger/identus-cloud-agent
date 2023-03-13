# Deactivate DID

DID deactivation is an important feature of SSI systems that gives greater control for managing digital identity.
DID deactivation can come in handy if the DID is compromised or no longer used.
This is crucial for the security and risk management of identity owners.

Similar to [DID update](./update.md), the deactivation of a PRISM DID is a process of putting deactivate-operation on the blockchain so that other participants know that the DID is no longer active.
The PRISM DID method only allows published DID to be updated.

## Roles

1. **DID Controller** is the organization or individual who has control of the DID.

## Prerequisites

1. **DID Controller** PRISM Agent up and running
2. **DID Controller** has a DID created on PRISM Agent (see [Create DID](./create.md))
3. **DID Controller** has a DID published to the blockchain (see [Publish DID](./publish.md))

## Overview

DID deactivation operates similarly to the DID update where deactivate-operation is published to the blockchain
and some confirmation blocks must be created before it is considered deactivated by other participants.
Once the DID is deactivated, all content in the DID document is emptied and no operation will have any effect on the DID afterward.
The same concept also holds for PRISM DID deactivation in that if any subsequent operation is made before the operation is confirmed, the fork can occur.
Please refer to the `SECURE_DEPTH` parameter in [PRISM method - protocol parameters](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#versioning-and-protocol-parameters) for the number of confirmation blocks.
At the time of writing, this number is 112 blocks.

PRISM Agent allows DID deactivation to be easily performed.
Under the hood, PRISM Agent uses the `MASTER` keys to sign the intended operation and automatically post the operation to the blockchain.
This example shows the DID deactivation and steps to observe the changes to the DID using PRISM Agent.

## Endpoints

The example uses the following endpoints

| Endpoint                                                                                                          | Description                   | Role           |
|-------------------------------------------------------------------------------------------------------------------|-------------------------------|----------------|
| [`POST /did-registrar/dids/{didRef}/deactivations`](/agent-api/#tag/DID-Registrar/operation/deactivateManagedDid) | Deactivate a PRISM DID        | DID Controller |
| [`GET /dids/{didRef}`](/agent-api/#tag/DID/operation/getDid)                                                      | Resolve a DID to DID document | DID Controller |

## DID Controller interactions

### 1. Check the current state of the DID document

Given the **DID Controller** has a DID on PRISM Agent and that DID is published, he can resolve the DID document using short-form DID.

```bash
curl --location --request GET 'http://localhost:8080/prism-agent/dids/{didRef}' \
--header 'Accept: application/json'
```

Example DID document response (some fields are omitted for readability)

```json
{
    "did": {...},
    "metadata": {
        "canonicalId": "did:prism:66e431434f201c7ae43f6e63569f1ee556d7dfbee1646101547324013e545d2c",
        "deactivated": false,
        ...
    }
}
```
The DID metadata shows the `deactivation` status as `false` meaning that this DID is still active.

### 2. Requesting the DID deactivation to PRSIM Agent

The active status is observed from the last step.
The DID deactivation can be performed by calling `POST /did-registrar/dids/{didRef}/deactivations` and replacing `{didRef}` with the DID to deactivate.

```bash
curl --location --request POST 'http://localhost:8080/prism-agent/did-registrar/dids/{didRef}/deactivations' \
--header 'Accept: application/json'
```

Under the hood, PRISM Agent constructs the DID deactivate-operation from the *last confirmed operation* observed on the blockchain at that time.
The **DID Controller** should receive a response about the operation that has been scheduled, waiting for confirmation on the blockchain.
If this deactivate-operation gets confirmed on the blockchain and not discarded as a fork, the DID becomes deactivated.

### 3. Wait for the confirmation and observe the change on the DID metadata

When the **DID Controller** tries to resolve the DID again using the example in step 1,
the content might still be the same because the operation is not yet confirmed and applied.

The **DID Controller** keeps polling this endpoint until the `deactivated` status in DID document metadata is changed to `true`.

Example response of deactivated DID document (some fields are omitted for readability)

```json
{
    "did": {
        "assertionMethod": [],
        "authentication": [],
        ...
    },
    "metadata": {
        "canonicalId": "did:prism:66e431434f201c7ae43f6e63569f1ee556d7dfbee1646101547324013e545d2c",
        "deactivated": true,
        ...
    }
}
```

The DID metadata indicates that the DID is already deactivated and the DID document no longer contains any keys or services.
