# Examples of OpenAPI specifications
This directory contains OpenAPI/Swagger specifications of Atala competitors.
Not all the companies are real competitors, but number of them are experts in particular area, so we have a good opportunity to get inspiration and build a better solution.

### godiddy-api.yaml
[Godiddy](https://godiddy.com/) is a hosted platform that makes it easy for SSI developers and solution providers to work with DIDs
It provides the following functionality:
- Resolve DIDs
- Manage DIDs
- Search DIDs

`Castor` is going to provide slightly similar functionality

Points to consider:
- routes are grouped by service name
- path convention: {service-name}/{resource}: `/wallet-service/keys`
- each schema contain a prefix of the service name: `WalletService.Key`
- did methods are `verbs`, DIDs is not a collection. The path for methods looks like `RPC`
- `diddocuments` is a collection of all the `versions` of the DID
- wallet service implementation contains path with verbs: `sign` and `verify`

### aries-cloud-agent.json
[Aries Cloud Agent]() is the SSI solution from Hyperledger

Hyperledger Aries Cloud Agent Python (ACA-Py) is a foundation for building Verifiable Credential (VC) ecosystems. It operates in the second and third layers of the Trust Over IP framework (PDF) using DIDComm messaging and Hyperledger Aries protocols. The "cloud" in the name means that ACA-Py runs on servers (cloud, enterprise, IoT devices, and so forth), and is not designed to run on mobile devices.

`Castor`, `Pollux`, `Mercury` are going to provide similar functionality

Aries Cloud Agent API is a mix of everything you need for SSI in a single place.

Pros:
- everything is in the single place
- easy to deploy and integrate

Cons:
- monolith architecture
- probably, it's hard to evolve it because of coupling
- mix of all the agents for Issuer/Verifier/Holder/Mediator/Other APIs

Points to consider:
- part of the routes are grouped by entity/protocol/service: `did-exchange`, `action-menu`, `mediation`
- part of the routes are started from the root and it's hard to figure out what exactly you are going to do: look at `/connections`
- mix of resource and verbs for protocol facades which is hard to understand: `present-proof`, `issue-credentials`

### sipca-essif-bridge-api.json
[Essif Bridge](https://github.com/sicpa-dlab/essif-bridge)

For the BRIDGE-project, SICPA proposes 3 technological building blocks that will enhance interoperability and scalability in the SSI ecosystem by giving freedom of choice between verifiable credentials exchange protocols (DIDcomm & CHAPI), credential types (JSON-LD & Anoncreds) and DID-methods.

This API documentation is a good example of the integration solution.

### trinsic-credentials-v1-resolved.json

[Trinsic](https://docs.trinsic.id/reference/authentication)

Trinsic is the proof of anything platform. We make it easy for people and organizations to prove things about themselves with technology instead of paper documents. Our software is based on Decentralized Identifiers (DIDs) and Verifiable Credentials (VCs), a new digital identity standard. We use the open-source Hyperledger Aries project, to which we are a primary contributor. (c)

Trinsic is a competitor of Atala Prism that provides similar functionality.

It's focused on commodity wallet solution, uses Hyperledger Aries under the hood, provides Open API spec for all the endpoints and SDK in number of languages.

`Castor`, `Pollux`, `Mercury` are going to provide similar functionality.
Number of Atala products provides similar functionality

Pros:
- good REST API design
- one level of resources in REST API
- attention to documentation
- solid and simple models without overloading
- should be easy and build the solutions

Cons:
- simplicity of solution causes lack of `offline` functionality

Points to consider:

- Would be nice to have the same quality of API in Atala Prism
- API specification is much simple and engineer-friendly compared to Aries
- Schemas are intuitive, well described and easy-for-quick-start