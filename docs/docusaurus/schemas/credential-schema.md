# Credential Schema [DRAFT]

## Abstract
This document describes the purpose, supported formats, and technical details of the Credential Schema implementation in the Atala Prism Platform.

## 1. Introduction
Credential Schema is a data template for the Verifiable Credentials.
It contains claims (attributes) of the Verifiable Credentials, author of the schema, type, name, version, and proof of authorship.
By putting schema definitions on a public blockchain, they are available for all verifiers to examine to determine the semantic interoperability of the credential.


Prism Platform maintains the internal data model for the credential schema and derives the concrete credential schema according to one of the specifications:
- JSON/JSON-LD (used for the verifiable credentials encoded as JWT)
- AnonCreds


## 2. Terminology
#### Credential Schema
The Credential Schema is a template that defines a set of attributes that the Issuer uses to issue the Verifiable Credentials.

#### Schema Registry
The registry where the Credential Schema is published and is available for parties.

#### Issuer, Holder, Verifier
These are well-known roles in the SSI domain.

#### Party
Party refers to any of the roles: Issuer, Holder, Verifier

## 2. Credential Schema Attributes
#### id (UUID)
The unique identifier of the schema

**_NOTE:_**
According to the [W3C specification](https://w3c-ccg.github.io/vc-json-schemas/#id), this field is locally unique and is a combination of the Issuer `DID`, `uuid`, and `version`.
For example: `"did:example:MDP8AsFhHzhwUvGNuYkX7T/06e126d1-fa44-4882-a243-1e326fbe21db?version=1.0"`

According to the AnonCreds speficiation, this field is not required. But `schemaId` is composed of the Issuer `DID`, `name`, and `version` fields
For example: Indy Tx ["Y6LRXGU3ZCpm7yzjVRSaGu:2:BasicIdentity:1.0.0"](https://indyscan.io/tx/SOVRIN_MAINNET/domain/73904)

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

---
#### description (String)
It is a human-readable description of the schema.
Optional in both W3C and AnonCreds specifications.

---
#### version (String)
It is a version of the schema that contains the revision of the schema in [SemVer](https://semver.org/) format.
Required by both W3C and AnonCreds specifications.

---
#### author (DID)
DID of the identity which authored the schema.
It is required by both W3C and AnonCreds specifications.

---
#### authored (DateTime)
It's the date and time stamp of the schema creation.
AnonCreds specification uses a transaction timestamp`txnTime` for this purpose.

---
#### attributes (String[])
The set of attributes (claims) used in the verifiable credential.

**NOTE:** The field is required by AnonCreds specification and is an array of attribute names without types.
W3C specification defines the `schema` field where all the attributes are defined as a valid JSON schema.

---
#### tags (String[])
The tags are additional information for searching the credential schema instance.

## 3. Data Model
The credential schema has the data model specified by [Verifiable Credentials Data Model v2.0](https://w3c.github.io/vc-data-model/): `metadata`, `schema`, `proof`.

The current implementation contains the following
`metadata` fields:

- id
- name
- version
- type
- author
- authored

`schema` field:
- attributes - list of the attributes (attribute `name`, attribute `type` and `isRequired` field) that are used to define the credential schema

`proof` field:
- type
- created
- verificationMethod
- proofPurpose
- proofValue
- domain

---
**NOTE:**
Prism Platform will support the signing credential schema in the later versions.

---

Having this set of fields makes it possible to perform the following verifications:
- semantic verification of the verifiable credentials
- authorship verification

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
JSON and JSON-LD specification support JsonSchema objects inside the Credential Schema and reference 3rd party resources for resolving the JSON schema and linked data resources. However, this functionality is not supported in the current implementation.

`tags` field is specific to Prism Platform and is used for the search capabilities only

## 4. Credentia Schema Flows
#### The Issuer creates the Credential Schema (Manage)
The Issuer defines the credential schema by composing the metadata and required attributes.

The Issuer publishes the credential schema in a particular format according to the specification (W3C or AnonCreds)

#### The Issuer issues the Credential Schema and make it resolvable by `id`

#### The Party resolves the Credential Schema by `id`

#### The Party searches the Credential Schema by `id`, `name`, `author`, `tags`, and `attributes`

#### The Issuer uses the Credential Schema to issue the Verifiable Credential to the Holder using the Issue Credential 2.0 protocol

#### The Holder requests to issue the Verifiable Credential according to the Credential Schema using the Issue Credential 2.0 protocol

#### The Verifier defines the Verification Policy to accept the issued credential based on concrete Credential Schema in the Manage product

#### The Verifier requests the Holder to provide the Verifiable Credentials based on concrete Credential Schema using the Present Proof 2.0

#### The Party verifies the semantics of the Verifiable Credentials against concrete Credential Schema

# References

- [Verifiable Credentials JSON Schema 2022](https://w3c-ccg.github.io/vc-json-schemas/)
- [Verifiable Credentials Data Model v2.0](https://w3c.github.io/vc-data-model/)
- [Verifiable Credential Data Integrity 1.0](https://www.w3.org/TR/vc-data-integrity/)
- [AnonCreds Specification](https://hyperledger.github.io/anoncreds-spec/)
- [AnonCreds Schema Example](https://indyscan.io/tx/SOVRIN_MAINNET/domain/73904)

