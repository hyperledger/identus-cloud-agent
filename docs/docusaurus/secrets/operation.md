# Operating an agent with secrets

## Introduction

PRISM agent offers a DID (Decentralized Identifier) management solution
which involves creating, storing and using key materials.
To generate a DID key material, the software relies on a seed, following the BIP32 / BIP39 standards.
The system operators have the option to either provide their own seed or
allow the software to generate one automatically. However, in a production environment,
it is crucial for the system operators to explicitly supply the seed to the agent.
This ensures full control over the DID key material and guarantees secure management of user identities.

The PRISM agent includes a development mode that conveniently bypasses certain checks during development or integration.
By default, the agent does not start in the development mode.
This behavior can be modified using the `DEV_MODE` environment variable,
which accepts the value `true` or `false`.
In development mode, the agent can start with or without a user-provided seed.
To provide a seed, set the `WALLET_SEED` variable with a
BIP32 binary seed encoded as a hexadecimal string.
If `WALLET_SEED` is not provided, the agent will generate one automatically.
However, if `DEV_MODE=false` and the `WALLET_SEED` is not provided,
the agent will fail to initialize. This is crucial to maintain secure
and controlled management of the system, given the agent's lack of seed persistence.

__Note that it is important to set `DEV_MODE=false` for the production instance.__

PRISM agent uses the following environment variables for secret management.

| Name          | Description                                          | Default                 |
|---------------|------------------------------------------------------|-------------------------|
| `DEV_MODE`    | Whether PRISM agent should start in development mode | `false`                 |
| `SECRET_STORAGE_BACKEND`| The storage backend that will be used for the secret storage | `vault` |
| `VAULT_TOKEN` | The token for accessing HashiCorp Vault              | `root`                  |
| `VAULT_ADDR`  | The address which PRISM agent can reach the Vault    | `http://localhost:8200` |
| `WALLET_SEED` | The seed used for DID key management                 | -                       |

## Storage backend configuration

Secret storage supports various backends like the Vault service or Postgres database.
By default, the backend chosen for secret storage is Vault, which is suitable for production environments.
There are multiple supported backend implementations, each catering to specific use cases.

__HachiCorp Vault__

When operating in a production environment, the agent has the option to utilize Vault
as a secure secret storage backend. This choice is deemed suitable for production because
all data is encrypted and it also offers additional security-related capabilities.
By default, the agent uses this backend but the option is configurable.
To utilize this backend, set the `SECRET_STORAGE_BACKEND` variable to `vault` and
provide the `VAULT_TOKEN` and `VAULT_ADDR` environment variables.

__Postgres__

Postgres is an alternative backend option for secret storage.
However, this option must be explicitly chosen and will replace Vault.
By opting for Postgres, there is no need for an additional service like Vault,
which simplifies the process of setting up a local development instance.
It utilizes the same database instance as the agent itself. To enable this option,
set the `SECRET_STORAGE_BACKEND` to `postgres`, and it will utilize the same database
configuration as the agent's database. It is important to note that while this option
facilitates an easier development experience, it does not provide a secure method of storing secrets.
The data is not encrypted, making it unsuitable for production use.

## Automatic seed generation in the development mode

When starting the agent in development mode (`DEV_MODE=true`) without setting the `WALLET_SEED`,
it automatically generates a new seed, which is logged in the console for convenience.
However, this mode is insecure and intended solely for development purposes.
It's important to note that the agent does not persist the seed, so it will be regenerated
upon each restart, potentially resulting in unintended behavior.

Leaving the `WALLET_SEED` undefined, the agent should automatically generate a seed on startup.
When starting the PRISM agent locally the following log should appear in the console.

```
level=INFO message="Resolving a wallet seed using WALLET_SEED environemnt variable"
level=INFO message="WALLET_SEED environment is not found."
level=INFO message="Generating a new wallet seed"
INFO  org.bitcoinj.crypto.MnemonicCode - PBKDF2 took 51.59 ms
level=INFO message="New seed generated : 1044f87e445ca10c537688a9645d738bb48747aee39396e4769257268a7d996575a7913028dc30c455b9faec264a66fb7d4f1f46ddda20a9acc04f77113e43cb ([lobster, nice, congress, chair, offer, security, club, live, wide, drum, fringe, sea, situate, sugar, pear, canoe, caught, embody, health, tell, mimic, spray, kid, parent])"
```
The log indicates an attempt to read the seed from the environment variable initially.
As the variable is not set, the log message includes a binary seed (encode as hex string)
and a 24-words mnemonic (BIP39 compliant).

## Specify a wallet seed in the development mode

Similar to the previous example, set `WALLET_SEED=<seed_hex>` to specify the `WALLET_SEED` value.
When starting the agent, the log message will confirm the retrieval of the seed from
the `WALLET_SEED` variable.

```
level=INFO message="Resolving a wallet seed using WALLET_SEED environemnt variable"
```

## Running in production

For production use of the PRISM agent, ensure the `WALLET_SEED` is set.
If the `WALLET_SEED` is not provided and `DEV_MODE=false`, the agent will fail to
start and display the following error message.

```
level=ERROR java.lang.Exception: WALLET_SEED must be present when running with DEV_MODE=false
```
