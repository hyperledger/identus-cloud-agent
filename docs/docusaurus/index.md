# Tutorials

Welcome to the Atala PRISM Tutorials!

These tutorials will help you get started with using Atala PRISM.
The tutorials will guide you through setting up a connection, working with [Decentralized Identifiers (DIDs)](docs/concepts/glossary#decentralized-identifer), and using [verifiable credentials](docs/concepts/glossary#verifiable-credentials).

Whether you are new to [self-sovereign identity (SSI)](docs/concepts/glossary#self-sovereign-identity) or have prior experience, these tutorials will provide the necessary information and skills to build and use SSI-based applications.


Throughout all code examples in tutorials, the following conventions are in use:
* Issuer Agent is hosted at `http://localhost:8080/prism-agent/`
* Holder Agent is hosted at `http://localhost:8090/prism-agent/`
* Verifier Agent is hosted at `http://localhost:8100/prism-agent/`

:::info To use the PRISM Cloud Agents, you must include an `apiKey` header in your requests. You can configure the key, and in some instances, it will be provided to you, so make sure to create an environment variable with the proper value.
```shell
export API_KEY=<API Key value>
```
Alternatively, replace `$API_KEY` with your key in the CURL commands supplied throughout this tutorial. ::: 


