# PrismAgent service

## Quickstart

### Running PrismAgent service local development

__Environmental setup__

Set the environment variable `ATALA_GITHUB_TOKEN` so we can pull `prism-crypto` from Github packages.

__Spin up PrismAgent dependencies__

Then we need to spin up services and databases for Castor, Pollux and optionally Iris.
The easiest way is to reuse the `./infrastructure/local/docker-compose.yml` which contains
databases and all the services. It should also apply database migrations on startup.
Some services in `docker-compose.yml` can be commented out if not needed
during local development.

```bash
# From the root directory
./infrastructure/loca/run.sh
```
Then the services should be availble in the following ports

|service|port|
|---|---|
|castor db|5432|
|pollux db|???|
|iris|8081|

Then configure `prism-agent` to use the services listed above.

---

## DID key management

`prism-agent` is a cloud agent that represents the digital identity (is a DID controller)
of the Issuing / Verification organization. As a DID controller, it needs to perform
the operation with private and public keys through the Wallet API abstraction level.
The interface for key-mangement is heavily inspired by
[indy-sdk-java-wrapper](https://github.com/hyperledger/indy-sdk/tree/main/wrappers/java).

There is an `key-management` subproject which is responsible for managing and storing DID key-pairs.
The main goal is to wrap Castor and Pollux libraries which does not handle private-keys
and ease the usage by providing key-mangement capabilities.
Similar to [Indy Wallet SDK - secret API](https://github.com/hyperledger/indy-sdk/tree/main/docs/design/003-wallet-storage#secrets-api),
*it does not expose a private-key* for external use, instead it provide functions to perform cryptographic actions using internally stored private-keys.

---

## Known limitations

### Castor `worker` process embedded inside `prism-agent` runnable

Castor worker process which is responsible for async comminucation with other components is temporarily embedded inside `server`.
This impose some restrictions on scalability as it requires some coordination of between `worker` instances.
It should be addressed going forward.
