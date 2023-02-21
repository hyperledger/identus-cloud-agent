# Create DID

PRISM DID is a type of _decentralized identifier_ that is used across Atala PRISM product suites.
It is powered by a variation of a [_sidetree protocol_](https://identity.foundation/sidetree/spec/) and uses Cardano blockchain as the underlying ledger for DID resolution and operation publication.

A PRISM DID can be created entirely offline without interacting with the blockchain.
This can be done by constructing a DID create-operation, which is a protobuf message with a set of public keys and services.
Once the create-operation is constructed, a DID can be derived from this operation which is well-defined by the PRISM DID method. [**TODO**: insert a link to the spec]

## Roles

1. **DID Controller** is the organization or individual who has control of the DID.

## Prerequisites

1. **DID Controller** PRISM Agents up and running

## Overview

For this example, a PRISM DID is created and stored inside PRISM Agent along with the private keys.
The DID is not automatically published after its creation.
The Agent will keep track of private keys used for the create-operation and the content of the operation itself.

PRISM Agent provides two endpoint groups to facilitate the PRISM DID usage.

- `/dids/*`
Facilitates a low-level interaction between DID operation and the blockchain.
The DID controllers are expected to handle key management independently and use these endpoints for blockchain interaction.

- `/did-registrar/*`
Facilitates a higher-level interaction with PRISM DID where PRISM Agent handles key-management concerns.

## Endpoints

The example uses the following endpoints

| Endpoint                                                                               | Description                                   | Role           |
|----------------------------------------------------------------------------------------|-----------------------------------------------|----------------|
| [`GET /did-registrar/dids`](/agent-api/#tag/DID-Registrar/operation/listManagedDid)    | List all DIDs stored in PRISM Agent           | DID Controller |
| [`POST /did-registrar/dids`](/agent-api/#tag/DID-Registrar/operation/createManagedDid) | Create a new PRISM DID managed by PRISM Agent | DID Controller |
| [`GET /dids/{didRef}`](/agent-api/#tag/DID/operation/getDid)                           | Resolve a DID to DID document                 | DID Controller |

## DID Controller interactions

### 1. Check existing DID on the PRISM Agent

```bash
curl --location --request GET 'http://localhost:8080/prism-agent/did-registrar/dids' \
  --header 'Accept: application/json'
```
The result should show an empty list as no DID has been created on this PRISM Agent instance.

### 2. Create a PRISM Agent managed DID using DID registrar endpoint

The DID controller can create a new DID by sending a DID document template to the Agent.
Since key pairs are generated and managed by PRISM Agent, DID controller only has to specify the key `id` and its purpose (e.g. `authentication`, `assertionMethod`, etc).

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

`GET /did-registrar/dids` endpoint returns the response that should have a list containing one DID.

```json
[
    {
        "did": "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff",
        "longFormDid": "did:prism:4a5b5cf0a513e83b598bbea25cd6196746747f361a73ef77068268bc9bd732ff:Cr4BCrsBElsKBmF1dGgtMRAEQk8KCXNlY3AyNTZrMRIg0opTuxu-zt6aRbT1tPniG4eu4CYsQPM3rrLzvzNiNgwaIIFTnyT2N4U7qCQ78qtWC3-p0el6Hvv8qxG5uuEw-WgMElwKB21hc3RlcjAQAUJPCglzZWNwMjU2azESIKhBU0eCOO6Vinz_8vhtFSAhYYqrkEXC8PHGxkuIUev8GiAydFHLXb7c22A1Uj_PR21NZp6BCDQqNq2xd244txRgsQ",
        "status": "CREATED"
    }
]
```

### 4. Resolution of the created DID

To check that the DID document is correctly populated, test the created DID against the resolution endpoint.

Replacing the `{DID_REF}` with the long-form DID and the response should return the DID document.
Replacing the `{DID_REF}` with the short-form DID and the resolution should fail since the DID is not yet published.

```bash
curl --location --request GET 'http://localhost:8080/prism-agent/dids/{DID_REF}' \
--header 'Accept: application/json'
```
