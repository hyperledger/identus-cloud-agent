# Castor BB service

## Quickstart

__Running Castor service locally for development__

```bash
docker-compose -f docker/docker-compose-local.yaml up -d
sbt server/run
```

---

## Known limitations

### Castor `worker` process embedded inside `server`

Castor worker process which is responsible for async comminucation with other components is temporarily embedded inside `server`.
This impose some restrictions on scalability as it requires some coordination of between `worker` instances.
It should be addressed going forward.

---

## Custodial key management

`prism-agent` also has a responsibility for managing DID key-pair for users.
The interface for key-mangement is heavily inspired by
[indy-sdk-java-wrapper](https://github.com/hyperledger/indy-sdk/tree/main/wrappers/java).

There is an `custodian` subproject which is responsible for managing and storing DID key-pair.
The main goal is to wrap Castor and Pollux libraries which does not handle private-keys
and ease the usage by providing key-mangement capabilities.
Similar to [Indy Wallet SDK - secret API](https://github.com/hyperledger/indy-sdk/tree/main/docs/design/003-wallet-storage#secrets-api),
*it does not expose a private-key* for external use, instead it provide functions to perform cryptographic actions using internally stored private-keys.
