# Create DID

PRISM DIDs are a type of _decentralized identifier_ used across Atala PRISM product suites.

It is a variation of a [_sidetree protocol_](https://identity.foundation/sidetree/spec/) and uses the Cardano blockchain as the underlying ledger for DID resolution and operation publication.
Please refere to [PRISM method specification](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md) for more detailed explanation of how it works.

A PRISM DID can be created entirely offline without interacting with the blockchain by constructing a DID create-operation, a protobuf message with a set of public keys and services.
Once the create-operation gets constructed, deriving a DID from this operation is possible, which is well-defined by the [PRISM DID method](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md).

## Roles

1. **DID Controller** is the organization or individual who has control of the DID.

## Prerequisites

1. **DID Controller** PRISM Agents up and running

## Overview

For this example, a PRISM DID gets created and stored inside PRISM Agent along with the private keys. It is not automatically published.
The Agent will keep track of private keys used for the create-operation and the content of the operation itself.

PRISM Agent provides two endpoint groups to facilitate PRISM DID usage.

- `/dids/*`
are facilitators of low-level interactions between DID operations and the blockchain.
The DID controllers will handle key management independently and use these endpoints for blockchain interaction.

- `/did-registrar/*`
Facilitates a higher-level interaction with PRISM DID, where the PRISM Agent handles key management concerns.

## Endpoints

The example uses the following endpoints

| Endpoint                                                                               | Description                                         | Role           |
|----------------------------------------------------------------------------------------|-----------------------------------------------------|----------------|
| [`GET /did-registrar/dids`](/agent-api/#tag/DID-Registrar/operation/listManagedDid)    | List all DIDs stored in PRISM Agent                 | DID Controller |
| [`POST /did-registrar/dids`](/agent-api/#tag/DID-Registrar/operation/createManagedDid) | Create a new PRISM DID to be managed by PRISM Agent | DID Controller |
| [`GET /dids/{didRef}`](/agent-api/#tag/DID/operation/getDid)                           | Resolve a DID to DID document                       | DID Controller |

## DID Controller interactions

### 1. Check existing DID on the PRISM Agent

```bash
curl --location --request GET 'http://localhost:8080/prism-agent/did-registrar/dids' \
  --header 'Accept: application/json'
```
The result should show an empty list, as no DIDs exist on this PRISM Agent instance.

### 2. Create a PRISM Agent managed DID using DID registrar endpoint

The DID controller can create a new DID by sending a DID document template to the Agent.
Since key pairs are generated and managed by PRISM Agent, DID controller only has to specify the key `id` and its purpose (e.g., `authentication`, `assertionMethod`, etc.).
The current PRISM DID method supports a key with a single purpose, but it may be extended to support a key with multitple purposes in the future.

```bash
curl --location --request POST 'http://localhost:8080/prism-agent/did-registrar/dids' \
  --header 'Content-Type: application/json' \
  --header 'Accept: application/json' \
  --data-raw '{
    "documentTemplate": {
      "publicKeys": [
        {
          "id": "auth-1",
          "purpose": "authentication"
        }
      ],
      "services": []
    }
  }'
```

The response should look like

```json
{
    "longFormDid": "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff:Cr4BCrsBElsKBmF1dGgtMRAEQk8KCXNlY3AyNTZrMRIg0opTuxu-zt6aRbT1tPniG4eu4CYsQPM3rrLzvzNiNgwaIIFTnyT2N4U7qCQ78qtWC3-p0el6Hvv8qxG5uuEw-WgMElwKB21hc3RlcjAQAUJPCglzZWNwMjU2azESIKhBU0eCOO6Vinz_8vhtFSAhYYqrkEXC8PHGxkuIUev8GiAydFHLXb7c22A1Uj_PR21NZp6BCDQqNq2xd244txRgsQ"
}
```

### 3. List the created DID

Checking the `GET /did-registrar/dids` endpoint, the response should return a list containing one DID.

```json
{
    "contents": [
        {
            "did": "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff",
            "longFormDid": "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff:Cr4BCrsBElsKBmF1dGgtMRAEQk8KCXNlY3AyNTZrMRIg0opTuxu-zt6aRbT1tPniG4eu4CYsQPM3rrLzvzNiNgwaIIFTnyT2N4U7qCQ78qtWC3-p0el6Hvv8qxG5uuEw-WgMElwKB21hc3RlcjAQAUJPCglzZWNwMjU2azESIKhBU0eCOO6Vinz_8vhtFSAhYYqrkEXC8PHGxkuIUev8GiAydFHLXb7c22A1Uj_PR21NZp6BCDQqNq2xd244txRgsQ",
            "status": "CREATED"
        }
    ],
    "kind": "ManagedDIDPage",
    "pageOf": "http://localhost:8080/prism-agent/did-registrar/dids",
    "self": "http://localhost:8080/prism-agent/did-registrar/dids"
}
```

### 4. Resolution of the created DID

To check that the DID document is correctly populated, test the created DID against the resolution endpoint.

Replacing the `{didRef}` with the long-form DID, and the response should return the DID document.
Replacing the `{didRef}` with the short-form DID, and the resolution should fail since the DID is not yet published.

```bash
curl --location --request GET 'http://localhost:8080/prism-agent/dids/{didRef}' \
--header 'Accept: application/json'
```
