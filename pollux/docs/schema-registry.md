# Schema Registry

## Abstract
This document describes the purpose, supported formats, technical considerations of the `Credential Schema` implementation.

`Schema Registry` is a registry where the `Credential Schema` for the `Verifiable Credential` is stored and distributed among the peers.



## 1. Introduction
`Credential Schema` is a data template for the `Verifiable Credentials`. 
It contains `claims` (`attributes`) of the`Verifiable Credentials`, `author` or the schema, `type`, `name`, `version`, and proof that is was issued by the `author`.
By putting schema definitions on a public blockchain, they are available for all verifiers to examine to determine semantic interoperability of the credential.

Prism Platform v2.x is aimed to support the `Credential Schema` for the following `Verifiable Credential` types:
- JSON/JSON-LD credentials encoded as JWT (v2.0)
- AnonCreds (after v2.0)


## 2. Terminology
#### Verifiable Credential Schema (Schema)
A `Schema` is a template that defines a set of attributes which are going to be used by issuers for issuance of Verifiable Credentials.
#### Schema Registry
The registry where the schema is published and is available for parties.
#### id (UUID)
The unique identifier of the schema that is generated either issuer or by the `Agent`

**_NOTE:_** 
In the [W3C specification](https://w3c-ccg.github.io/vc-json-schemas/#id) this field is localy unique and is a combination of the issuer `DID`, `uuid`, and `version`. 
For example: `"did:example:MDP8AsFhHzhwUvGNuYkX7T/06e126d1-fa44-4882-a243-1e326fbe21db?version=1.0"`

In the AnonCreds speficiation this field is not required. But `schemaId` is also composed from the issuer `DID`, `name` and `version` fields
For example: ["Y6LRXGU3ZCpm7yzjVRSaGu:2:BasicIdentity:1.0.0"](https://indyscan.io/tx/SOVRIN_MAINNET/domain/73904)

---
#### type (String)
A type of the credential which corresponds to the current schema. 

**_NOTE:_** Required by W3C Schema only.
For example: 
```
"type": ["VerifiableCredential", "UniversityDegreeCredential"]
```
AnonCreds specification doesn't require it.

---
#### name (String)
A human-readable name for the schema.

**_NOTE_:** Required for W3C and AnonCreds specifications.

---
#### description (String)
A human-readable descriptoin of the schema. Optional in both W3C and AnonCreds specifications.

---
#### version (String)
A version of the schema. It's recomended to use  [SemVer](https://semver.org/) format in it. 
Required for both W3C and AnonCreds specifications.

---
#### author (DID)
DID of the identity which authored the schema.
Required for both W3C and AnonCreds specifications.

---
#### authored (DateTime)
Date on which the schema was created.
Required for both W3C spec, and corresponds to Tx timestamp `txnTime` in AnonCreds specifications.

---
#### attributes (String[])
The set of attributes (claims) which will be used in the Verifiable Credential

**NOTE:** The field is required for AnonCreds specification and is an array of attributes names without types.
In W3C specification the field `schema` is used to define all the attributes as a valid json-schema.

---
#### tags (String[])
The set of the tags that can be used to add the metainformation to the schema instance. 
It's not required according to W3C and AnonCreds specifications.

## 3. Data Model
The `Credential Schema` has the following data model: `metadata`, `schema`, `proof`.
The current implementation contains the following 
`metadata` fields:

- id
- name
- version
- type
- author
- authored

`schema` field:
- attributes - list of the claims without types

`proof` field:
- type
- created
- verificationMethod
- proofPurpose
- proofValue
- domain
---
**NOTE:** 
`Proof` capability (signing the schema by the issuer key and publishing the anchor to the VDR) is not supported in Prism v2.0
`metadata` is similar a

---

Having these set of fields it possible to perform the following verifications:
- semantic verification of the Verifiable Credentials (Verifiable Credentials contains the subset of the claims defined in the `attribute` field)
- authorship verification (Credential Schema is signed by author DID)

Fields mapping between the standards:


| Prism       | JSON                 | JSON-LD              | AnonCreds  |
|:----------- | -------------------- | -------------------- | ---------- |
| id          | id                   | id                   | id         |
| name        | name                 | name                 | name       |
| version     | version              | version              | version    |
| type        | type                 | type                 | :x:        |
| description | $schema.description  | $schema.description  | :x:        |
| author      | author               | author               | author     |
| authored    | authored             | authored             | authored   |
| attributes  | $schema.properties.* | $schema.properties.* | attrNames  |
| :x:         | :x:                  | @context             | :x:        |
| tags        | :x:                  | :x:                  | :x:        |
| proof       | :question:           | :question:           | :question: |

---
**NOTE:**
JSON and JSON-LD specification support JsonSchema objects inside of the Credential Schema and references to 3rd party resources for resolving the json schema and linked data resources. It's not supported in the current implementation

`tags` fields is specific to Prism and is used for lookup purpose only

:exclamation:`proof` is not implemented and is a matter of discussion (probably, we need to sign the same schema for each of representations)

---
## 4. Use Cases
#### Author creates the Credential Schema (Manage)
//TODO

#### Party lookups the Credential Schema by ID
//TODO

#### Party lookups the Credential Schema by `id`, `name`, `author`, `tags`, `attributes` fields
//TODO

#### Issuer uses the Credential Schema to issue the Verifiable Credential to Holder (Issue Credential 2.0)
//TODO

#### Holder requests the Verifiable Credential to be issued according to the Credential Schema (Issue Credential 2.0)
//TODO

#### Verifier defines the Verification Policy to accept the credentials which were issued based on concrete Credential Schema (Manage)
//TODO

#### Verifier request the Verifiable Credentials from Holder to be issued according to the Credential Schema (Present Proof 2.0)
//TODO

#### Party verifies the semantic of the Verifiable Credentials agains the concrete Credential Schema (Verify)
//TODO

#### Party verifies the authorship of the Credential Schema (Verify)
//TODO

# References

- [Verifiable Credentials JSON Schema 2022](https://w3c-ccg.github.io/vc-json-schemas/)
- [Verifiable Credentials Data Model v2.0](https://w3c.github.io/vc-data-model/)
- [Verifiable Credential Data Integrity 1.0](https://www.w3.org/TR/vc-data-integrity/)
- [AnonCreds Specification](https://hyperledger.github.io/anoncreds-spec/)
- [AnonCreds Schema Example](https://indyscan.io/tx/SOVRIN_MAINNET/domain/73904)

