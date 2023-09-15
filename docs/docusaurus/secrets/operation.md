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
This behavior can be modified using the `DEV_MODE` environment variable, which accepts the value `true` or `false`.

__Note that it is important to set `DEV_MODE=false` for the production instance.__

PRISM agent uses the following environment variables for secret management.

| Name                    | Description                                                  | Default                 |
|-------------------------|--------------------------------------------------------------|-------------------------|
| `DEV_MODE`              | Whether PRISM agent should start in development mode         | `false`                 |
| `SECRET_STORAGE_BACKEND`| The storage backend that will be used for the secret storage | `vault`                 |
| `VAULT_TOKEN`           | The token for accessing HashiCorp Vault                      | `root`                  |
| `VAULT_ADDR`            | The address which PRISM agent can reach the Vault            | `http://localhost:8200` |
| `DEFAULT_WALLET_SEED`   | The seed used for DID key management for the default wallet  | -                       |

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
