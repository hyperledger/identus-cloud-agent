<h1 align="center">Atala PRISM v2 - The modern SSI ecosystem.</h1>
<p align="center">
  <img src="docs/images/logos/atala-prism-logo.svg" alt="atala-prism-logo" width="120px" height="120px" />
  <br>
  <i> is an ecosystem and development platform for Self-Sovereign Identity applications
  </i>
  <br>
</p>
<p align="center">
  <a href="https://www.atalaprism.io">
    <strong>www.atalaprism.io</strong>
  </a>
  <br>
</p>
<p align="center">
  <a href="CONTRIBUTING.md">Contributing Guidelines</a>
</p>
<p align="center">
  [![Unit tests](https://github.com/input-output-hk/atala-prism-building-blocks/actions/workflows/prism-unit-tests.yml/badge.svg)](https://github.com/input-output-hk/atala-prism-building-blocks/actions/workflows/prism-unit-tests.yml)
  [![End-to-end tests](https://github.com/input-output-hk/atala-prism-building-blocks/actions/workflows/e2e-tests.yml/badge.svg)](https://github.com/input-output-hk/atala-prism-building-blocks/actions/workflows/e2e-tests.yml)
  [![Performance tests](https://github.com/input-output-hk/atala-prism-building-blocks/actions/workflows/performance-tests.yml/badge.svg)](https://github.com/input-output-hk/atala-prism-building-blocks/actions/workflows/performance-tests.yml)
</p>
<hr>

## Documentation

<!-- FIXME * [OpenAPI docs](openapi) -->

* [Interdependencies](./Interdependencies.md)
* [repositories and relationships](./RepositoriesRrelationships.md)

## Running a single instance locally

Instructions for running the `building-block` stack locally can be found here: [Running locally](infrastructure/local/README.md)

## Running multiple instances locally

Instructions for running multiple instances of the `building-block` stack locally can be found here: [Running multiple locally](infrastructure/multi/README.md)

## Running a instanve locally from sbt (single project)


Run sbt from the root of the repository
- if you want to run the `prism-agent` call the comment `sbt> prismAgentServer/runMain io.iohk.atala.agent.server.AgentApp`

## Developing

Instructions for running the `building-block` stack for development purposes can be found here: [Developing locally](infrastructure/local/README.md)


## Contributing

Read through our [contributing guidelines][contributing] to learn about our submission process, coding rules and more.

<hr>

**Love Atala PRISM? Give our repo a star :star: :arrow_up:.**

[openapi]: docs/README.md
[contributing]: CONTRIBUTING.md
