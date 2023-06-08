# DID-linked-resources

- Status: draft
- Deciders: Yurii Shynbuiev, Benjamin Voiturier, Lohan Spies, Ezequiel Postan, Shota Jolbordi
- Date: 2023-04-05
- Tags: did, linked-data, ledger

## Target

[Research Spike - Schema and Verifiable Presentation Registry](https://input-output.atlassian.net/browse/ATL-3186)

- Provide a clear and concise analysis of the various schema registry implementation and the associated benefits and downfalls of each approach. 
- Provide a concrete proposal for what we would like to implement for PRISM.
- Provide a generic way of storing and linking the resources for the DID in the PRISM platform.

## Context and Problem Statement

Atala Prism platform must be able to store and distribute the various resources such as credential schemas, logos, revocation status lists, and documents (aka any text, JSON, images, etc). But in the scope of the current ADR the following resource types are discussed:

- credential schema (JSON and AnonCreds)
- credential definition (AnonCreds)
- revocation list

**NOTE**: Resources containing the PII must never be stored on-chain.

Requirements for storing and distributing resources:

- Decentralization - resources must be stored in decentralized storage
- Discoverability - it should be possible to discover the resource
- Longevity - the ability of a storage solution to maintain the availability and integrity of stored resources
- Interoperability - it should be possible for other SSI systems to fetch the resource
- Trust - resources must be stored in reliable tamper-proof storage and be trusted by other SSI systems

Other requirements, such as `versioning`, `security` and `immutability` are out of the scope of this ADR:

- Versioning - is a specific requirement for the particular resource and belongs to the resource metadata
- Security - is an important aspect that must be taken into account by the underlying storage system and data access layer
- Immutability - is one of the strategies to guarantee `trust` and `decentralisation``, but it shouldn't be a requirement by itself.

The technical solution contains a lot of variations and particular small decisions but overall it can be split into two main questions:

- where the resource is stored?
- how the resource is discovered and fetched?

## Constraints

### Storage limitations

All decentralized storage (DLT or IPFS) has storage limitations, and the amount of data that can be stored is limited by the available storage capacity and the way how the resources are stored. 

The following aspect must be taken into account for storing the resources in DLT:

- transaction size limit (can be mitigated by data fragmentation, so the single resource is stored in multiple transactions) - 16KB, 32KB, 64KB, up to 1MB - depending on the type of the blockchain 
- throughput - bytes we can insert to storage per unit of time
- latency - multi-second time per insert
- cost - each insertion costs fees

Based on the nature of the resource the size limitations must be considered.

For the following resource types and the common use cases 16KB should be enough, so it's possible to store these on DLT:
- credential schema
- credential definition
- logo in SVG format
- Merkle Tree
- documentation in the markdown format

For larger resource types IPFS or another option should be considered. Large resource examples:
- media files
- large documents
- large revocation status lists

IPFS doesn't have a size limitation (it's limited by the underlying storage or the particular SDK) and requires additional infrastructure and `incentives` (the way to pay for the storage) from the community.

IPFS can be used for storing the resources, but it should be covered in the scope of separated ADR. 

### Scalability

While DLT and IPFS are designed for scalability, they can still face issues with scalability when it comes to storing SSI resources. As more users store their SSI resources on these platforms, it can become more difficult to scale the infrastructure to handle the increased demand.

To mitigate the scalability challenge the `hybrid` solution can be considered. In this case, the resource is stored in the centralized database or IPFS system, and the `hash`, `signature` or other metadata that guarantees the `trust` is stored on-chain.

Storing credential schemas, and logos, not large documents don't require the hybrid solution, so the use cases for it is out of scope in the current ADR.

Scalability issues also must be considered in the decision for linking the resources to the DID. For instance, the Cheqd solution keeps all the resources linked to the DID inside of the metadata of the DIDDoc which leads to growing the DIDDoc size after some period and an update of the DID Document when the new resource is published on-chain and linked to the DID.

### Access control

SSI resources stored in DLT and IPFS can be accessed by anyone who has access to the network. 

This can be a security concern for organizations that need to control access to their SSI resources.

Access control also can be an issue for interoperability with other SSI systems.

The types of resources such as credential schemas, logos, and revocation lists should be available without additional access control.

### Data privacy

While DLT and IPFS are designed to be secure, there is still a risk that SSI resources stored on these platforms could be accessed or stolen by unauthorized parties. 

This is especially concerning when it comes to sensitive personal information.

Personal data or any other sensitive information should not be stored on-chain and be available for unauthorized parties.

Credential schemas, documents, and logos usually do not contain personal data, so can be stored on-chain.

Revocation lists that are designed using privacy-preserving capabilities can be stored on-chain as well.

## Decision Drivers

- Interoperability
- Trust
- Longevity
- Scalability
- Discoverability
- Vendor Lock

## Storage

Choosing the right storage for resources is an architectural decision.

Companies that build SSI platforms usually use the underlying blockchain for storing the resources in a generic way and an API layer with an underlying centralized database and SDK for indexing, and access to the resources.

Usually, resources are stored efficiently (any binary format, protobuf, CBOR) on-chain to reduce the size and the cost of the operation.

The application layer that communicates with the underlying blockchain is used for publishing and retrieval of the resource.
Based on concrete implementation, the resources can be decoded and indexed in the database and available via internal API, SDI, or Universal Resolver.

Storing resources off-chain also makes sense, but in this case, the `longevity` of the storage and an API layer is limited by the lifetime of the organization that published the resource. For this solution, `trust` can be achieved by signing the resource using the key of the DID stored on-chain. This solution is not fully centralized as the organizations have their infrastructure with the database.

## Linking the resource to the DID

Linking a resource to a DID means associating a specific resource with a DID and resolving the resource via the Universal Resolver or application API or SDK or finding it on-chain.

In all the considered solutions this is achieved using a DID document and the algorithm for discovery and resource dereferencing.

## Considered Options

### DID document linkedResources field

The particular resource must be available via URL and the metadata of the resource are described in the `linkedResources` array.

Example:

```
{
  "@context": "https://w3id.org/did/v1",
  "id": "did:example:123456789abcdefghi",
  "publicKey": [{
    "id": "did:example:123456789abcdefghi#keys-1",
    "type": "Ed25519VerificationKey2018",
    "controller": "did:example:123456789abcdefghi",
    "publicKeyBase58": "7dNN1A8H4DwPU1h4btvohGadnbx8sHF2U6XJU6vLBBfA"
  }],
  "linkedResources": [{
    "url": "https://example.com/credentialschema/123",
    "type": "CredentialSchema",
    "name": "DrivingLicense"
  }]
}
```

#### Positive Consequences

- solution describes the simple way of linking the resources to the DID Document. This approach looks outdated and is not part of the did-core specification.

#### Negative Consequences

The drawbacks of the solution:

- interoperability: it is not a part of the DID-core specification anymore even if it is possible to find information about it
- interoperability: the resource should be fetched by the application or SDK
- trust: must be guaranteed by the underlying DLT, the DID document should have an anchor: hash, signature or Tx id that references to the DLT
- discoverability: information about the resource's metadata (author, version, content type) is absent
- scalability: DID document must be updated when a new resource is added, so the solution sacrifices `scalability` as the content of the DID document will grow

#### Out of the Scope

- longevity: should be guaranteed by the underlying DLT

### DID document didDocumentMetadata -> linkedResourceMetadata (Cheqd ADR)

Each resource entry is a part of the collection and is described in the `linkedResourceMetadata` field.

The solution is described in the Cheqd ARD in the [Links](#Links) section of the current ADR 

Example:

```
{
	"didDocumentMetadata": {
		"linkedResourceMetadata": [
		  {
		    "resourceURI": "did:cheqd:mainnet:1f8e08a2-eeb6-40c3-9e01-33e4a0d1479d/resources/f3d39687-69f5-4046-a960-3aae86a0d3ca",
		    "resourceCollectionId": "1f8e08a2-eeb6-40c3-9e01-33e4a0d1479d",
		    "resourceId": "f3d39687-69f5-4046-a960-3aae86a0d3ca",
		    "resourceName": "PassportSchema", // First version of a Resource called PassportSchema
		    "resourceType": "CL-Schema",
		    "mediaType": "application/json",
		    "created": "2022-07-19T08:40:00Z",
		    "checksum": "7b2022636f6e74656e74223a202274657374206461746122207d0ae3b0c44298",
		    "previousVersionId": null, // null if no previous version, otherwise, resourceId of previous version
		    "nextVersionId": null, // null if no new version, otherwise, resourceId of new version
		  }
		]
	}
}
```

The solution is not fully interoperable with the SSI ecosystem, but it's probably the first successful specification that formalizes the DID-linked resources and the DID URL. 

Cheqd's approach for linking the resources to the DID is not a part of the current version of DID specification. Even if it's possible to find some information about `linkedResources` and `linkedResourceMetadata` optional field of the DIDDoc in the cache of the search system or ChatGPT. 

Looks like the ToIP specification is inspired by Cheqd's ADR.

#### Positive Consequences

- versioning of the resource, as metadata contains the references to the previous and next version
- collection definition is formalized and published on-chain and in the DID document, so all the resources are categorized
- discoverability: URI is replaced with DID URL that allows discovering the resource using Internal and/or Universal resolver
- trust: the `checksum` is provided, so it is possible to verify that the resource was not modified by 3rd party


#### Negative Consequences

- scalability: the DID document should be updated when the new resource is created
- interoperability: using the Universal Resolver is optional, so either SDK or internal application API must be used to fetch the resource
- standard: the `linkedResourceMetadata` field is not a standard part of the DID specification, so the application should be aware of how to deal with it


### DID URL dereferencing (W3C specification)

The current solution is based on the dereferencing algorithm described in the [DID-Resolution#dereferencing](https://w3c-ccg.github.io/did-resolution/#dereferencing) specification and describes how the DID resolver can dereference the resource linked to the DID. It does not describe where the resource is stored.

The main idea is an algorithm that allows using the DID URL and the information about the services in the DID Document that allows DID Resolver to compose the final resource URL and return the requested resource.

Dereference is performed by defining the service `id` and `relativeRef` params or `path` in the DID URL

**NOTE:**
The `service.type` property is not taken into account in this flow. 
According to the did-core specification, the service type and its associated properties SHOULD be registered in the [DID Specification Registries](
https://www.w3.org/TR/did-spec-registries/#service-types).
So, defining and registering the `schemaService` or `resourceService` should be the next step to facilitate the interoperability of SSI systems. 

Example 1: using `service` and `relativeRef`

The credential schema resource can be defined as a DID URL

```
did:prism:abcdefg?service=credentialschema&relativeRef=%2Fcredentialschemas%2F123e4567-e89b-12d3-a456-426614174000
```

and the DID Document must have the service defined

```
{  
  "service": [
    {
      "id": "did:prism:abcdefg#credentialschema",
      "type": "CredentialSchema",
      "serviceEndpoint": "https://agent.example.com/schema-registry"
    }
  ]
}
```

so, the Universal Resolver using the concrete DID resolver must dereference the resource as

```
https://agent.example.com/schema-registry/credentialschemas/F123e4567-e89b-12d3-a456-426614174000
```

and should return the instance of the credential schema

Example 2: is another variation but using `service` and `path` in the DID URL

```
did:prism:abcdefg/credentialschemas/123e4567-e89b-12d3-a456-426614174000?service=credentialschema
```

In this case, the DID Method may describe how the path should be resolved and the resource must be fetched.

#### Positive Consequences

- interoperability: the resource is resolved by the conformant DID resolver according to the specification
- discoverability: the resource defined in DID URL is resolved and fetched dynamically
- scalability: the DID document is not updated for each new resource

#### Negative Consequences

- specification: for the particular cases when the `path` is used in the DID URL, the resolution behavior must be described in the DID Method
- scalability: the algorithm contains 2 or 3 steps and the DID Document is always must be resolved in the first step

#### Out of the Scope
- trust, longevity, and technology stack are not specified in this solution

### DID URL Dereferencing (Trust over IP specification - outdated)

[ToIP specification](https://wiki.trustoverip.org/display/HOME/DID+URL+Resource+Parameter+Specification) is an analog of the W3C dereferencing specification and describes the convention for dereferencing the resources from the DID URL

The main idea is the same: use the DID URL and a combination of convention and the DID method to resolve the digital resource associated with the DID.

But instead of relying on the `service` and the `relativeRef` parameter, the ToIP spec is focused on the `resource` parameter - so, if the DID URL contains the `resource` parameter - it must return the resource.

Example:

```
did:example:21tDAKCERh95uGgKbJNHYp/some/path?resource=true

did:example:21tDAKCERh95uGgKbJNHYp/some/longer/path?resource=json

did:example:21tDAKCERh95uGgKbJNHYp/uuid:33ad7beb-1abc-4a26-b892-466df4379a51/?resource=ld+json

did:example:21tDAKCERh95uGgKbJNHYp/resources/uuid:33ad7beb-1abc-4a26-b892-466df4379a51/?resource=cbor
```

The main disadvantage of this approach is that the logic for resolving and fetching the resource associated with the given DID URL completely relies on DID method specification (in the W3C variation it's just a convention and the algorithm for the resource resolution)

ToIP specification doesn't describe the details about the storage of the underlying resource. It might be DLT (blockchain or IPFS) or classic cloud or on-premise storage.

### DID URL Dereferencing (Trust over IP specification - latest)

The new specification for DID URL dereferencing is an improved specification with recommended Cheqd idea to publish the resource metadata in the DID Document.

The main difference with the previous specification is an introduction of parameters that can discover the resource (instead of using `resource` field only) and simplification of the Cheqd's approach by skipping the `collection` abstraction. 

The DID Document refers to the associated resource via linked resource metadata.

The changes to the DID method are also required (described in the Verifiable Data Registry and DID Method Requirements)

The current status of the document is a draft, but it's going to be published in the did-core specification.

The list of resource parameters with descriptions is the following:

- `resourceUri` (required): A string or a map that conforms to the rules of [RFC3986] for URIs which SHOULD directly lead to a location where the resource can be accessed from. 
- `resourceCollectionId` (optional): A string that conforms to a method-specific unique identifier format.
- `resourceId` (optional): A string that conforms to a method-specific unique identifier format.
- `resourceName` (required): A string that uniquely names and identifies a resource. This property, along with the resourceType below, can be used to track version changes within a resource.
- `resourceType` (required): A string that identifies the type of resource. This property, along with the `resourceName` above, can be used to track version changes within a resource. Not to be confused with the media type. (TBC to add to DID Spec Registries)
- `resourceVersionId` (optional): A string that uniquely identifies the version of the resource provided by the resource creator as a tag.
- `mediaType` (required): A string that identifies the IANA-registered Media Type for a resource.
- `created` (required): A JSON String serialized as an XML DateTime normalized to UTC 00:00:00 and without sub-second decimal precision.
- `checksum` (optional): A string that provides a checksum (e.g. SHA256, MD5) for the resource to facilitate data integrity.
- `previousVersionId` (optional): The value of the property MUST be a string. This is the previous version of a resource with the same resourceName and resourceType. The value must be 'null' if there is no previous version. 
- `nextVersionId` (optional): The value of the property MUST be a string. This is the previous version of a resource with the same resourceName and resourceType. The value must be 'null' if there is no previous version. 

This specification describes many important aspects:

- the list of the query parameters in the DID URL for dereferencing the resource and error messages, 
- DID Method and VDR requirements, and 
- DID Resolver requirements

#### Positive Consequences

- interoperability: the resource is resolved in a standard way according to the ToIP specification following W3C specification for DID URL dereferencing
- discoverability: the resource defined in DID URL is resolved and fetched dynamically
- scalability: compared to W3C specification, the DID Document is not required to fetch the resource, so instead of 2-3 steps (calls), the resource resolution should be completed in a single step. The behavior must be described in the DID Method and implemented by the DID resolver.
- trust: publishing the `checksum` of the resource inside of the DID Document allows other SSI system to check the resource validity. 

#### Negative Consequences

- scalability: the specification is inspired by the Cheqd approach to store the linkedResourceMetadata inside of the DID Document. ToIP specification describes this functionality as optional ("Through associating the resource with a DID Document, the DID Document may generate associated metadata about the resource")
- complexity: the specification is the most complex to fetch the resource, so it's not trivial to implement it for all DIDs in the SSI ecosystem.
- specification: the resolution logic of resources must be described in the DID method and implemented in the DID resolver. As a consequence of this approach, the solution must either communicate directly with the DLT or rely on the SaaS layer for fetching the resources.

#### Out of the Scope

- longevity, and technology stack are not specified in this solution but must be guaranteed by the underlying DLT


### RootsID - Cardano AnonCreds (Implementation of ToIP at the Cardano stack)
RootsID adopted the AnonCreds specification to store the credential schema and credential definition on the Cardano blockchain.

Links to the implementation and the method description are in the #Links section for this ADR

It is a proof of concept implementation of ToIP specification of DID URL dereferencing for resolving the resources linked to the DID in TypeScript and Python using Blockfrost REST API to the Cardano blockchain. The Blockfrost SaaS middle layer is used for publishing and fetching the Tx from the Cardano blockchain.

The solution is limited to storing AnonCreds entities only but can be extended to store the general resources.

For more details, please refer to the source code.

As the solution is based on the latest ToIP specification, it derives all positive and negative consequences from the previous but contains the concrete implementation for the Cardano blockchain that solves the trust and longevity aspects of the technical solution.

#### Positive Consequences

- interoperability: the resource is resolved in a standard way according to the ToIP specification following W3C specification for DID URL dereferencing
- discoverability: the resource metadata is published in the DID Document
- trust & longevity: is guaranteed by the underlying Cardano blockchain
- technology stack: the solution is leveraging Blockfrost REST API for communicating with the Cardano blockchain
- technology stack: the solution is stateless and is much cheaper in terms of the infrastructure cost
- technology stack: the solution is implemented in Python and TypeScript (mobile platforms can use the same approach as well)


#### Negative Consequences
- scalability: the specification is inspired by the Cheqd approach to store the linkedResourceMetadata inside of the DID Document
- the convention for references and the logic must be carefully reviewed:
	- `schemaId` in this solution is `{didRef}/resources/{cardano_transaction_id}`, so it doesn't refer to the `id` but to the Tx where everything else is stored (it's an interesting idea for a stateless design)
	- resource metadata is built according to the ToIP specification but for AnonCreds entities only: credential schema and credential definition.
- technology stack: it doesn't fit to current Atala PRISM platform, but can be used for inspiration.


### Hyperledger AnonCreds

According to the AnonCreds specification, such kinds of resources as credential schema and credential definition are stored on-chain. Indy blockchain is used by the Hyperledger technology stack.

The credential schema and definition are not signed by the issuer, but the transaction with the underlying resource is published by the issuer. So, the integrity of the resource is guaranteed by the fact that it's published inside of the transaction signed by the issuer. 

Example of the credential schema transaction:

```
{
  "txn": {
    "data": {
      "data": {
        "attr_names": [
          "birthlocation",
          "facephoto",
          "expiry_date",
          "citizenship",
          "name",
          "birthdate",
          "firstname",
          "uuid"
        ],
        "name": "BasicIdentity",
        "version": "1.0.0"
      }
    },
    "metadata": {
      "digest": "06bf8a90335563826154700bf80003598932c8ffaffd4f3656fd8ed604bbb639",
      "endorser": "Ar1YzNwcM74M2Z4XKUWXMW",
      "from": "Y6LRXGU3ZCpm7yzjVRSaGu",
      "payloadDigest": "44e0181c9f9d5080434f9bf11801f1b0768a6b985195e14d56e5dab06fde0cb8",
      "reqId": 1632381635230531300,
      "taaAcceptance": {
        "mechanism": "at_submission",
        "taaDigest": "8cee5d7a573e4893b08ff53a0761a22a1607df3b3fcd7e75b98696c92879641f",
        "time": 1632355200
      }
    },
    "protocolVersion": 2,
    "type": "101",
    "typeName": "SCHEMA"
  },
  "txnMetadata": {
    "seqNo": 73904,
    "txnId": "Y6LRXGU3ZCpm7yzjVRSaGu:2:BasicIdentity:1.0.0",
    "txnTime": "2021-09-23T07:20:40.000Z"
  }
}
```

The resource (credential schema) in the current example can be discovered using Indy SDK by the following id:
```
Y6LRXGU3ZCpm7yzjVRSaGu:2:BasicIdentity:1.0.0
```

Technical details and flows are described in the [AnonCreds](https://hyperledger.github.io/anoncreds-spec/) specification.

#### Positive Consequences

- discoverability: the credential schema or any other resource is discovered using the Indy SDK
- scalability and longevity: are guaranteed by the underlying blockchain technology
- trust: is achieved by underlying blockchain technology, the transaction with the resource contains the hashes and contains the `digest`, `taaDigest`, and `payloadDigest` fields

#### Negative Consequences

- interoperability: the current solution for storing the resources on-chain is coupled with the Indy blockchain and SDK (it will be mitigated by decoupling the AnonCreds specification from the Indy blockchain)
- vendor lock: the solution is tightly coupled to the Indy blockchain (it will be mitigated in the future by decoupling the Aries project from the underlying Indy blockchain)

**Note**: there is a new specification of the AnonCreds that is decoupled from the Hyperledger stack. The specification can describe more details about the resource publishing.

### Trinsic Solution  

The Trinsic solution is built on top of the Hyperledger Aries platform on the Indy blockchain
The main benefit is the Trinsic application layer that defines the domain models, entities, REST API and SDK for working with these.

The resource, such as credential schema, is stored on-chain, but the technical complexity and low-level details are hidden under `Template` and [`Template Service`](https://docs.trinsic.id/reference/services/template-service/#template-service)

#### Positive & Negative Consequences

Are similar to the Hyperledger AnonCreds solution

The main benefit of the Trinsic approach to storing resources is a good abstraction layer, documentation, REST API and a variety of supported programming languages in SDKs for dealing with underlying resources.


### Atala PRISM solution #1 (W3C with dynamic resource resolution)

Atala PRISM solution for storing the resources linked to the DID depends on two decisions that are described in the Context and Problem Statement:

- where the resource is stored
- how the resource is discovered and fetched

Taking into account the advantages and disadvantages of the existing solutions the decision about the solution for the Atala PRISM platform might be the following:

-the resource is linked to the DID by convention specified in the W3C specification, so specifying the resource in the DID URL and defining the service endpoint that exposes the resource allows to discover and fetch the resource using the Universal Resolver
- as an option, the same resource can be discovered and fetched by the PRISM platform backend and SDK without loading the Universal resolver
- the resource integrity must be guaranteed by one of the following options:
	- by signing the payload with one of the DID's keys or 
	- by publishing the resource metadata that contains the information about the resource (id, type, name, media type, hash) on-chain or
	- for the resource that is less than the blockchain limitation (up to 64KB) by publishing the resource together with the hash, and/or signature
- the resource can be stored in the cloud storage - PostgreSQL database - for indexing and lookup API

As the Atala PRISM platform can leverage the Cardano blockchain and there is a strong requirement for longevity and security - the resource together with the signature and/or hash must be stored in the Cardano blockchain.

An example of this solution will be the following (concerning the current infrastructure and services):

- prism-node must be able to store the generic resource payload, signature and/or hash on-chain and restore the given resource in the underlying database (PostgreSQL) for indexing and lookup API
- credential schema (or any other resource module) must be a part of the Atala SSI infrastructure and allow
	- publishing the concrete resource as a generic resource using the prism-node API
	- expose the API for discovery and fetching the resource by URL
	- expose the API for managing the resources (create, publish, lookup with pagination)
- the Universal Resolver for the DID Method must be able to discover and fetch the resource by DID URL
- is needed, SDK and backend services can fetch the resources directly (not via the Universal Resolver)

Example:

Given the credential schema with the signature:

```
{
  "$id": "driving-license-1.0",
  "$schema": "http://json-schema.org/draft-07/schema#",
  "description": "Driving License",
  "type": "object",
  "properties": {
    "credentialSubject": {
      "type": "object",
      "properties": {
        "emailAddress": {
          "type": "string",
          "format": "email"
        },
        "givenName": {
           "type": "string"
        },
        "familyName": {
           "type": "string"
        },
        "dateOfIssuance": {
           "type": "datetime"
        },
        "drivingLicenseID": {
           "type": "string"
        },
        "drivingClass": {
           "type": "integer"
        },
        "required": [
          "emailAddress",
          "familyName",
          "dateOfIssuance",
          "drivingLicenseID",
          "drivingClass"
        ],
        "additionalProperties": true
      }
    }
  },
  "proof": {
    "type": "RsaSignature2018",
    "created": "2023-04-18T10:30:00Z",
    "jws": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6Imh0dHBzOi8vZXhhbXBsZS5jb20vY3JlZGVudGlhbHMvc3ViamVjdCIsInR5cGUiOiJWZXJpZmljYWxpY0NyZWRlbnRpYWwiLCJpc3N1ZXIiOiJodHRwczovL2V4YW1wbGUuY29tL2lzc3VlciIsImlzc3VlckRhdGUiOiIyMDIzLTA0LTE4VDEwOjMwOjAwWiIsImV4cGlyYXRpb25EYXRlIjoiMjAyNC0wNC0xOFQxMDowOTo1MFoiLCJjcmVkZW50aWFsU3ViamVjdCI6eyJpZCI6Imh0dHBzOi8vZXhhbXBsZS5jb20vY3JlZGVudGlhbHMvc3ViamVjdC9zdWJqZWN0IiwibmFtZSI6IkpvaG4gRG9lIiwic2lnbmF0dXJlIjoxMH19.OesuS6eC0gVh8SZpESM7Z4Yln9sGSsJHQ8s0LlcsD99H6_7U6vukUeT2_GZTtuTf9SwIfdtgViFTOfhzGTyM6oMGEeUJv6Umlh6TQ1fTm9XEDQV7JDBiaxRzV7S_vS6i",
    "alg": "RS256"
  }
}
```

In order to store it as a general resource on-chain, it should be binary encoded into CBOR format (or Base64 encoded string) and the metadata must be added to it.

For example, it might look like the following JSON object:

```
{
        "id": "f3d39687-69f5-4046-a960-3aae86a0d3ca",
        "name": "DrivingLicense-1.0",
        "resourceType": "CredentialSchema",
        "mediaType": "application/json", // MIME Type of the resource
        "data": "SGVsbG8sIHdvcmxk", // base 64 encoded or CBOR file
        "did": "did:prism:abdcefg", // the DID reference to link the resource to the DID and create the anchor to the DID
}
```

... and published on the Cardano blockchain as a payload of the AtalaOperation object, so can be retrieved from the blockchain and added to the indexed database for resolution by the REST API

Given there is an Agent or CredentialSchema service that exposes the REST API for fetching the credential schema by ID (in the current implementation it corresponds to the PrismAgent `/schema-registry/credential-schema/{uuid}`, but later might be changed to `/credential-schema/{didRef}/{id}?version={version}` )

So, the services of the PRISM platform and SDK can resolve the given schema by URL and use the convenient lookup API with filtering and pagination to manage the credential schema in the Web application.

To define the `schemaId` in the message of Issue Credential and Present Proof protocols the following DID URL can be used:

```
did:prism:abcdefg1234567890?service=credentialschema&relativeRef=%2Ff3d39687-69f5-4046-a960-3aae86a0d3ca
```

The version is skipped as for resolving the single resource we don't need a `version` parameter
`f3d39687-69f5-4046-a960-3aae86a0d3ca` - is a unique identifier that is derived from the triple: didRef, id and version.

So, having the following service endpoint definition in the DID Document:


```
{  
  "service": [
    {
      "id": "did:prism:abcdefg#credentialschema",
      "type": "CredentialSchemaService",
      "serviceEndpoint": "https://agent.example.com/schema-registry/schemas"
    }
  ]
}
```

And having the logic for dereferencing the DID URL in the PRISM DID Resolver, any system in the SSI ecosystem can fetch this resource and validate its authorship.

Storing resources larger than 64KB is out of the scope of this ADR. These must be stored in a slightly different way, for instance, the image ~10MB, can be stored and linked to the DID Document in the following way:

- the image is stored in the cloud database in a binary format
- the metadata and the hash of the image are stored on-chain
- optionally, the signature of the owner DID can be generated for the payload and stored together with the hash 
- to prove the integrity of the image file, the hash of the binary representation must be the same and/or the signature must be verified
- the resource can be fetched in the same way and the credential schema from the previous example

#### Positive Consequences

- discoverability: the credential schema or any other resource is discovered using the Universal Resolver
- scalability: the size of DID document doesn't grow when a new resource is added, the number of the resources is limited by the scalability of the underlying database and the blockchain
- longevity: for the resource that can be stored on-chain the longevity is 100% guaranteed by the underlying blockchain technology
- trust: is achieved by cryptography and the underlying blockchain technology, the transaction with the resource contains the hashes and the signatures that can be verified
- interoperability: any system that uses Universal Resolver can fetch the resource by dereferencing the DID URL (following the  W3C specification)
- vendor lock: by publishing the specifications and the algorithms for fetching the data, the resource can be resolved by any other SSI system

#### Negative Consequences

- longevity: for the resource that can not be stored on-chain because of the large size longevity is guaranteed by the cloud recovery procedures and data backup. As an option for mitigating this problem, the resource can be stored in IPFS (additional ADR is required for this)
- vendor lock: the solution is coupled to the Cardano blockchain 

**NOTE:** one of the main concerns of this ADR is storing the resources on-chain because of size limitation, throughput, latency and cost. This option allows to postpone this decision and implement the DID-linked resources without the need of storing resources on-chain.

---

### Atala PRISM solution #2 (ToIP specification implementation)

ToIP specification can be used to implement the resource resolution.
To implement it the following things are required:

- specify in the DID method the logic of resolution of the resources from the DID URL
- specify the service mapping in the DID method and implement the resource resolution logic in the DID resolver
- add `didDocumentMetadata.linkedResourceMetadata` field to the DID method and implement the logic in the VDR layer
- implement the service layer according to the ToIP specification

ToIP solution specifies the requirements to the VDR (blockchain) that is not easy to achieve with the current implementation of the Atala PRISM platform. 
According to this specification, the Universal Resolver must have the direct access to the blockchain or use a centralized layer for fetching the resources over REST API.
Before implementing this specification is the Atala PRISM platform we need to answer the following questions:

- who is hosting the `prism-node` infrastructure for the Universal Resolver and how it's managed?
- should we make the PRISM DID Method responsible for resource resolution logic?

#### Positive Consequences

- interoperability: the resource is resolved in a standard way according to the ToIP specification following W3C specification for DID URL dereferencing
- discoverability: the resource defined in DID URL is resolved and fetched dynamically
- scalability: compared to W3C specification, the DID Document is not required to fetch the resource, so instead of 2-3 steps (calls), the resource resolution should be completed in a single step. The behavior must be described in the DID Method and implemented by the DID resolver.

#### Negative Consequences

- complexity: the solution is complex and over-engineered. A lot of components and flows must be defined to fetch the resource.
- specification: current approach might be changed as it's still in the draft status, so implementing it is risky

## Decision Outcome

Each option has technical challenges and limitations, but it's possible to define the following decisions as an outcome:

- the resource MUST be stored on-chain to guarantee trust and longevity aspects, for the Atala PRISM platform it is the Cardano blockchain
- the resource SHOULD be indexed for quick lookup over the API
- the resource CAN be referenced in the DID Document for additional discoverability
- the resource MUST be dereferenced from the DID URL according to W3C or ToIP specification and implementation
- the resource resolution CAN be described in the DID Method (for the dynamic resource linking and W3C dereferencing algorithm it's not required)
- the complexity of the solution SHOULD be adequate to the original goal: get the resource linked to the DID
- the solution SHOULD be scalable
- the solution MUST be interoperable and easily adopted by the SSI ecosystem

Atala PRISM solution option #1 is considered a good option as it satisfies the requirements and the majority of the negative consequences are mitigated.
The following comparison table is a summary of the available options.

| Option | Simplicity | Trust | Scalability | Interop | Discoverability | Decentalisation |
| ------ | ---------- | ----- | ----------- | ------- | --------------- | --------------- |
| linkedResources field | :heavy_plus_sign: | :heavy_check_mark: | :heavy_minus_sign: | :heavy_minus_sign: | :heavy_plus_sign: | N/A |
| linkedResourceMetadata (Cheqd)| :heavy_minus_sign:/:heavy_plus_sign: | :heavy_check_mark: | :heavy_minus_sign:/:heavy_plus_sign:| :heavy_plus_sign:|:heavy_plus_sign: | :heavy_check_mark: |
| DID URL Dereferencing (W3C specification)| :heavy_plus_sign: | N/A | :heavy_plus_sign: | :heavy_plus_sign: | :heavy_minus_sign: | :heavy_check_mark: |
| DID URL Dereferencing (ToIP specification) | :heavy_minus_sign: | :heavy_check_mark: | :heavy_plus_sign:/:heavy_minus_sign: | :heavy_plus_sign:/:heavy_minus_sign: | :heavy_plus_sign: | :heavy_check_mark: |
| RootsID - Cardano AnonCreds | :heavy_plus_sign: | :heavy_check_mark: | :heavy_plus_sign:/:heavy_minus_sign: | :heavy_plus_sign: | :heavy_plus_sign: | :heavy_check_mark: |
| Hyperledger AnonCreds | :heavy_plus_sign: | :heavy_check_mark:| :heavy_plus_sign: | :heavy_minus_sign: | :heavy_minus_sign: | :heavy_check_mark: |
| Trinsic | :heavy_minus_sign: | :heavy_check_mark: | :heavy_plus_sign:/:heavy_minus_sign: | :heavy_minus_sign: | :heavy_minus_sign: | :heavy_check_mark: |
| Atala PRISM #1 W3C | :heavy_plus_sign: | :heavy_check_mark: | :heavy_plus_sign: | :heavy_plus_sign: | :heavy_minus_sign: | :heavy_check_mark: |
| Atala PRISM #2 ToIP | :heavy_minus_sign: | :heavy_check_mark: | :heavy_minus_sign:/:heavy_plus_sign: | :heavy_plus_sign:/:heavy_minus_sign: | :heavy_plus_sign: | :heavy_check_mark: |

---

Each option reviewed in this ADR is a composition of the following architectural decisions:

- the resource is stored on-chain or off-chain
- the VDR layer is managed or unmanaged (for instance, leveraging the Blockfrost REST API can simplify the solution, but might be expensive at scale)
- domestic or official (W3C/ToIP specification implementation
- static/dynamic resource discoverability (is resource metadata stored in the DID Document or not)
- DID URL dereferencing algorithm and naming convention
- level of trust: Tx signature, resource hash, resource signature
- decentralized or SaaS solution
- SDK, Universal Resolver or REST API for fetching the resource

The main benefits of option #1 for the Atala PRISM platform are the following:

- the resource is stored on-chain
- the resource is published and indexed by the managed VDR layer (prism-node)
- the resource is available via REST API & SDK for the product-level applications
- the resource is dereferenced via the DID URL in the DID resolver
- the resource is linked to the DID dynamically (using DID URL + dereferencing algorithm)
- this solution is scalable and decentralized (anyone can deploy the PRISM stack)
- level of trust can be guaranteed by the underlying VDR and enforced by hashes or signatures of the resource


## Links

- [Our Approach to DID-Linked Resources](https://blog.cheqd.io/our-approach-to-resources-on-ledger-25bf5690c975)
- [Context for Developing DID-Linked Resources](https://docs.cheqd.io/identity/guides/did-linked-resources/context)
- [ADR 002: DID-Linked Resources](https://docs.cheqd.io/identity/architecture/adr-list/adr-002-did-linked-resources)
- [Hyperledger Anoncreds - Schema Publisher: Publish Schema Object](https://hyperledger.github.io/anoncreds-spec/#schema-publisher-publish-schema-object)
- [ToIP - DID URL Resource Parameter Specification](https://wiki.trustoverip.org/display/HOME/DID+URL+Resource+Parameter+Specification)
- [ToPI - DID-Linder Resources Specification](https://wiki.trustoverip.org/display/HOME/DID-Linked+Resources+Specification)
- [DID-Core#did-parameters](https://www.w3.org/TR/did-core/#did-parameters)
- [DID-Resolution#dereferencing](https://w3c-ccg.github.io/did-resolution/#dereferencing)
- [RootsID AnonCreds Methods](https://github.com/roots-id/cardano-anoncreds/blob/main/cardano-anoncred-methods.md)
- [RootsID Cardano AnonCreds repo](https://github.com/roots-id/cardano-anoncreds)

