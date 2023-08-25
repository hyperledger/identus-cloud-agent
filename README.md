<p align="center">
  <a href="https://atalaprism.io">
    <img src="docs/images/logos/atala-prism-logo.png" alt="atala-prism-logo" width="513px" height="99px" />
  </a>
  <br>
  <i> <font size="18"> Cloud Agent </font> </i>
  <br>
  <br>
  <a href='https://coveralls.io/github/input-output-hk/atala-prism-building-blocks?branch=main'><img src='https://coveralls.io/repos/github/input-output-hk/atala-prism-building-blocks/badge.svg?branch=main&amp;t=91BUzX&kill_cache=1' alt='Coverage Status' /></a>
  <a href="https://github.com/input-output-hk/atala-prism-building-blocks/actions/workflows/prism-unit-tests.yml"> <img src="https://github.com/input-output-hk/atala-prism-building-blocks/actions/workflows/prism-unit-tests.yml/badge.svg" alt="Unit tests" /> </a>
  <a href="https://github.com/input-output-hk/atala-prism-building-blocks/actions/workflows/e2e-tests.yml"> <img src="https://github.com/input-output-hk/atala-prism-building-blocks/actions/workflows/e2e-tests.yml/badge.svg" alt="End-to-end tests" /> </a>
  <a href="https://github.com/input-output-hk/atala-prism-building-blocks/actions/workflows/performance-tests.yml"> <img src="https://github.com/input-output-hk/atala-prism-building-blocks/actions/workflows/performance-tests.yml/badge.svg" alt="Performance tests" /> </a>
</p>
<hr>

## Overview

The PRISM Cloud Agent is a W3C/Aries standards-based cloud agent written in Scala that provides self-sovereign identity services to build products and solutions based on it. The term "cloud" indicates that it operates on servers and is not intended for use on mobile devices.

PRISM Cloud Agent supports standard-based protocols built on top of DIDComm V2 for issuing, verifying, and holding verifiable credentials using both JWT and Hyperledger AnonCreds (coming soon) formats.

In order to use the PRISM Cloud Agent, you establish a business logic controller responsible for communicating with the agent (initiating HTTP requests and processing webhook notifications). This controller can be created using any programming language capable of sending and receiving HTTP requests.

As a result, you can concentrate on crafting self-sovereign identity solutions using well-known web development tools, without the need to delve into the intricacies of lower-level cryptography and identity protocol internals.

## Features

* Rest API
* DIDComm V2
* W3C-compliant `did:prism` method
* Credential types
  * JWT
  * AnonCreds (coming soon)
* HTTP events notification
* Cardano as a distributed ledger
* Secrets management with Hashicorp vault
* Multi-tenancy (coming soon)

## Example use cases

* A government issues verifiable credentials (VCs) to its citizens to prove their identity and access government services.
* An enterprise issues VCs to its employees to prove their employment and access enterprise services.
* A Web3 authentication service based on the presentation of verifiable proofs (VPs).

## Getting started

### Understanding SSI

Before starting to use the PRISM Cloud Agent, it is important to understand the basic concepts of self-sovereign identity (SSI). The following resources provide a good introduction to SSI:

* [Atala PRISM SSI introduction](https://docs.atalaprism.io/docs/category/concepts/)
* [Linux Foundation Course: Getting Started with SSI](https://www.edx.org/learn/computer-programming/the-linux-foundation-getting-started-with-self-sovereign-identity)

### Architecture

The next diagram offers a concise architectural overview, depicting a PRISM Cloud Agent instance, a controller, the interconnections linking the controller and agent, as well as the external routes to other agents and public ledgers across the Internet.

![PRISM Cloud Agent architecture](docs/images/cloud-agent-architecture-dark.png#gh-dark-mode-only)
![PRISM Cloud Agent architecture](docs/images/cloud-agent-architecture-light.png#gh-light-mode-only)

### Installation and usage

Atala PRISM Cloud Agent is distributed as a Docker image to be run in a containerized environment. All versions can be found [here](https://github.com/input-output-hk/atala-prism-building-blocks/pkgs/container/prism-agent).

The following sections describe how to run the PRISM Cloud Agent in different configurations.

#### Configuration

The PRISM Cloud Agent can be configured to use different types of ledger, secret storage and DID persistence. Any combination of options is available, but the most common configurations are:

| Configuration  | Secret Storage | DIDs persistence | VDR                                  |
| -------------- | -------------- | ---------------- | ------------------------------------ |
| Demo           | PostgreSQL     | No               | In-memory                            |
| Pre-production | PostgreSQL     | Yes              | Cardano testnet (preview or preprod) |
| Production     | Hashicorp      | Yes              | Cardano mainnet                      |

To start playing with PRISM Cloud Agent, we recommend using the demo configuration. Pre-production and production configurations are intended for real-world use cases and require additional more complex configurations of the Cardano stack setup.

> If you're interested in a hosted version of PRISM Cloud Agent, please, contact us at [atalaprism.io](https://atalaprism.io).

#### System requirements

System requirements can vary depending on the use case. The following are the minimum requirements for running the PRISM Cloud Agent with the demo configuration:

* Linux or MacOS operating system
* Docker (with docker-compose support)
* Modern x86 or ARM-based CPU
* \>=2GB RAM

#### Running locally in demo mode

Here is a general example of running a PRISM Agent locally:
```bash
PORT=${PORT} PRISM_AGENT_VERSION=${PRISM_AGENT_VERSION} PRISM_NODE_VERSION=${PRISM_NODE_VERSION} \
  docker compose \
    -p "${AGENT_ROLE}" \
    -f ./infrastructure/shared/docker-compose-demo.yml \
    up --wait
```

The `PORT` variable is used to specify the port number for the PRISM Cloud Agent to listen on. The `PRISM_AGENT_VERSION` and `PRISM_NODE_VERSION` variables are used to specify the versions of the PRISM Cloud Agent and PRISM Node to use. The `AGENT_ROLE` variable is used to specify the role of the PRISM Cloud Agent. The `AGENT_ROLE` variable can be set to `issuer`, `verifier` or `holder`.

In real life, you will need to start at least two PRISM Cloud Agent instances with different roles. For example, you can start one instance with the `issuer` role and another one with the `holder` role. The `issuer` instance will be used to issue verifiable credentials (VCs) and the `holder` instance will be used to hold VCs. Here is an example of how you can do this:
  
```bash
PORT=8080 PRISM_AGENT_VERSION=1.9.2 PRISM_NODE_VERSION=2.2.1 \
  docker compose \
    -p "issuer" \
    -f ./infrastructure/shared/docker-compose-demo.yml \
    up --wait
```

```bash
PORT=8090 PRISM_AGENT_VERSION=1.9.2 PRISM_NODE_VERSION=2.2.1 \
  docker compose \
    -p "holder" \
    -f ./infrastructure/shared/docker-compose-demo.yml \
    up --wait
```

If the PRISM Cloud Agent is started successfully, all the running containers should achieve `Healthy` state, and Cloud Agent Rest API should be available at the specified port, for example:
* `http://localhost:8080/prism-agent` for the `issuer` instance
* `http://localhost:8090/prism-agent` for the `holder` instance

You can check the status of the running containers using the [health endpoint](https://docs.atalaprism.io/agent-api/#tag/System/operation/systemHealth):
```bash
$ curl http://localhost:8080/prism-agent/_system/health
{"version":"1.9.2"}
```

> For more information about all available configuration parameters, please, check [PRISM Cloud Agent configuration](https://docs.atalaprism.io/docs/atala-prism/prism-cloud-agent/environment-variables) section at the documentation portal and edit the `docker-compose-demo.yml` file accordingly.

#### Compatibility between PRISM Cloud Agent and PRISM Node

There could be some incompatibilities between the most latest versions of PRISM Cloud Agent and PRISM Node. Please, use the following table to check the compatibility between the versions:

| PRISM Cloud Agent | PRISM Node |
| ----------------- | ---------- |
| 1.9.2             | 2.2.1      |
| 1.6.0             | 2.1.1      |
| 1.4.0             | 2.1.1      |

### Following the PRISM Cloud Agent tutorials

The following tutorials will help you to get started with the PRISM Cloud Agent and issue your first credentials:

* [Creating, updating and deactivating Decentralized Identifiers (DIDs)](https://docs.atalaprism.io/tutorials/category/dids/)
* [Setting up connections between agents using out-of-band (OOB) protocol](https://docs.atalaprism.io/tutorials/connections/connection)
* [Issuing verifiable credentials (VCs)](https://docs.atalaprism.io/tutorials/credentials/issue)
* [Presenting VC proofs](https://docs.atalaprism.io/tutorials/credentials/present-proof)

## User documentation

All extended documentation, tutorials and API references for the PRISM ecosystem can be found at https://docs.atalaprism.io/

More information about Atala and how we work can be found in our handbook at https://handbook.atalaprism.io/

## Contributing

Please read our [contributions guidelines](CONTRIBUTING) and submit your PRs. We enforce [developer certificate of origin (DCO) commit signing](DCO). We also welcome issues submitted about problems you encounter in using PRISM Cloud Agent.

## License

[Apache License Version 2.0](LICENSE)

<hr>

**Love Atala PRISM? Give our repo a star :star: :arrow_up:.**
