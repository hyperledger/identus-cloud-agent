# Mercury folder

We are following the directory structure defined in [RFC 0013 - Service-Oriented BB API Dependency Management](https://input-output.atlassian.net/wiki/spaces/ATB/pages/3534848001/RFC+0013+-+Service-Oriented+BB+API+Dependency+Management).

**Folders:**

- `api`:
  Contains the API's for the Mercury Building Block.

  **Warning:** this folder is a dependicy of `prism-mediator`/`build.sbt`'s `apiBaseDirectory` property.

- `mercury-library`:
  Is our project implementation of the DID Comm and DID Comm protocols.

- `mercury-cloud-agent`:
  An implementation of a DID Comm Agent on the cloud.

- `mercury-mediator`:
  Is our project implementation of the DID Comm mediator agent.

  See [Mercury Mediator](./prism-mediator/README.md)

- `roots-id-mediator`:
  Contains a configuration setup and utilities script to test interoperability against ROOTS-ID's mediator.

  See [ROOTS-ID-mediator](./roots-id-mediator/REAMDE-ROOTS-ID-mediator.md)
