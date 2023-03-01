# Publish DID

PRISM DID creation involves generating key pairs and some additional data (e.g. services) to construct a create-operation.
The create-operation allows **DID Controller** to derive the DID and there are two types of DID that can be derived:

1. [Long-form DID](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#long-form-dids-unpublished-dids). It has the following format `did:prism:<initial-hash>:<encoded-state>`
2. Short-form DID. It has the following format `did:prism:<initial-hash>`

The difference is the long-form DID contains the encoded create-operation in the DID itself while the short-form DID doesn't have this information.
As a result, when resolving the short-form DID, the resolver must look for additional data from the blockchain.
On the other hand, resolving long-form DID is self-contained.
Even if the data is not on the blockchain, the resolver can still work out the DID document of the long-form DID.

Enabling the resolution of short-form DID can be done by DID publication which is a process of putting the create-operation on the blockchain.


## Roles

1. **DID Controller** is the organization or individual who has control of the DID.

## Prerequisites

1. **DID Controller** PRISM Agents up and running
2. **DID Controller** has a DID created on PRISM Agent (see [Create DID](./create.md))

## Overview

Publishing a DID requires the DID create-operation and the `MASTER` key pairs which PRISM Agent already created under the hood when the DID was created on PRISM Agent.
When the **DID Controller** requests a publication of their DID, PRISM Agent will use the `MASTER` key to sign the operation and submit the signed operation to the blockchain.
Once the operation is submitted to the blockchain, the **DID Controller** will have to wait for a specific number of confirmation blocks before the DID operation is processed.
(see [PRISM DID method - Processing of DID operation](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md#processing-of-operations))

## Endpoints

The example uses the following endpoints

| Endpoint                                                                                                      | Description                                             | Role           |
|---------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|----------------|
| [`GET /did-registrar/dids/{didRef}`](/agent-api/#tag/DID-Registrar/operation/getManagedDid)                   | Get the DID stored in PRISM Agent                       | DID Controller |
| [`POST /did-registrar/dids/{didRef}/publications`](/agent-api/#tag/DID-Registrar/operation/publishManagedDid) | Publish the DID stored in PRISM Agent to the blockchain | DID Controller |
| [`GET /dids/{didRef}`](/agent-api/#tag/DID/operation/getDid)                                                  | Resolve a DID to DID document                           | DID Controller |

## DID Controller interactions
