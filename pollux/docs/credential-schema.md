# Credential Schema [DRAFT]

## Abstract
This document describes the purpose, supported formats, and technical details of the Credential Schema implementation in the Atala Prism Platform.

## 1. Introduction
Credential Schema is a data template for the Verifiable Credentials.
It contains claims (attributes) of the Verifiable Credentials, author of the schema, type, name, version, and proof that the author issued.
By putting schema definitions on a public blockchain, they are available for all verifiers to examine to determine the semantic interoperability of the credential.

Prism Platform v2.x aims to support the Credential Schema for the following Verifiable Credential types:
- JSON/JSON-LD credentials encoded as JWT (v2.0)
- AnonCreds (after v2.0)


## 2. Terminology
#### Credential Schema
The Credential Schema is a template that defines a set of attributes that the Issuer uses to issue the Verifiable Credentials.

#### Schema Registry
The registry where the Credential Schema is published and is available for parties.

#### Issuer, Holder, Verifier
Well-known roles in the SSI domain.

#### Party
Party refers to any of the roles: Issuer, Holder, Verifier

## 2. Credential Schema Attributes
#### id (UUID)
The unique identifier of the schema

**_NOTE:_** 
According to the [W3C specification](https://w3c-ccg.github.io/vc-json-schemas/#id), this field is locally unique and is a combination of the Issuer `DID`, `uuid`, and `version`. 
For example: `"did:example:MDP8AsFhHzhwUvGNuYkX7T/06e126d1-fa44-4882-a243-1e326fbe21db?version=1.0"`

According to the AnonCreds speficiation, this field is not required. But `schemaId` is composed of the Issuer `DID`, `name`, and `version` fields
For example: ["Y6LRXGU3ZCpm7yzjVRSaGu:2:BasicIdentity:1.0.0"](https://indyscan.io/tx/SOVRIN_MAINNET/domain/73904)

---
#### type (String)
A type of credential that corresponds to the current schema. 

**_NOTE:_** Required by W3C Schema only.
For example: 
```
"type": ["VerifiableCredential", "UniversityDegreeCredential"]
```
AnonCreds specification does not require it.

---
#### name (String)
It is a human-readable name for the schema.

**_NOTE_:** Required for W3C and AnonCreds specifications.

---
#### description (String)
It is a human-readable description of the schema. Optional in both W3C and AnonCreds specifications.

---
#### version (String)
It is a version of the schema that contains the revision of the schema in  [SemVer](https://semver.org/) format. 
Required for both W3C and AnonCreds specifications.

---
#### author (DID)
DID of the identity which authored the schema.
It is required for both W3C and AnonCreds specifications.

---
#### authored (DateTime)
It is the date and time the schema was created.
AnonCreds specification uses a transaction timestamp`txnTime` for this purpose.

---
#### attributes (String[])
The set of attributes (claims) used in the Verifiable Credential

**NOTE:** The field is required for AnonCreds specification and is an array of attribute names without types.
W3C specification defines the field `schema` field where all the attributes are defined as a valid JSON schema.

---
#### tags (String[])
The tags are used to add the meta information to the schema instance.

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

//TODO
- add json example of W3C schema
- add json example of AnonCreds schema
- add json example of Prism implementation

---
**NOTE:** 
`Proof` capability (signing the schema by the Issuer key and publishing the anchor to the VDR) is not supported in Prism v2.0

---

Having this set of fields makes it possible to perform the following verifications:
- emantic verification of the Verifiable Credentials (Verifiable Credentials contains the subset of the claims defined in the `attribute` field)
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
JSON and JSON-LD specification support JsonSchema objects inside the Credential Schema and reference 3rd party resources for resolving the JSON schema and linked data resources. This functionality is not supported in the current implementation.

`tags` field is specific to Prism Platform and is used for lookup purposes only

:exclamation:`proof` is not implemented and is a matter of discussion (probably, we need to sign the same schema for each of representations)

---
## 4. Use Cases
#### The Issuer creates the Credential Schema (Manage)
//TODO

#### The Party lookups the Credential Schema by ID
//TODO

#### The Party lookups the Credential Schema by `id`, `name`, `author`, `tags`, `attributes` fields
//TODO

#### The Issuer uses the Credential Schema to issue the Verifiable Credential to the Holder (Issue Credential 2.0)
//TODO

#### The Holder requests to issue the Verifiable Credential according to the Credential Schema (Issue Credential 2.0)
//TODO

#### The Verifier defines the Verification Policy to accept the issued credential based on concrete Credential Schema (Manage)
//TODO

#### The Verifier requests the Holder to provide the Verifiable Credentials based on concrete Credential Schema (Present Proof 2.0)
//TODO

#### The Party verifies the semantics of the Verifiable Credentials against concrete Credential Schema (Verify)
//TODO

#### The Party verifies the authorship of the Credential Schema (Verify)
//TODO

# References

- [Verifiable Credentials JSON Schema 2022](https://w3c-ccg.github.io/vc-json-schemas/)
- [Verifiable Credentials Data Model v2.0](https://w3c.github.io/vc-data-model/)
- [Verifiable Credential Data Integrity 1.0](https://www.w3.org/TR/vc-data-integrity/)
- [AnonCreds Specification](https://hyperledger.github.io/anoncreds-spec/)
- [AnonCreds Schema Example](https://indyscan.io/tx/SOVRIN_MAINNET/domain/73904)

